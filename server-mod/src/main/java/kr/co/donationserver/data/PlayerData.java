package kr.co.donationserver.data;

import net.minecraft.nbt.NBTTagCompound;

public class PlayerData {
    public long balance, donation;
    public boolean tutorialComplete;
    public boolean tutorialRewardGiven;
    public boolean joinedOnce;
    public int tutorialStep;
    public String title = "";
    public LocationData home, back;

    public NBTTagCompound write() {
        NBTTagCompound n = new NBTTagCompound();
        n.setLong("balance", balance); n.setLong("donation", donation);
        n.setBoolean("tutorialComplete", tutorialComplete); n.setBoolean("tutorialRewardGiven", tutorialRewardGiven);
        n.setBoolean("joinedOnce", joinedOnce); n.setInteger("tutorialStep", tutorialStep); n.setString("title", title == null ? "" : title);
        if (home != null) n.setTag("home", home.write());
        if (back != null) n.setTag("back", back.write());
        return n;
    }

    public static PlayerData read(NBTTagCompound n) {
        PlayerData d = new PlayerData();
        d.balance = n.getLong("balance"); d.donation = n.getLong("donation");
        d.tutorialComplete = n.getBoolean("tutorialComplete"); d.tutorialRewardGiven = n.getBoolean("tutorialRewardGiven");
        d.joinedOnce = n.hasKey("joinedOnce") ? n.getBoolean("joinedOnce") : true;
        d.tutorialStep = Math.max(0,n.getInteger("tutorialStep")); d.title = n.getString("title");
        if (n.hasKey("home", 10)) d.home = LocationData.read(n.getCompoundTag("home"));
        if (n.hasKey("back", 10)) d.back = LocationData.read(n.getCompoundTag("back"));
        return d;
    }
}
