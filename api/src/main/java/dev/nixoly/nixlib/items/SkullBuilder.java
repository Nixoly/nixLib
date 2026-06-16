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

    public SkullBuilder texture(String value) {
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null || value == null || value.isBlank()) {
            return this;
        }
        String trimmed = value.trim();
        String url = resolveTextureUrl(trimmed);

        boolean applied = url != null && applyProfile(meta, safeUri(url));
        if (!applied) {
            String base64 = url != null ? encodeUrl(url) : trimmed;
            applyLegacyTexture(meta, base64);
        }

        skull.setItemMeta(meta);
        return this;
    }

    static String resolveTextureUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String decoded = tryDecodeBase64(trimmed);
        if (decoded != null && decoded.contains("\"url\"")) {
            return extractUrl(decoded);
        }
        if (isTextureHash(trimmed)) {
            return TEXTURE_URL_PREFIX + trimmed;
        }
        return null;
    }

    private static String tryDecodeBase64(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isTextureHash(String value) {
        if (value.length() < 16) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static URI safeUri(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public SkullBuilder textureUrl(String url) {
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return this;
        applyProfile(meta, safeUri(url));
        skull.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return skull;
    }

    private boolean applyProfile(SkullMeta meta, URI textureUri) {
        if (textureUri == null) return false;
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "nixlib");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(textureUri.toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;
        } catch (Throwable ignored) {
            return false;
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

            if (!assignProfileField(meta, profile)) {
                trySetProfileViaSetter(meta, profile);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean assignProfileField(SkullMeta meta, Object gameProfile) {
        Class<?> type = meta.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getName().toLowerCase(java.util.Locale.ROOT).contains("profile")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (field.getType().isAssignableFrom(gameProfile.getClass())) {
                        field.set(meta, gameProfile);
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static void trySetProfileViaSetter(SkullMeta meta, Object gameProfile) {
        for (var method : meta.getClass().getMethods()) {
            if (!method.getName().equals("setProfile") || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isAssignableFrom(gameProfile.getClass())) {
                try {
                    method.setAccessible(true);
                    method.invoke(meta, gameProfile);
                    return;
                } catch (Throwable ignored) {
                    return;
                }
            }
        }
    }

    static String extractUrl(String decoded) {
        if (decoded == null) return null;
        int idx = decoded.indexOf("\"url\"");
        if (idx < 0) return null;
        int start = decoded.indexOf('"', idx + 5);
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
