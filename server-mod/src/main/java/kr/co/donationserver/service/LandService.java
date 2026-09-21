package kr.co.donationserver.service;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.util.Texts;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.UUID;

public final class LandService {
    private LandService() {}

    public static UUID owner(World world, BlockPos pos) {
        return DonationData.get(world).landClaims.get(DonationData.claimKey(world.provider.getDimension(), pos.getX() >> 4, pos.getZ() >> 4));
    }

    public static boolean canModify(EntityPlayerMP player, BlockPos pos) {
        DonationData data = DonationData.get(player.world);
        UUID owner = data.landClaims.get(DonationData.claimKey(player.dimension, pos.getX() >> 4, pos.getZ() >> 4));
        return owner == null || owner.equals(player.getUniqueID()) || player.canUseCommand(2, "land.admin")
                || data.landTrusted.containsKey(owner) && data.landTrusted.get(owner).contains(player.getUniqueID());
    }

    public static void claim(EntityPlayerMP player) {
        DonationData data = DonationData.get(player.world);
        int x = player.getPosition().getX() >> 4;
        int z = player.getPosition().getZ() >> 4;
        String key = DonationData.claimKey(player.dimension, x, z);
        int nearestX = Math.max(x * 16, Math.min(data.shopRegionX, x * 16 + 15));
        int nearestZ = Math.max(z * 16, Math.min(data.shopRegionZ, z * 16 + 15));
        if (data.inShopRegion(player.dimension, nearestX, nearestZ)) {
            player.sendMessage(Texts.text("&c상점가 보호 구역에는 영지를 설정할 수 없습니다."));
            return;
        }
        UUID existing = data.landClaims.get(key);
        if (existing != null) {
            player.sendMessage(Texts.text(existing.equals(player.getUniqueID()) ? "&e이미 내 영지인 청크입니다." : "&c다른 플레이어가 소유한 청크입니다."));
            return;
        }
        int count = data.claimCount(player.getUniqueID());
        if (data.maxLandClaims > 0 && count >= data.maxLandClaims) {
            player.sendMessage(Texts.text("&c보유 가능한 영지 청크 수를 모두 사용했습니다."));
            return;
        }
        if (count > 0 && !adjacent(data, player.getUniqueID(), player.dimension, x, z)) {
            player.sendMessage(Texts.text("&c확장 영지는 기존 영지와 변이 맞닿은 청크만 설정할 수 있습니다."));
            return;
        }
        int voucherSlot = findVoucher(player);
        if (voucherSlot < 0) {
            player.sendMessage(Texts.text("&c영지 등록에는 [영지 구매권] 1장이 필요합니다. 튜토리얼 완료 보상 또는 상점에서 받을 수 있습니다."));
            return;
        }
        ItemStack voucher = player.inventory.getStackInSlot(voucherSlot);
        voucher.shrink(1);
        if (voucher.isEmpty()) player.inventory.setInventorySlotContents(voucherSlot, ItemStack.EMPTY);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        data.landClaims.put(key, player.getUniqueID());
        data.markDirty();
        player.sendMessage(Texts.text("&a영지 청크를 설정했습니다. (" + x + ", " + z + ") / " +
                "영지 구매권 1장 사용 / 보유 " + (count + 1) + "청크"));
    }

    public static void release(EntityPlayerMP player) {
        DonationData data = DonationData.get(player.world);
        int x = player.getPosition().getX() >> 4;
        int z = player.getPosition().getZ() >> 4;
        String key = DonationData.claimKey(player.dimension, x, z);
        UUID owner = data.landClaims.get(key);
        if (owner == null) {
            player.sendMessage(Texts.text("&c현재 청크는 등록된 영지가 아닙니다."));
            return;
        }
        if (!owner.equals(player.getUniqueID())) {
            player.sendMessage(Texts.text("&c본인 소유의 영지만 해제할 수 있습니다."));
            return;
        }
        data.landClaims.remove(key);
        int remaining = data.claimCount(owner);
        if (remaining == 0) data.landTrusted.remove(owner);
        data.markDirty();
        player.sendMessage(Texts.text("&a현재 청크 (" + x + ", " + z + ")의 영지를 해제했습니다. 구매권은 환급되지 않습니다. / 남은 영지 " + remaining + "청크"));
        if (remaining == 0) player.sendMessage(Texts.text("&e마지막 영지를 해제해 공유 유저 목록도 초기화했습니다."));
    }

    private static int findVoucher(EntityPlayerMP player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++)
            if (LandVoucher.isVoucher(player.inventory.getStackInSlot(i))) return i;
        return -1;
    }

    private static boolean adjacent(DonationData data, UUID owner, int dim, int x, int z) {
        return owner.equals(data.landClaims.get(DonationData.claimKey(dim, x + 1, z)))
                || owner.equals(data.landClaims.get(DonationData.claimKey(dim, x - 1, z)))
                || owner.equals(data.landClaims.get(DonationData.claimKey(dim, x, z + 1)))
                || owner.equals(data.landClaims.get(DonationData.claimKey(dim, x, z - 1)));
    }
}
