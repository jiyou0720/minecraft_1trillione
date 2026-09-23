package kr.co.donationserver;

import kr.co.donationserver.command.*;
import kr.co.donationserver.config.ModConfig;
import kr.co.donationserver.event.ServerEvents;
import kr.co.donationserver.network.NetworkHandler;
import kr.co.donationserver.proxy.CommonProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraft.util.ResourceLocation;
import kr.co.donationserver.entity.EntityShopNpc;
import org.apache.logging.log4j.Logger;

@Mod(modid = DonationServerMod.MODID, name = DonationServerMod.NAME, version = DonationServerMod.VERSION,
        acceptedMinecraftVersions = "[1.12.2]")
public class DonationServerMod {
    public static final String MODID = "donationserver";
    public static final String NAME = "1억 기부 서버 코어";
    public static final String VERSION = "0.2.13";

    @SidedProxy(clientSide = "kr.co.donationserver.proxy.ClientProxy", serverSide = "kr.co.donationserver.proxy.CommonProxy")
    public static CommonProxy proxy;
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        ModConfig.load(event.getSuggestedConfigurationFile());
        EntityRegistry.registerModEntity(new ResourceLocation(MODID, "shop_npc"), EntityShopNpc.class, "shop_npc", 1, this, 64, 3, true);
        NetworkHandler.init();
        proxy.preInit();
        MinecraftForge.EVENT_BUS.register(ServerEvents.INSTANCE);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new DonateCommand());
        event.registerServerCommand(new DonationStatusCommand());
        event.registerServerCommand(new TutorialCompleteCommand());
        event.registerServerCommand(new TutorialCommand());
        event.registerServerCommand(new SetHomeCommand());
        event.registerServerCommand(new FastTravelCommand());
        event.registerServerCommand(new CropShopCommand());
        event.registerServerCommand(new EconomyAdminCommand());
        event.registerServerCommand(new ShopAdminCommand());
        event.registerServerCommand(new TransferCommand());
        event.registerServerCommand(new LandClaimCommand());
        event.registerServerCommand(new LandAdminCommand());
        event.registerServerCommand(new JoinSpawnCommand());
        event.registerServerCommand(new ShopRegionCommand());
        event.registerServerCommand(new RandomBoxAdminCommand());
    }
}
