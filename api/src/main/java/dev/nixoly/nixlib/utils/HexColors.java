package dev.nixoly.nixlib.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HexColors {

    private static final Pattern PLAIN_HEX = Pattern.compile("^#?([0-9a-fA-F]{6})$");
    private static final Pattern SHORT_HEX = Pattern.compile("^#?([0-9a-fA-F]{3})$");
    private static final Pattern BUKKIT_HEX = Pattern.compile("^&#([0-9a-fA-F]{6})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNUSUAL_HEX = Pattern.compile("(?i)^&x(&[0-9a-f]){6}$");
    private static final Pattern EMBEDDED_HEX = Pattern.compile("#?([0-9a-fA-F]{6})");

    private HexColors() {
    }

    public static @Nullable String normalize(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String candidate = plainInput(text);
        if (candidate.isEmpty()) {
            String legacy = extractLegacySectionHex(text);
            if (legacy != null) {
                return "#" + legacy.toUpperCase();
            }
            return null;
        }
        Matcher bukkit = BUKKIT_HEX.matcher(candidate);
        if (bukkit.matches()) {
            return "#" + bukkit.group(1).toUpperCase();
        }
        Matcher unusual = UNUSUAL_HEX.matcher(candidate);
        if (unusual.matches()) {
            StringBuilder hex = new StringBuilder(6);
            for (int i = 0; i < unusual.group().length(); i++) {
                char ch = unusual.group().charAt(i);
                if (isHexDigit(ch)) {
                    hex.append(ch);
                }
            }
            if (hex.length() == 6) {
                return "#" + hex.toString().toUpperCase();
            }
        }
        Matcher exact = PLAIN_HEX.matcher(candidate);
        if (exact.matches()) {
            return "#" + exact.group(1).toUpperCase();
        }
        Matcher shortHex = SHORT_HEX.matcher(candidate);
        if (shortHex.matches()) {
            String s = shortHex.group(1);
            return "#" + ("" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2)).toUpperCase();
        }
        Matcher embedded = EMBEDDED_HEX.matcher(candidate);
        if (!candidate.matches("^[#0-9a-fA-F]+$") && embedded.find()) {
            return "#" + embedded.group(1).toUpperCase();
        }
        return null;
    }

    public static boolean isValid(@Nullable String text) {
        return normalize(text) != null;
    }

    public static @NotNull TextColor color(@NotNull String normalized) {
        TextColor color = TextColor.fromCSSHexString(normalized.startsWith("#") ? normalized : "#" + normalized);
        return color != null ? color : NamedTextColor.WHITE;
    }

    private static @NotNull String plainInput(@NotNull String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.indexOf('§') >= 0) {
            try {
                trimmed = PlainTextComponentSerializer.plainText().serialize(
                        LegacyComponentSerializer.legacySection().deserialize(trimmed));
            } catch (Throwable ignored) {
                trimmed = trimmed.replaceAll("§.", "");
            }
        }
        trimmed = trimmed.replace('\u00a0', ' ').trim();
        return trimmed;
    }

    private static @Nullable String extractLegacySectionHex(@NotNull String text) {
        if (text.indexOf('§') < 0) {
            return null;
        }
        StringBuilder hex = new StringBuilder(6);
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                char next = text.charAt(i + 1);
                if (isHexDigit(next)) {
                    hex.append(next);
                }
            }
        }
        if (hex.length() < 6) {
            return null;
        }
        return hex.substring(hex.length() - 6);
    }

    private static boolean isHexDigit(char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
    }
}
