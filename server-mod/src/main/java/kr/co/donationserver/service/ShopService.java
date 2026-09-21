package kr.co.donationserver.service;

import kr.co.donationserver.data.DonationData;
import kr.co.donationserver.data.PlayerData;
import kr.co.donationserver.data.ShopData;
import kr.co.donationserver.network.NetworkHandler;
import kr.co.donationserver.network.PacketOpenShop;
import kr.co.donationserver.util.Texts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import java.util.Random;

public final class ShopService {
    public static final long PRICE_INTERVAL_MS = 30L * 60L * 1000L;
    private static final Random RANDOM = new Random();
    private ShopService() {}

    public static ShopData byNpc(DonationData data, Entity entity) {
        for (ShopData shop : data.shops.values())
            if (shop.dimension == entity.dimension && shop.entityId.equals(entity.getUniqueID())) return shop;
        return null;
    }

    public static void open(EntityPlayerMP player, ShopData shop) {
        if (nearNpc(player, shop)) NetworkHandler.CHANNEL.sendTo(new PacketOpenShop(shop), player);
    }

    private static boolean nearNpc(EntityPlayerMP player, ShopData shop) {
        if (player.dimension != shop.dimension) return false;
        Entity npc = player.getServerWorld().getEntityFromUuid(shop.entityId);
        return npc != null && npc.isEntityAlive() && player.getDistanceSq(npc) <= 36.0;
    }

    public static void trade(EntityPlayerMP player, int shopId, int productIndex, boolean buy, int quantity, long displayedPrice) {
        if (quantity != 1 && quantity != 10 && quantity != 64) return;
        DonationData data = DonationData.get(player.world);
        ShopData shop = data.shops.get(shopId);
        if (shop == null || !nearNpc(player, shop) || productIndex < 0 || productIndex >= shop.products.size()) {
            player.sendMessage(Texts.text("&c상점 NPC 가까이에서 다시 시도해 주세요."));
            return;
        }
        ShopData.Product product = shop.products.get(productIndex);
        long unitPrice = buy ? product.buyPrice : product.currentSellPrice;
        if (displayedPrice != unitPrice) {
            player.sendMessage(Texts.text("&e시세가 변경되었습니다. 갱신된 가격을 확인해 주세요."));
            open(player, shop);
            return;
        }
        if (unitPrice < 1 || unitPrice > Long.MAX_VALUE / quantity) {
            player.sendMessage(Texts.text("&c거래 금액이 허용 범위를 초과했습니다."));
            return;
        }
        long total = unitPrice * quantity;
        PlayerData account = data.player(player.getUniqueID());
        if (buy) {
            if (!product.buyEnabled) return;
            if (account.balance < total) {
                player.sendMessage(Texts.text("&c잔액이 부족합니다. 필요 금액: " + Texts.money(total)));
                return;
            }
            if (availableSpace(player, product.item) < quantity) {
                player.sendMessage(Texts.text("&c인벤토리 공간이 부족합니다. " + quantity + "개를 받을 공간이 필요합니다."));
                return;
            }
            give(player, product.item, quantity);
            account.balance -= total;
            player.sendMessage(Texts.text("&a" + product.item.getDisplayName() + " " + quantity + "개 구매: -" + Texts.money(total)));
        } else {
            if (!product.sellEnabled) return;
            if (ownedCount(player, product.item) < quantity) {
                player.sendMessage(Texts.text("&c판매할 상품이 " + quantity + "개 필요합니다."));
                return;
            }
            take(player, product.item, quantity);
            account.balance = Long.MAX_VALUE - account.balance < total ? Long.MAX_VALUE : account.balance + total;
            player.sendMessage(Texts.text("&a" + product.item.getDisplayName() + " " + quantity + "개 판매: +" + Texts.money(total)));
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        data.markDirty();
        EconomyService.sync(player);
    }

    private static boolean matches(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.areItemsEqual(a, b)
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static int ownedCount(EntityPlayerMP player, ItemStack template) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (matches(stack, template)) count += stack.getCount();
        }
        return count;
    }

    private static int availableSpace(EntityPlayerMP player, ItemStack template) {
        int space = 0;
        int max = Math.min(template.getMaxStackSize(), player.inventory.getInventoryStackLimit());
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) space += max;
            else if (matches(stack, template)) space += Math.max(0, Math.min(stack.getMaxStackSize(), max) - stack.getCount());
        }
        return space;
    }

    private static void give(EntityPlayerMP player, ItemStack template, int quantity) {
        int remaining = quantity;
        int max = Math.min(template.getMaxStackSize(), player.inventory.getInventoryStackLimit());
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!matches(stack, template)) continue;
            int add = Math.min(remaining, Math.max(0, Math.min(stack.getMaxStackSize(), max) - stack.getCount()));
            stack.grow(add);
            remaining -= add;
        }
        for (int i = 0; i < 36 && remaining > 0; i++) {
            if (!player.inventory.getStackInSlot(i).isEmpty()) continue;
            ItemStack stack = template.copy();
            int add = Math.min(remaining, max);
            stack.setCount(add);
            player.inventory.setInventorySlotContents(i, stack);
            remaining -= add;
        }
    }

    private static void take(EntityPlayerMP player, ItemStack template, int quantity) {
        int remaining = quantity;
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!matches(stack, template)) continue;
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (stack.isEmpty()) player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
        }
    }

    public static int updatePrices(MinecraftServer server, boolean force) {
        WorldServer world = server.getWorld(0);
        if (world == null) return 0;
        DonationData data = DonationData.get(world);
        long now = System.currentTimeMillis();
        if (!force && data.nextShopPriceChange == 0) {
            data.nextShopPriceChange = now + PRICE_INTERVAL_MS;
            data.markDirty();
            return 0;
        }
        if (!force && now < data.nextShopPriceChange) return 0;
        int changed = 0;
        for (ShopData shop : data.shops.values()) for (ShopData.Product product : shop.products) {
            if (!product.sellEnabled) continue;
            long old = Math.max(product.minSellPrice, product.currentSellPrice);
            long change = Math.max(1, Math.round(old * 0.10));
            boolean rise = product.consecutiveDrops >= 5 || old <= product.minSellPrice || RANDOM.nextBoolean();
            long next = rise ? old > Long.MAX_VALUE - change ? Long.MAX_VALUE : old + change
                    : Math.max(product.minSellPrice, old - change);
            product.previousSellPrice = old;
            product.currentSellPrice = next;
            product.consecutiveDrops = rise ? 0 : product.consecutiveDrops + 1;
            if (next != old) changed++;
        }
        data.nextShopPriceChange = now + PRICE_INTERVAL_MS;
        data.markDirty();
        if (changed > 0) server.getPlayerList().sendMessage(Texts.text("&6[상점 시세] &f" + changed + "개 상품의 판매 가격이 변동되었습니다. NPC 상점에서 &c▲ 상승 &9▼ 하락&f을 확인하세요."));
        return changed;
    }

    public static void updatePrices(MinecraftServer server) { updatePrices(server, false); }
}
