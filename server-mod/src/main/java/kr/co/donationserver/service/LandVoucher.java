package kr.co.donationserver.service;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/** A named paper is not enough: only server-created paper with our NBT marker is redeemable. */
public final class LandVoucher {
    private static final String TAG = "donationserverLandVoucher";
    private LandVoucher() {}

    public static ItemStack create() {
        ItemStack paper = new ItemStack(Items.PAPER, 1);
        paper.setStackDisplayName("[영지 구매권]");
        NBTTagCompound root = paper.getTagCompound();
        if (root == null) root = new NBTTagCompound();
        root.setInteger(TAG, 1);
        NBTTagCompound display = root.getCompoundTag("display");
        NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString("§7영지를 구매할 수 있습니다."));
        lore.appendTag(new NBTTagString("§e인접 청크에서 우클릭 또는 /영지설정"));
        display.setTag("Lore", lore);
        root.setTag("display", display);
        paper.setTagCompound(root);
        return paper;
    }

    public static boolean isVoucher(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.PAPER
                && stack.hasTagCompound() && stack.getTagCompound().getInteger(TAG) == 1;
    }
}
