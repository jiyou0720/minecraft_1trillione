package kr.co.donationserver.service;

import kr.co.donationserver.config.ModConfig;
import kr.co.donationserver.data.*;
import kr.co.donationserver.network.NetworkHandler;
import kr.co.donationserver.util.Texts;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public final class EconomyService {
    private EconomyService(){}
    public static void sync(EntityPlayerMP p){DonationData d=DonationData.get(p.world);PlayerData pd=d.player(p.getUniqueID());NetworkHandler.sync(p,pd.balance,pd.donation,d.serverTotal);}
    public static boolean donate(EntityPlayerMP p,long amount){
        if(amount<=0){p.sendMessage(Texts.text("&c기부 금액은 1원 이상이어야 합니다."));return false;}
        DonationData d=DonationData.get(p.world);PlayerData pd=d.player(p.getUniqueID());
        if(pd.balance<amount){p.sendMessage(Texts.text("&c잔액이 부족합니다. 현재 잔액: "+Texts.money(pd.balance)));return false;}
        long old=pd.donation;pd.balance-=amount;pd.donation=safeAdd(pd.donation,amount);d.serverTotal=safeAdd(d.serverTotal,amount);d.markDirty();
        p.sendMessage(Texts.text("&a"+Texts.money(amount)+"을 기부했습니다. 개인 누적: "+Texts.money(pd.donation)));
        checkMilestones(p,pd,old);checkEnding(p.getServer(),d);sync(p);return true;
    }
    public static boolean transfer(EntityPlayerMP from, EntityPlayerMP to, long amount) {
        if (from.getUniqueID().equals(to.getUniqueID())) {
            from.sendMessage(Texts.text("&c자기 자신에게는 송금할 수 없습니다."));
            return false;
        }
        DonationData data = DonationData.get(from.world);
        PlayerData source = data.player(from.getUniqueID());
        PlayerData target = data.player(to.getUniqueID());
        if (amount < 1 || source.balance < amount) {
            from.sendMessage(Texts.text("&c송금 금액이 잘못되었거나 잔액이 부족합니다. 현재 잔액: " + Texts.money(source.balance)));
            return false;
        }
        if (target.balance > Long.MAX_VALUE - amount) {
            from.sendMessage(Texts.text("&c상대방의 잔액 한도를 초과해 송금할 수 없습니다."));
            return false;
        }
        source.balance -= amount;
        target.balance += amount;
        data.markDirty();
        sync(from);
        sync(to);
        from.sendMessage(Texts.text("&a" + to.getName() + "님께 " + Texts.money(amount) + "을 송금했습니다."));
        to.sendMessage(Texts.text("&a" + from.getName() + "님에게서 " + Texts.money(amount) + "을 받았습니다."));
        return true;
    }
    private static long safeAdd(long a,long b){return Long.MAX_VALUE-a<b?Long.MAX_VALUE:a+b;}
    private static void checkMilestones(EntityPlayerMP p,PlayerData pd,long old){
        for(int i=0;i<ModConfig.milestones.length;i++){long m=ModConfig.milestones[i];if(old<m&&pd.donation>=m){String title=i<ModConfig.milestoneTitles.length?ModConfig.milestoneTitles[i]:"기부자";pd.title=title;p.getServer().getPlayerList().sendMessage(Texts.text("&6[기부] &e"+p.getName()+"님이 "+Texts.money(m)+"을 달성해 ["+title+"] 칭호를 획득했습니다!"));}}
    }
    private static void checkEnding(MinecraftServer server,DonationData d){
        if(d.endingTriggered||d.serverTotal<ModConfig.goal)return;d.endingTriggered=true;d.markDirty();
        server.getPlayerList().sendMessage(Texts.text("&6&l축하합니다! 서버 전체 기부금이 "+Texts.money(ModConfig.goal)+"을 달성했습니다!"));
        for(EntityPlayerMP p:server.getPlayerList().getPlayers())for(int i=0;i<3;i++)spawnFirework(p,i);
    }
    private static void spawnFirework(EntityPlayerMP p,int i){ItemStack rocket=new ItemStack(net.minecraft.init.Items.FIREWORKS);NBTTagCompound root=new NBTTagCompound(),fireworks=new NBTTagCompound(),explosion=new NBTTagCompound();NBTTagList list=new NBTTagList();explosion.setByte("Type",(byte)1);explosion.setIntArray("Colors",new int[]{0xFFD700,0x55FF55});explosion.setBoolean("Trail",true);explosion.setBoolean("Flicker",true);list.appendTag(explosion);fireworks.setTag("Explosions",list);fireworks.setByte("Flight",(byte)1);root.setTag("Fireworks",fireworks);rocket.setTagCompound(root);EntityFireworkRocket f=new EntityFireworkRocket(p.world,p.posX+(i-1)*1.5,p.posY,p.posZ,rocket);p.world.spawnEntity(f);}
}
