package kr.co.donationserver.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class EntityShopNpc extends EntityCreature {
    private static final DataParameter<String> SKIN_NAME = EntityDataManager.createKey(EntityShopNpc.class, DataSerializers.STRING);
    private static final DataParameter<String> SKIN_UUID = EntityDataManager.createKey(EntityShopNpc.class, DataSerializers.STRING);
    private static final DataParameter<String> SKIN_TEXTURE = EntityDataManager.createKey(EntityShopNpc.class, DataSerializers.STRING);
    private static final DataParameter<String> SKIN_SIGNATURE = EntityDataManager.createKey(EntityShopNpc.class, DataSerializers.STRING);

    public EntityShopNpc(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        setNoAI(true);
        enablePersistence();
    }

    @Override protected void entityInit() {
        super.entityInit();
        dataManager.register(SKIN_NAME, "Alex");
        dataManager.register(SKIN_UUID, "");
        dataManager.register(SKIN_TEXTURE, "");
        dataManager.register(SKIN_SIGNATURE, "");
    }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0);
    }

    @Override protected void initEntityAI() {}
    @Override public boolean attackEntityFrom(DamageSource source, float amount) { return false; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected boolean processInteract(EntityPlayer player, EnumHand hand) { return true; }

    public void setSkin(String name, String uuid, String texture, String signature) {
        dataManager.set(SKIN_NAME, name);
        dataManager.set(SKIN_UUID, uuid == null ? "" : uuid);
        dataManager.set(SKIN_TEXTURE, texture == null ? "" : texture);
        dataManager.set(SKIN_SIGNATURE, signature == null ? "" : signature);
    }
    public String getSkinName() { return dataManager.get(SKIN_NAME); }
    public String getSkinUuid() { return dataManager.get(SKIN_UUID); }
    public String getSkinTexture() { return dataManager.get(SKIN_TEXTURE); }
    public String getSkinSignature() { return dataManager.get(SKIN_SIGNATURE); }

    @Override public void writeEntityToNBT(NBTTagCompound n) {
        super.writeEntityToNBT(n);
        n.setString("shopSkinName", getSkinName());
        n.setString("shopSkinUuid", getSkinUuid());
        n.setString("shopSkinTexture", getSkinTexture());
        n.setString("shopSkinSignature", getSkinSignature());
    }
    @Override public void readEntityFromNBT(NBTTagCompound n) {
        super.readEntityFromNBT(n);
        setSkin(n.getString("shopSkinName"), n.getString("shopSkinUuid"),
                n.getString("shopSkinTexture"), n.getString("shopSkinSignature"));
    }
}
