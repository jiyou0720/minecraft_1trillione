package kr.co.donationserver.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import java.util.*;

public class DonationData extends WorldSavedData {
    private static final String ID = "donationserver_data";
    private final Map<UUID, PlayerData> players = new HashMap<>();
    public long serverTotal;
    public boolean endingTriggered;
    public LocationData trade, joinSpawn;
    public final Map<Integer, ShopData> shops = new LinkedHashMap<>();
    public final Map<Integer, RandomBoxData> randomBoxes = new LinkedHashMap<>();
    public final Map<String, UUID> landClaims = new HashMap<>();
    public final Map<UUID, Set<UUID>> landTrusted = new HashMap<>();
    public long landExpansionPrice;
    public int maxLandClaims, nextShopId = 1;
    public int nextRandomBoxId = 1;
    public long nextShopPriceChange;
    public int shopRegionDimension, shopRegionX, shopRegionZ, shopRegionRadius;

    public DonationData() { super(ID); }
    public DonationData(String name) { super(name); }

    public static DonationData get(World world) {
        World root = world.getMinecraftServer().getWorld(0);
        DonationData data = (DonationData) root.getPerWorldStorage().getOrLoadData(DonationData.class, ID);
        if (data == null) {
            data = new DonationData();
            root.getPerWorldStorage().setData(ID, data);
            data.markDirty();
        }
        return data;
    }

    public PlayerData player(UUID id) {
        PlayerData data = players.get(id);
        if (data == null) {
            data = new PlayerData();
            players.put(id, data);
            markDirty();
        }
        return data;
    }

    public static String claimKey(int dimension, int chunkX, int chunkZ) { return dimension + ":" + chunkX + ":" + chunkZ; }
    public int claimCount(UUID owner) {
        int count = 0;
        for (UUID id : landClaims.values()) if (owner.equals(id)) count++;
        return count;
    }
    public Set<UUID> trusted(UUID owner) {
        Set<UUID> users = landTrusted.get(owner);
        if (users == null) {
            users = new HashSet<>();
            landTrusted.put(owner, users);
        }
        return users;
    }

    public boolean inShopRegion(int dimension, int x, int z) {
        if (shopRegionRadius <= 0 || dimension != shopRegionDimension) return false;
        long dx = (long) x - shopRegionX, dz = (long) z - shopRegionZ;
        return dx * dx + dz * dz <= (long) shopRegionRadius * shopRegionRadius;
    }

