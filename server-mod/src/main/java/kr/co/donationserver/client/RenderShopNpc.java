package kr.co.donationserver.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import kr.co.donationserver.entity.EntityShopNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RenderShopNpc extends RenderLiving<EntityShopNpc> {
    private final Map<String, ResourceLocation> textures = new HashMap<>();
    private final Set<String> requested = new HashSet<>();

    public RenderShopNpc(RenderManager manager) { super(manager, new ModelPlayer(0, false), 0.5F); }

    @Override protected ResourceLocation getEntityTexture(EntityShopNpc npc) {
        UUID uuid;
        try { uuid = UUID.fromString(npc.getSkinUuid()); }
        catch (IllegalArgumentException ex) { uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + npc.getSkinName()).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        String textureValue = npc.getSkinTexture();
        String cacheKey = uuid.toString() + ':' + textureValue.hashCode();
        ResourceLocation texture = textures.get(cacheKey);
        if (texture != null) return texture;
        if (requested.add(cacheKey) && !npc.getSkinUuid().isEmpty() && !textureValue.isEmpty()) {
            String requestedKey = cacheKey;
            GameProfile profile = new GameProfile(uuid, npc.getSkinName());
            String signature = npc.getSkinSignature();
            profile.getProperties().put("textures", signature.isEmpty()
                    ? new Property("textures", textureValue)
                    : new Property("textures", textureValue, signature));
            Minecraft.getMinecraft().getSkinManager().loadProfileTextures(profile, (type, location, skin) -> {
                if (type == MinecraftProfileTexture.Type.SKIN) textures.put(requestedKey, location);
            }, true);
        }
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }
}
