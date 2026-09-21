package kr.co.donationserver.network;
import kr.co.donationserver.DonationServerMod;import kr.co.donationserver.event.ServerEvents;import net.minecraft.entity.player.EntityPlayerMP;import net.minecraftforge.fml.common.network.NetworkRegistry;import net.minecraftforge.fml.common.network.simpleimpl.*;import net.minecraftforge.fml.relauncher.Side;
public final class NetworkHandler {
 public static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel(DonationServerMod.MODID);private NetworkHandler(){}
 public static void init(){CHANNEL.registerMessage(ActionHandler.class,PacketAction.class,0,Side.SERVER);CHANNEL.registerMessage(ShopTradeHandler.class,PacketShopTrade.class,4,Side.SERVER);}
 public static void registerServerClientMessages(){CHANNEL.registerMessage(NoopOpen.class,PacketOpenGui.class,1,Side.CLIENT);CHANNEL.registerMessage(NoopSync.class,PacketSync.class,2,Side.CLIENT);CHANNEL.registerMessage(NoopShop.class,PacketOpenShop.class,3,Side.CLIENT);}
 public static void registerClient(){CHANNEL.registerMessage(kr.co.donationserver.client.ClientPacketHandlers.Open.class,PacketOpenGui.class,1,Side.CLIENT);CHANNEL.registerMessage(kr.co.donationserver.client.ClientPacketHandlers.Sync.class,PacketSync.class,2,Side.CLIENT);CHANNEL.registerMessage(kr.co.donationserver.client.ClientPacketHandlers.OpenShop.class,PacketOpenShop.class,3,Side.CLIENT);}
 public static void open(EntityPlayerMP p,int type){CHANNEL.sendTo(new PacketOpenGui(type),p);}public static void sync(EntityPlayerMP p,long b,long d,long t){CHANNEL.sendTo(new PacketSync(b,d,t),p);}
 public static class ActionHandler implements IMessageHandler<PacketAction,IMessage>{public IMessage onMessage(PacketAction m,MessageContext c){EntityPlayerMP p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->{if(m.action>=0&&m.action<=2)ServerEvents.INSTANCE.requestTravel(p,m.action);});return null;}}
 public static class ShopTradeHandler implements IMessageHandler<PacketShopTrade,IMessage>{public IMessage onMessage(PacketShopTrade m,MessageContext c){EntityPlayerMP p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->kr.co.donationserver.service.ShopService.trade(p,m.shopId,m.productIndex,m.buy,m.quantity,m.displayedPrice));return null;}}
 public static class NoopOpen implements IMessageHandler<PacketOpenGui,IMessage>{public IMessage onMessage(PacketOpenGui m,MessageContext c){return null;}}
 public static class NoopSync implements IMessageHandler<PacketSync,IMessage>{public IMessage onMessage(PacketSync m,MessageContext c){return null;}}
 public static class NoopShop implements IMessageHandler<PacketOpenShop,IMessage>{public IMessage onMessage(PacketOpenShop m,MessageContext c){return null;}}
}
