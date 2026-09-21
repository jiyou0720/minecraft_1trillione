package kr.co.donationserver.data;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.ArrayList;
import java.util.List;

public class RandomBoxData {
    public int id;
    public String name;
    public final List<Reward> rewards = new ArrayList<>();

    public NBTTagCompound write() {
        NBTTagCompound n = new NBTTagCompound();
        n.setInteger("id", id);
        n.setString("name", name);
        NBTTagList list = new NBTTagList();
        for (Reward reward : rewards) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("item", reward.item.writeToNBT(new NBTTagCompound()));
            entry.setInteger("weight", reward.weight);
            list.appendTag(entry);
        }
        n.setTag("rewards", list);
        return n;
    }

    public static RandomBoxData read(NBTTagCompound n) {
        RandomBoxData box = new RandomBoxData();
        box.id = n.getInteger("id");
        box.name = n.getString("name");
        NBTTagList list = n.getTagList("rewards", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            ItemStack item = new ItemStack(entry.getCompoundTag("item"));
            if (!item.isEmpty() && entry.getInteger("weight") > 0) box.rewards.add(new Reward(item, entry.getInteger("weight")));
        }
        return box;
    }

    public static class Reward {
        public final ItemStack item;
        public final int weight;
        public Reward(ItemStack item, int weight) { this.item = item; this.weight = weight; }
    }
}
