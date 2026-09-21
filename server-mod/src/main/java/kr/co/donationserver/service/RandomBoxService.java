package kr.co.donationserver.service;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.data.RandomBoxData;
import kr.co.donationserver.util.Texts;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.network.play.server.SPacketTitle;
import java.util.Random;

public final class RandomBoxService {
    private static final String TAG = "donationserverSkinBox";
    private static final Random RANDOM = new Random();
    private RandomBoxService() {}

    public static ItemStack create(RandomBoxData box) {
        ItemStack item = new ItemStack(Blocks.CHEST, 1);
        item.setStackDisplayName("[스킨 랜덤박스] " + box.name);
        NBTTagCompound tag = item.getTagCompound();
        if (tag == null) tag = new NBTTagCompound();
        tag.setInteger(TAG, box.id);
        item.setTagCompound(tag);
        return item;
    }

    public static int boxId(ItemStack item) {
        return item.isEmpty() || item.getItem() != net.minecraft.item.Item.getItemFromBlock(Blocks.CHEST)
                || !item.hasTagCompound() ? 0 : item.getTagCompound().getInteger(TAG);
    }

    public static boolean open(EntityPlayerMP player, ItemStack held) {
        int id = boxId(held);
        if (id <= 0) return false;
        RandomBoxData box = DonationData.get(player.world).randomBoxes.get(id);
        if (box == null || box.rewards.isEmpty()) {
            player.sendMessage(Texts.text("&c이 랜덤박스에는 등록된 보상이 없습니다. 운영자에게 문의해 주세요."));
            return true;
        }
        long weightSum = 0;
        for (RandomBoxData.Reward reward : box.rewards) weightSum += reward.weight;
        if (weightSum <= 0) return true;
        long draw = (long) (RANDOM.nextDouble() * weightSum);
        RandomBoxData.Reward chosen = box.rewards.get(box.rewards.size() - 1);
        for (RandomBoxData.Reward reward : box.rewards) {
            draw -= reward.weight;
            if (draw < 0) { chosen = reward; break; }
        }
        ItemStack prize = chosen.item.copy();
        held.shrink(1);
        player.inventory.markDirty();
        if (!player.inventory.addItemStackToInventory(prize) && !prize.isEmpty()) player.dropItem(prize, false);
        player.inventoryContainer.detectAndSendChanges();
        ITextComponent title = Texts.text("&6[스킨 랜덤박스]");
        ITextComponent subtitle = Texts.text("&a" + chosen.item.getDisplayName() + " 당첨!");
        player.connection.sendPacket(new SPacketTitle(5, 45, 10));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE, title));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.SUBTITLE, subtitle));
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 1);
        player.sendMessage(Texts.text("&a" + box.name + "에서 " + chosen.item.getDisplayName() + "을(를) 획득했습니다."));
        return true;
    }
}
