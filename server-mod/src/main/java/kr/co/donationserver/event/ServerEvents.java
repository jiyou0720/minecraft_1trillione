package kr.co.donationserver.event;

import kr.co.donationserver.config.ModConfig;
import kr.co.donationserver.data.*;
import kr.co.donationserver.service.EconomyService;
import kr.co.donationserver.service.ShopService;
import kr.co.donationserver.service.LandService;
import kr.co.donationserver.service.TutorialService;
import kr.co.donationserver.service.LandVoucher;
import kr.co.donationserver.service.RandomBoxService;
import kr.co.donationserver.util.Texts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.EnumCreatureType;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.eventhandler.Event;
import com.mojang.authlib.GameProfile;
import java.util.*;

public enum ServerEvents {INSTANCE;
    private final Map<UUID,PendingTravel> pending=new HashMap<>();
    private final Map<UUID,Long> cooldown=new HashMap<>();
    private final Map<UUID,Long> borderUntil=new HashMap<>();
    private final Map<UUID,String> lastLand=new HashMap<>();

    @SubscribeEvent public void login(PlayerLoggedInEvent e){
        if(!(e.player instanceof EntityPlayerMP))return;
        EntityPlayerMP p=(EntityPlayerMP)e.player;
        DonationData data=DonationData.get(p.world);
        PlayerData pd=data.player(p.getUniqueID());
        if(!pd.joinedOnce){
            pd.joinedOnce=true;
            data.markDirty();
            LocationData spawn=data.joinSpawn;
            if(spawn==null){
                WorldServer world=p.getServer().getWorld(0);
                BlockPos surface=world.getHeight(new BlockPos(0,0,0));
                spawn=new LocationData(0,0.5,surface.getY(),0.5,0,0);
            }
            teleportNow(p,spawn);
        }
        EconomyService.sync(p);
        if(ModConfig.tutorialEnabled&&!pd.tutorialComplete)TutorialService.show(p);
    }
    @SubscribeEvent public void logout(PlayerLoggedOutEvent e){
        UUID id=e.player.getUniqueID();
        pending.remove(id);cooldown.remove(id);borderUntil.remove(id);lastLand.remove(id);
    }
    @SubscribeEvent public void chat(ServerChatEvent e){PlayerData pd=DonationData.get(e.getPlayer().world).player(e.getPlayer().getUniqueID());if(pd.title!=null&&!pd.title.isEmpty())e.setComponent(new TextComponentString("§6["+pd.title+"] §f<"+e.getUsername()+"> "+e.getMessage()));}
    @SubscribeEvent public void interact(PlayerInteractEvent.EntityInteract e){if(e.getWorld().isRemote||!(e.getEntityPlayer() instanceof EntityPlayerMP))return;EntityPlayerMP player=(EntityPlayerMP)e.getEntityPlayer();if(e.getTarget() instanceof EntityVillager){e.setCanceled(true);if(e.getHand()==net.minecraft.util.EnumHand.MAIN_HAND)player.sendMessage(Texts.text("&c일반 주민 거래는 사용할 수 없습니다. NPC 상점을 이용해 주세요."));return;}if(!LandService.canModify(player,e.getTarget().getPosition())){e.setCanceled(true);if(e.getHand()==net.minecraft.util.EnumHand.MAIN_HAND)player.sendMessage(Texts.text("&c다른 플레이어의 영지입니다."));return;}ShopData shop=ShopService.byNpc(DonationData.get(e.getWorld()),e.getTarget());if(shop!=null){e.setCanceled(true);if(e.getHand()==net.minecraft.util.EnumHand.MAIN_HAND)ShopService.open(player,shop);}}
    @SubscribeEvent public void protectShop(LivingAttackEvent e){if(!e.getEntity().world.isRemote&&ShopService.byNpc(DonationData.get(e.getEntity().world),e.getEntity())!=null)e.setCanceled(true);}
    @SubscribeEvent public void breakLand(BlockEvent.BreakEvent e){if(!e.getWorld().isRemote&&e.getPlayer() instanceof EntityPlayerMP){EntityPlayerMP p=(EntityPlayerMP)e.getPlayer();if(!LandService.canModify(p,e.getPos())||protectedShop(p,e.getPos())){e.setCanceled(true);p.sendMessage(Texts.text("&c보호된 영지 또는 상점가에서는 블록을 파괴할 수 없습니다."));}}}
    @SubscribeEvent public void placeLand(BlockEvent.PlaceEvent e){if(!e.getWorld().isRemote&&e.getPlayer() instanceof EntityPlayerMP){EntityPlayerMP p=(EntityPlayerMP)e.getPlayer();if(!LandService.canModify(p,e.getPos())||protectedShop(p,e.getPos())){e.setCanceled(true);p.sendMessage(Texts.text("&c보호된 영지 또는 상점가에서는 블록을 설치할 수 없습니다."));}}}
    @SubscribeEvent public void useLand(PlayerInteractEvent.RightClickBlock e){if(e.getWorld().isRemote||!(e.getEntityPlayer() instanceof EntityPlayerMP))return;EntityPlayerMP player=(EntityPlayerMP)e.getEntityPlayer();if(e.getHand()==net.minecraft.util.EnumHand.MAIN_HAND&&RandomBoxService.boxId(e.getItemStack())>0){e.setCanceled(true);RandomBoxService.open(player,e.getItemStack());return;}if(e.getHand()==net.minecraft.util.EnumHand.MAIN_HAND&&LandVoucher.isVoucher(e.getItemStack())){e.setCanceled(true);LandService.claim(player);return;}if(!LandService.canModify(player,e.getPos())){e.setCanceled(true);player.sendMessage(Texts.text("&c다른 플레이어의 영지입니다."));}}
    @SubscribeEvent public void useLandVoucher(PlayerInteractEvent.RightClickItem e){if(e.getWorld().isRemote||!(e.getEntityPlayer() instanceof EntityPlayerMP))return;EntityPlayerMP player=(EntityPlayerMP)e.getEntityPlayer();if(RandomBoxService.boxId(e.getItemStack())>0){e.setCanceled(true);RandomBoxService.open(player,e.getItemStack());return;}if(LandVoucher.isVoucher(e.getItemStack())){e.setCanceled(true);LandService.claim(player);}}
    @SubscribeEvent public void explosionLand(ExplosionEvent.Detonate e){if(!e.getWorld().isRemote){DonationData data=DonationData.get(e.getWorld());e.getAffectedBlocks().removeIf(pos->LandService.owner(e.getWorld(),pos)!=null||data.inShopRegion(e.getWorld().provider.getDimension(),pos.getX(),pos.getZ()));}}
    @SubscribeEvent public void protectedMobSpawn(LivingSpawnEvent.CheckSpawn e){
        if(!e.getWorld().isRemote&&isHostile(e.getEntityLiving())&&isMobProtected(e.getWorld(),e.getEntityLiving().getPosition()))e.setResult(Event.Result.DENY);
    }
    @SubscribeEvent public void protectedMobJoin(EntityJoinWorldEvent e){
        if(!e.getWorld().isRemote&&e.getEntity() instanceof EntityLiving&&isHostile((EntityLiving)e.getEntity())&&isMobProtected(e.getWorld(),e.getEntity().getPosition()))e.setCanceled(true);
    }
    @SubscribeEvent public void protectedMobInside(LivingEvent.LivingUpdateEvent e){
        EntityLiving living=e.getEntityLiving() instanceof EntityLiving?(EntityLiving)e.getEntityLiving():null;
        if(living!=null&&!living.world.isRemote&&isHostile(living)&&living.ticksExisted%20==0&&isMobProtected(living.world,living.getPosition()))living.setDead();
    }
    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.side.isServer()){MinecraftServer server=net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();if(server!=null&&server.getTickCounter()%100==0)ShopService.updatePrices(server);}}
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||e.player.world.isRemote||!(e.player instanceof EntityPlayerMP))return;EntityPlayerMP p=(EntityPlayerMP)e.player;UUID id=p.getUniqueID();
        PendingTravel pt=pending.get(id);if(pt!=null){if(p.getDistanceSq(pt.x,pt.y,pt.z)>.09){pending.remove(id);p.sendMessage(Texts.text("&c움직여서 이동이 취소되었습니다."));}else if(p.world.getTotalWorldTime()>=pt.executeAt){pending.remove(id);DonationData d=DonationData.get(p.world);PlayerData pd=d.player(id);pd.back=LocationData.of(p);d.markDirty();teleportNow(p,pt.target);cooldown.put(id,System.currentTimeMillis());p.sendMessage(Texts.text("&a안전하게 이동했습니다."));EconomyService.sync(p);}}
        if(p.ticksExisted%10==0){
            checkLandEntry(p);
            Long until=borderUntil.get(id);
            if(until!=null){if(System.currentTimeMillis()<until)drawLandBorders(p);else borderUntil.remove(id);}
        }
    }
    public void showLandBorders(EntityPlayerMP player){
        DonationData data=DonationData.get(player.world);
        if(data.claimCount(player.getUniqueID())==0){player.sendMessage(Texts.text("&c먼저 /영지설정으로 첫 영지를 등록해 주세요."));return;}
        borderUntil.put(player.getUniqueID(),System.currentTimeMillis()+8000);
        drawLandBorders(player);
        player.sendMessage(Texts.text("&a주변 내 영지의 바깥 경계를 8초 동안 표시합니다."));
    }
    private void checkLandEntry(EntityPlayerMP player){
        UUID owner=LandService.owner(player.world,player.getPosition());
        String current=owner==null?"":owner.toString();
        String previous=lastLand.put(player.getUniqueID(),current);
        if(owner==null||current.equals(previous))return;
        GameProfile profile=player.getServer().getPlayerProfileCache().getProfileByUUID(owner);
        String name=profile!=null?profile.getName():owner.toString().substring(0,8);
        player.connection.sendPacket(new SPacketTitle(5,50,5));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.SUBTITLE,Texts.text("&f영지에 입장하셨습니다")));
        player.connection.sendPacket(new SPacketTitle(SPacketTitle.Type.TITLE,Texts.text("&6"+name+"님")));
    }
    private void drawLandBorders(EntityPlayerMP player){
        DonationData data=DonationData.get(player.world);
        UUID id=player.getUniqueID();
        int centerX=player.getPosition().getX()>>4,centerZ=player.getPosition().getZ()>>4;
        double y=player.posY+0.15;
        for(int cx=centerX-1;cx<=centerX+1;cx++)for(int cz=centerZ-1;cz<=centerZ+1;cz++){
            if(!id.equals(data.landClaims.get(DonationData.claimKey(player.dimension,cx,cz))))continue;
            int minX=cx*16,minZ=cz*16;
            if(!id.equals(data.landClaims.get(DonationData.claimKey(player.dimension,cx-1,cz))))for(int n=0;n<=16;n+=2)borderPoint(player,minX,y,minZ+n);
            if(!id.equals(data.landClaims.get(DonationData.claimKey(player.dimension,cx+1,cz))))for(int n=0;n<=16;n+=2)borderPoint(player,minX+16,y,minZ+n);
            if(!id.equals(data.landClaims.get(DonationData.claimKey(player.dimension,cx,cz-1))))for(int n=0;n<=16;n+=2)borderPoint(player,minX+n,y,minZ);
            if(!id.equals(data.landClaims.get(DonationData.claimKey(player.dimension,cx,cz+1))))for(int n=0;n<=16;n+=2)borderPoint(player,minX+n,y,minZ+16);
        }
    }
    private void borderPoint(EntityPlayerMP player,double x,double y,double z){
        player.connection.sendPacket(new SPacketParticles(EnumParticleTypes.END_ROD,false,(float)x,(float)y,(float)z,0,0,0,0,1));
    }
    private boolean protectedShop(EntityPlayerMP player,BlockPos pos){
        return DonationData.get(player.world).inShopRegion(player.dimension,pos.getX(),pos.getZ());
    }
    private boolean isMobProtected(World world,BlockPos pos){
        DonationData data=DonationData.get(world);
        return LandService.owner(world,pos)!=null||data.inShopRegion(world.provider.getDimension(),pos.getX(),pos.getZ());
    }
    private boolean isHostile(EntityLivingBase living){
        return living instanceof IMob||living.isCreatureType(EnumCreatureType.MONSTER,false);
    }
    public void requestTravel(EntityPlayerMP p,int action){DonationData d=DonationData.get(p.world);PlayerData pd=d.player(p.getUniqueID());LocationData target=action==0?pd.home:action==1?d.trade:pd.back;if(target==null){p.sendMessage(Texts.text("&c해당 이동 위치가 설정되지 않았습니다."));return;}Long last=cooldown.get(p.getUniqueID());long remain=last==null?0:ModConfig.teleportCooldownSeconds*1000L-(System.currentTimeMillis()-last);if(remain>0){p.sendMessage(Texts.text("&c이동 쿨다운: "+((remain+999)/1000)+"초"));return;}if(pending.containsKey(p.getUniqueID())){p.sendMessage(Texts.text("&e이미 이동을 준비 중입니다."));return;}long at=p.world.getTotalWorldTime()+ModConfig.teleportDelaySeconds*20L;pending.put(p.getUniqueID(),new PendingTravel(p.posX,p.posY,p.posZ,target,at));p.sendMessage(Texts.text("&e"+ModConfig.teleportDelaySeconds+"초 후 이동합니다. 움직이면 취소됩니다."));}
    private void teleportNow(EntityPlayerMP p,LocationData raw){MinecraftServer s=p.getServer();WorldServer w=s.getWorld(raw.dimension);if(w==null){p.sendMessage(Texts.text("&c대상 차원을 찾을 수 없습니다."));return;}LocationData loc=safe(w,raw);if(p.dimension!=loc.dimension)p.changeDimension(loc.dimension,new FixedTeleporter(loc));else p.connection.setPlayerLocation(loc.x,loc.y,loc.z,loc.yaw,loc.pitch);p.fallDistance=0;}
    private LocationData safe(WorldServer w,LocationData l){BlockPos base=new BlockPos(l.x,l.y,l.z);for(int dy=0;dy<=12;dy++){for(int sign:new int[]{1,-1}){BlockPos feet=base.up(dy*sign);if(feet.getY()<2||feet.getY()>253)continue;if(w.isAirBlock(feet)&&w.isAirBlock(feet.up())&&w.getBlockState(feet.down()).getMaterial().isSolid())return new LocationData(l.dimension,feet.getX()+.5,feet.getY(),feet.getZ()+.5,l.yaw,l.pitch);}}return l;}
    private static class PendingTravel{final double x,y,z;final LocationData target;final long executeAt;PendingTravel(double x,double y,double z,LocationData target,long executeAt){this.x=x;this.y=y;this.z=z;this.target=target;this.executeAt=executeAt;}}
    private static class FixedTeleporter implements ITeleporter{final LocationData l;FixedTeleporter(LocationData l){this.l=l;}public void placeEntity(World world,Entity entity,float yaw){entity.setLocationAndAngles(l.x,l.y,l.z,l.yaw,l.pitch);}}
}
