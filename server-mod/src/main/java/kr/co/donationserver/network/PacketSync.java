package kr.co.donationserver.network;
import io.netty.buffer.ByteBuf;import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
public class PacketSync implements IMessage {public long balance,personal,total;public PacketSync(){}public PacketSync(long b,long p,long t){balance=b;personal=p;total=t;}public void fromBytes(ByteBuf b){balance=b.readLong();personal=b.readLong();total=b.readLong();}public void toBytes(ByteBuf b){b.writeLong(balance);b.writeLong(personal);b.writeLong(total);}}