    @Override public void readFromNBT(NBTTagCompound n) {
        serverTotal = n.getLong("serverTotal");
        endingTriggered = n.getBoolean("endingTriggered");
        trade = n.hasKey("trade", 10) ? LocationData.read(n.getCompoundTag("trade")) : null;
        joinSpawn = n.hasKey("joinSpawn", 10) ? LocationData.read(n.getCompoundTag("joinSpawn")) : null;
        shopRegionDimension = n.getInteger("shopRegionDimension");
        shopRegionX = n.getInteger("shopRegionX");
        shopRegionZ = n.getInteger("shopRegionZ");
        shopRegionRadius = Math.max(0, n.getInteger("shopRegionRadius"));
        players.clear();
        NBTTagCompound savedPlayers = n.getCompoundTag("players");
        for (String key : savedPlayers.getKeySet()) try {
            players.put(UUID.fromString(key), PlayerData.read(savedPlayers.getCompoundTag(key)));
        } catch (IllegalArgumentException ignored) {}
        shops.clear();
        NBTTagList savedShops = n.getTagList("shops", 10);
        for (int i = 0; i < savedShops.tagCount(); i++) try {
            ShopData shop = ShopData.read(savedShops.getCompoundTagAt(i));
            shops.put(shop.id, shop);
        } catch (IllegalArgumentException ignored) {}
        randomBoxes.clear();
        NBTTagList savedBoxes = n.getTagList("randomBoxes", 10);
        for (int i = 0; i < savedBoxes.tagCount(); i++) {
            RandomBoxData box = RandomBoxData.read(savedBoxes.getCompoundTagAt(i));
            if (box.id > 0) randomBoxes.put(box.id, box);
        }
        nextRandomBoxId = Math.max(1, n.getInteger("nextRandomBoxId"));
        for (Integer id : randomBoxes.keySet()) nextRandomBoxId = Math.max(nextRandomBoxId, id + 1);
        nextShopId = Math.max(1, n.getInteger("nextShopId"));
        for (Integer id : shops.keySet()) nextShopId = Math.max(nextShopId, id + 1);
        nextShopPriceChange = n.getLong("nextShopPriceChange");
        landExpansionPrice = Math.max(0, n.getLong("landExpansionPrice"));
        maxLandClaims = Math.max(0, n.getInteger("maxLandClaims"));
        landClaims.clear();
        NBTTagList claims = n.getTagList("landClaims", 10);
        for (int i = 0; i < claims.tagCount(); i++) {
            NBTTagCompound claim = claims.getCompoundTagAt(i);
            try {
                landClaims.put(claimKey(claim.getInteger("dimension"), claim.getInteger("chunkX"), claim.getInteger("chunkZ")),
                        UUID.fromString(claim.getString("owner")));
            } catch (IllegalArgumentException ignored) {}
        }
        landTrusted.clear();
        NBTTagList trusted = n.getTagList("landTrusted", 10);
        for (int i = 0; i < trusted.tagCount(); i++) {
            NBTTagCompound entry = trusted.getCompoundTagAt(i);
            try {
                UUID owner = UUID.fromString(entry.getString("owner"));
                Set<UUID> members = trusted(owner);
                for (String value : entry.getString("members").split(",")) try {
                    members.add(UUID.fromString(value));
                } catch (IllegalArgumentException ignored) {}
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound n) {
        n.setLong("serverTotal", serverTotal);
        n.setBoolean("endingTriggered", endingTriggered);
        if (trade != null) n.setTag("trade", trade.write());
        if (joinSpawn != null) n.setTag("joinSpawn", joinSpawn.write());
        n.setInteger("shopRegionDimension", shopRegionDimension);
        n.setInteger("shopRegionX", shopRegionX);
        n.setInteger("shopRegionZ", shopRegionZ);
        n.setInteger("shopRegionRadius", shopRegionRadius);
        NBTTagCompound savedPlayers = new NBTTagCompound();
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet())
            savedPlayers.setTag(entry.getKey().toString(), entry.getValue().write());
        n.setTag("players", savedPlayers);
        NBTTagList savedShops = new NBTTagList();
        for (ShopData shop : shops.values()) savedShops.appendTag(shop.write());
        n.setTag("shops", savedShops);
        NBTTagList savedBoxes = new NBTTagList();
        for (RandomBoxData box : randomBoxes.values()) savedBoxes.appendTag(box.write());
        n.setTag("randomBoxes", savedBoxes);
        n.setInteger("nextRandomBoxId", nextRandomBoxId);
        n.setInteger("nextShopId", nextShopId);
        n.setLong("nextShopPriceChange", nextShopPriceChange);
        n.setLong("landExpansionPrice", landExpansionPrice);
        n.setInteger("maxLandClaims", maxLandClaims);
        NBTTagList claims = new NBTTagList();
        for (Map.Entry<String, UUID> claim : landClaims.entrySet()) {
            String[] parts = claim.getKey().split(":");
            if (parts.length != 3) continue;
            NBTTagCompound entry = new NBTTagCompound();
            try {
                entry.setInteger("dimension", Integer.parseInt(parts[0]));
                entry.setInteger("chunkX", Integer.parseInt(parts[1]));
                entry.setInteger("chunkZ", Integer.parseInt(parts[2]));
                entry.setString("owner", claim.getValue().toString());
                claims.appendTag(entry);
            } catch (NumberFormatException ignored) {}
        }
        n.setTag("landClaims", claims);
        NBTTagList trusted = new NBTTagList();
        for (Map.Entry<UUID, Set<UUID>> entry : landTrusted.entrySet()) {
            NBTTagCompound saved = new NBTTagCompound();
            saved.setString("owner", entry.getKey().toString());
            StringJoiner members = new StringJoiner(",");
            for (UUID id : entry.getValue()) members.add(id.toString());
            saved.setString("members", members.toString());
            trusted.appendTag(saved);
        }
        n.setTag("landTrusted", trusted);
        return n;
    }
}
