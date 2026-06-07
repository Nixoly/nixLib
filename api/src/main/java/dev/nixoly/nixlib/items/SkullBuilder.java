package dev.nixoly.nixlib.items;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;

public final class SkullBuilder {

    private static final String TEXTURE_URL_PREFIX = "http://textures.minecraft.net/texture/";

    private final ItemStack skull;

    private SkullBuilder() {
        this.skull = new ItemStack(Material.PLAYER_HEAD);
    }

    public static SkullBuilder head() {
        return new SkullBuilder();
    }

    public SkullBuilder owner(OfflinePlayer player) {
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
        }
        return this;
    }

    public SkullBuilder ownerName(String name) {
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return this;
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
        skull.setItemMeta(meta);
        return this;
    }

    public SkullBuilder texture(String base64) {
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return this;

        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            String url = extractUrl(decoded);
            applyProfile(meta, url == null ? null : URI.create(url));
        } catch (IllegalArgumentException ignored) {
            applyLegacyTexture(meta, base64);
        }

        skull.setItemMeta(meta);
        return this;
    }

    public SkullBuilder textureUrl(String url) {
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return this;
        applyProfile(meta, URI.create(url));
        skull.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return skull;
    }

    private void applyProfile(SkullMeta meta, URI textureUri) {
        if (textureUri == null) return;
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "nixlib");
        PlayerTextures textures = profile.getTextures();
        try {
            textures.setSkin(textureUri.toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception fallback) {
            applyLegacyTexture(meta, encodeUrl(textureUri.toString()));
        }
    }

    private void applyLegacyTexture(SkullMeta meta, String base64) {
        try {
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object profile = profileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "nixlib");
            Object propertyMap = profileClass.getMethod("getProperties").invoke(profile);
            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = propClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64);
            propertyMap.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(propertyMap, "textures", property);

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Throwable ignored) {
            // legacy path is best-effort
        }
    }

    private static String extractUrl(String decoded) {
        int idx = decoded.indexOf("\"url\"");
        if (idx < 0) return null;
        int start = decoded.indexOf('"', idx + 5);
        if (start < 0) return null;
        start = decoded.indexOf('"', start + 1);
        if (start < 0) return null;
        int end = decoded.indexOf('"', start + 1);
        if (end < 0) return null;
        return decoded.substring(start + 1, end);
    }

    private static String encodeUrl(String url) {
        String payload = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(payload.getBytes());
    }
}
