package kr.co.donationserver.proxy;
import kr.co.donationserver.client.ClientEvents;
import kr.co.donationserver.client.RenderShopNpc;
import kr.co.donationserver.entity.EntityShopNpc;
import kr.co.donationserver.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
public class ClientProxy extends CommonProxy {@Override public void preInit(){NetworkHandler.registerClient();RenderingRegistry.registerEntityRenderingHandler(EntityShopNpc.class,RenderShopNpc::new);MinecraftForge.EVENT_BUS.register(ClientEvents.INSTANCE);}}
