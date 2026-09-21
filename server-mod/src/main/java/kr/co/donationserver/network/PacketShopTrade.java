package kr.co.donationserver.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketShopTrade implements IMessage {
    public int shopId, productIndex, quantity;
    public boolean buy;
    public long displayedPrice;
    public PacketShopTrade() {}
    public PacketShopTrade(int shopId, int productIndex, boolean buy, int quantity, long displayedPrice) {
        this.shopId = shopId; this.productIndex = productIndex; this.buy = buy; this.quantity = quantity; this.displayedPrice = displayedPrice;
    }
    @Override public void fromBytes(ByteBuf b) { shopId = b.readInt(); productIndex = b.readInt(); buy = b.readBoolean(); quantity = b.readInt(); displayedPrice = b.readLong(); }
    @Override public void toBytes(ByteBuf b) { b.writeInt(shopId); b.writeInt(productIndex); b.writeBoolean(buy); b.writeInt(quantity); b.writeLong(displayedPrice); }
}
