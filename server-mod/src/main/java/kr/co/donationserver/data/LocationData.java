package kr.co.donationserver.data;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public class LocationData {
    public int dimension; public double x, y, z; public float yaw, pitch;
    public LocationData(int dimension, double x, double y, double z, float yaw, float pitch) { this.dimension=dimension; this.x=x; this.y=y; this.z=z; this.yaw=yaw; this.pitch=pitch; }
    public static LocationData of(EntityPlayerMP p) { return new LocationData(p.dimension, p.posX, p.posY, p.posZ, p.rotationYaw, p.rotationPitch); }
    public NBTTagCompound write() { NBTTagCompound n=new NBTTagCompound(); n.setInteger("dimension",dimension); n.setDouble("x",x); n.setDouble("y",y); n.setDouble("z",z); n.setFloat("yaw",yaw); n.setFloat("pitch",pitch); return n; }
    public static LocationData read(NBTTagCompound n) { return new LocationData(n.getInteger("dimension"),n.getDouble("x"),n.getDouble("y"),n.getDouble("z"),n.getFloat("yaw"),n.getFloat("pitch")); }
}
