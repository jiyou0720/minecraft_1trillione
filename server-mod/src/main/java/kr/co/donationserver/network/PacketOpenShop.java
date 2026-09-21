package kr.co.donationserver.network;

import io.netty.buffer.ByteBuf;
import kr.co.donationserver.data.ShopData;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class PacketOpenShop implements IMessage {
    public int shopId;
    public String name;
    public final List<Row> rows = new ArrayList<>();
    public PacketOpenShop() {}
    public PacketOpenShop(ShopData shop) {
        shopId = shop.id;
        name = shop.name;
        for (ShopData.Product product : shop.products)
            rows.add(new Row(product.item.copy(), product.buyEnabled, product.sellEnabled,
                    product.buyPrice, product.currentSellPrice, product.previousSellPrice));
    }
    @Override public void fromBytes(ByteBuf b) {
        shopId = b.readInt();
        name = ByteBufUtils.readUTF8String(b);
        int size = Math.max(0, Math.min(100, b.readInt()));
        for (int i = 0; i < size; i++) rows.add(new Row(ByteBufUtils.readItemStack(b), b.readBoolean(), b.readBoolean(), b.readLong(), b.readLong(), b.readLong()));
    }
    @Override public void toBytes(ByteBuf b) {
        b.writeInt(shopId);
        ByteBufUtils.writeUTF8String(b, name);
        b.writeInt(rows.size());
        for (Row row : rows) {
            ByteBufUtils.writeItemStack(b, row.item);
            b.writeBoolean(row.buy);
            b.writeBoolean(row.sell);
            b.writeLong(row.buyPrice);
            b.writeLong(row.sellPrice);
            b.writeLong(row.previousSellPrice);
        }
    }
    public static class Row {
        public final ItemStack item;
        public final boolean buy, sell;
        public final long buyPrice, sellPrice, previousSellPrice;
        public Row(ItemStack item, boolean buy, boolean sell, long buyPrice, long sellPrice, long previousSellPrice) {
            this.item = item; this.buy = buy; this.sell = sell; this.buyPrice = buyPrice; this.sellPrice = sellPrice; this.previousSellPrice = previousSellPrice;
        }
    }
}
