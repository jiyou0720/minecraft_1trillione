package kr.co.donationserver.network;
import io.netty.buffer.ByteBuf;import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
public class PacketAction implements IMessage {public int action;public PacketAction(){}public PacketAction(int action){this.action=action;}public void fromBytes(ByteBuf b){action=b.readInt();}public void toBytes(ByteBuf b){b.writeInt(action);}}
