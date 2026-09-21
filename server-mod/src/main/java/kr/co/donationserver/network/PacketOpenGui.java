package kr.co.donationserver.network;
import io.netty.buffer.ByteBuf;import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
public class PacketOpenGui implements IMessage {public int type;public PacketOpenGui(){}public PacketOpenGui(int type){this.type=type;}public void fromBytes(ByteBuf b){type=b.readInt();}public void toBytes(ByteBuf b){b.writeInt(type);}}
