package kr.co.donationserver.service;

import kr.co.donationserver.config.ModConfig;
import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.data.PlayerData;
import kr.co.donationserver.util.Texts;
import kr.co.donationserver.service.LandVoucher;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;

public final class TutorialService {
    private TutorialService() {}

    public static void show(EntityPlayerMP player) {
        if (!ModConfig.tutorialEnabled) return;
        DonationData data = DonationData.get(player.world);
        PlayerData progress = data.player(player.getUniqueID());
        if (progress.tutorialComplete) {
            player.sendMessage(Texts.text("&e튜토리얼을 이미 완료했습니다. 다시 보려면 /튜토리얼 다시를 입력하세요."));
            return;
        }
        String[] pages = ModConfig.tutorialMessages;
        if (pages == null || pages.length == 0) { complete(player); return; }
        if (progress.tutorialStep >= pages.length) { progress.tutorialStep = pages.length - 1; data.markDirty(); }
        int step = progress.tutorialStep;
        player.sendMessage(Texts.text("&6&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(Texts.text("&e&l[튜토리얼 " + (step + 1) + "/" + pages.length + "]"));
        player.sendMessage(Texts.text(pages[step]));
        TextComponentString controls = new TextComponentString("");
        if (step > 0) {
            TextComponentString previous = Texts.text("&7[이전]   ");
            previous.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/튜토리얼 이전"));
            controls.appendSibling(previous);
        }
        TextComponentString next = Texts.text(step + 1 < pages.length ? "&a[다음 ▶]" : "&a[튜토리얼 완료 ✓]");
        next.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/튜토리얼 다음"));
        controls.appendSibling(next);
        player.sendMessage(controls);
        player.sendMessage(Texts.text("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    public static void next(EntityPlayerMP player) {
        if (!ModConfig.tutorialEnabled) return;
        DonationData data = DonationData.get(player.world);
        PlayerData progress = data.player(player.getUniqueID());
        if (progress.tutorialComplete) { show(player); return; }
        if (progress.tutorialStep + 1 >= ModConfig.tutorialMessages.length) { complete(player); return; }
        progress.tutorialStep++;
        data.markDirty();
        show(player);
    }

    public static void previous(EntityPlayerMP player) {
        DonationData data = DonationData.get(player.world);
        PlayerData progress = data.player(player.getUniqueID());
        if (progress.tutorialComplete) { show(player); return; }
        if (progress.tutorialStep > 0) { progress.tutorialStep--; data.markDirty(); }
        show(player);
    }

    public static void restart(EntityPlayerMP player) {
        DonationData data = DonationData.get(player.world);
        PlayerData progress = data.player(player.getUniqueID());
        progress.tutorialStep = 0;
        progress.tutorialComplete = false;
        data.markDirty();
        show(player);
    }

    public static void complete(EntityPlayerMP player) {
        DonationData data = DonationData.get(player.world);
        PlayerData progress = data.player(player.getUniqueID());
        if (progress.tutorialComplete) {
            player.sendMessage(Texts.text("&e이미 튜토리얼을 완료했습니다."));
            return;
        }
        progress.tutorialComplete = true;
        progress.tutorialStep = 0;
        if (!progress.tutorialRewardGiven) {
            progress.tutorialRewardGiven = true;
            progress.balance = Long.MAX_VALUE - progress.balance < 10000 ? Long.MAX_VALUE : progress.balance + 10000;
            give(player, new ItemStack(Items.STONE_SWORD));
            give(player, new ItemStack(Items.STONE_PICKAXE));
            give(player, new ItemStack(Items.STONE_AXE));
            give(player, new ItemStack(Items.STONE_SHOVEL));
            give(player, new ItemStack(Items.STONE_HOE));
            give(player, LandVoucher.create());
            give(player, new ItemStack(Items.WHEAT_SEEDS, 64));
            player.inventoryContainer.detectAndSendChanges();
            EconomyService.sync(player);
            player.sendMessage(Texts.text("&a튜토리얼 보상: 돌도구 세트, 10,000원, 영지 구매권 1장, 밀 씨앗 64개"));
        }
        data.markDirty();
        player.sendMessage(Texts.text("&a튜토리얼을 완료했습니다! 현재 위치에서 자유롭게 플레이하세요."));
    }

    private static void give(EntityPlayerMP player, ItemStack item) {
        if (!player.inventory.addItemStackToInventory(item) && !item.isEmpty()) player.dropItem(item, false);
    }
}
