package kr.co.donationserver.data;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopData {
    public int id;
    public UUID entityId;
    public int dimension;
    public String name;
    public final List<Product> products = new ArrayList<>();

    public NBTTagCompound write() {
        NBTTagCompound n = new NBTTagCompound();
        n.setInteger("id", id);
        n.setString("entityId", entityId.toString());
        n.setInteger("dimension", dimension);
        n.setString("name", name);
        NBTTagList list = new NBTTagList();
        for (Product product : products) list.appendTag(product.write());
        n.setTag("products", list);
        return n;
    }

    public static ShopData read(NBTTagCompound n) {
        ShopData shop = new ShopData();
        shop.id = n.getInteger("id");
        shop.entityId = UUID.fromString(n.getString("entityId"));
        shop.dimension = n.getInteger("dimension");
        shop.name = n.getString("name");
        NBTTagList list = n.getTagList("products", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            Product product = Product.read(list.getCompoundTagAt(i));
            if (product != null) shop.products.add(product);
        }
        return shop;
    }

    public static class Product {
        public ItemStack item;
        public boolean buyEnabled, sellEnabled;
        public long buyPrice, baseSellPrice, minSellPrice, currentSellPrice, previousSellPrice;
        public int consecutiveDrops;

        public NBTTagCompound write() {
            NBTTagCompound n = new NBTTagCompound();
            ItemStack one = item.copy();
            one.setCount(1);
            n.setTag("item", one.writeToNBT(new NBTTagCompound()));
            n.setBoolean("buy", buyEnabled);
            n.setBoolean("sell", sellEnabled);
            n.setLong("buyPrice", buyPrice);
            n.setLong("baseSellPrice", baseSellPrice);
            n.setLong("minSellPrice", minSellPrice);
            n.setLong("currentSellPrice", currentSellPrice);
            n.setLong("previousSellPrice", previousSellPrice);
            n.setInteger("consecutiveDrops", consecutiveDrops);
            return n;
        }

        public static Product read(NBTTagCompound n) {
            Product p = new Product();
            p.item = new ItemStack(n.getCompoundTag("item"));
            if (p.item.isEmpty()) return null;
            p.item.setCount(1);
            p.buyEnabled = n.getBoolean("buy");
            p.sellEnabled = n.getBoolean("sell");
            p.buyPrice = Math.max(1, n.getLong("buyPrice"));
            p.baseSellPrice = Math.max(1, n.getLong("baseSellPrice"));
            p.minSellPrice = Math.max(1, n.getLong("minSellPrice"));
            p.currentSellPrice = Math.max(p.minSellPrice, n.getLong("currentSellPrice"));
            p.previousSellPrice = n.hasKey("previousSellPrice") ? Math.max(0, n.getLong("previousSellPrice")) : p.currentSellPrice;
            p.consecutiveDrops = Math.max(0, Math.min(5, n.getInteger("consecutiveDrops")));
            return p;
        }
    }
}
