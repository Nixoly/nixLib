package dev.nixoly.nixlib.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtils {

    private static final Pattern BUKKIT_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern AMP_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern X_HEX = Pattern.compile("(?i)&x(&[0-9a-f]){6}");

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer AMP_LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer SECTION_LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ColorUtils() {}

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        return AMP_LEGACY.deserialize(translateBukkitHex(input));
    }

    public static Component miniMessage(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        return MINI.deserialize(input);
    }

    public static Component translate(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        return MINI.deserialize(legacyToMiniMessage(input));
    }

    public static List<Component> parseAll(List<String> lines) {
        if (lines == null) return List.of();
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(parse(line));
        return out;
    }

    public static List<Component> translateAll(List<String> lines) {
        if (lines == null) return List.of();
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(translate(line));
        return out;
    }

    public static String toLegacySection(Component component) {
        if (component == null) return "";
        return SECTION_LEGACY.serialize(component);
    }

    public static String toMiniMessage(Component component) {
        if (component == null) return "";
        return MINI.serialize(component);
    }

    public static String stripColor(String input) {
        if (input == null) return null;
        return PLAIN.serialize(translate(input));
    }

    public static String stripColor(Component component) {
        if (component == null) return "";
        return PLAIN.serialize(component);
    }

    private static String translateBukkitHex(String input) {
        if (input.indexOf('#') < 0) return input;
        Matcher m = BUKKIT_HEX.matcher(input);
        if (!m.find()) return input;
        m.reset();
        StringBuilder out = new StringBuilder(input.length());
        int last = 0;
        while (m.find()) {
            out.append(input, last, m.start()).append("&x");
            String hex = m.group(1);
            for (int i = 0; i < hex.length(); i++) {
                out.append('&').append(Character.toLowerCase(hex.charAt(i)));
            }
            last = m.end();
        }
        out.append(input, last, input.length());
        return out.toString();
    }

    private static String translateUnusualHex(String input) {
        if (input.indexOf('x') < 0 && input.indexOf('X') < 0) {
            return input;
        }
        Matcher m = X_HEX.matcher(input);
        if (!m.find()) {
            return input;
        }
        m.reset();
        StringBuilder sb = new StringBuilder(input.length());
        while (m.find()) {
            StringBuilder hex = new StringBuilder(6);
            for (int i = 0; i < m.group().length(); i++) {
                char ch = m.group().charAt(i);
                if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
                    hex.append(ch);
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement("<#" + hex + ">"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String legacyToMiniMessage(String input) {
        String unusual = translateUnusualHex(input);
        String hexed = BUKKIT_HEX.matcher(unusual).replaceAll("<#$1>");
        Matcher m = AMP_CODE.matcher(hexed);
        if (!m.find()) return hexed;
        m.reset();
        StringBuilder sb = new StringBuilder(hexed.length());
        while (m.find()) {
            String code = m.group(1).toLowerCase();
            String tag = switch (code) {
                case "0" -> "<black>";
                case "1" -> "<dark_blue>";
                case "2" -> "<dark_green>";
                case "3" -> "<dark_aqua>";
                case "4" -> "<dark_red>";
                case "5" -> "<dark_purple>";
                case "6" -> "<gold>";
                case "7" -> "<gray>";
                case "8" -> "<dark_gray>";
                case "9" -> "<blue>";
                case "a" -> "<green>";
                case "b" -> "<aqua>";
                case "c" -> "<red>";
                case "d" -> "<light_purple>";
                case "e" -> "<yellow>";
                case "f" -> "<white>";
                case "k" -> "<obfuscated>";
                case "l" -> "<bold>";
                case "m" -> "<strikethrough>";
                case "n" -> "<underlined>";
                case "o" -> "<italic>";
                case "r" -> "<reset>";
                default -> m.group(0);
            };
            m.appendReplacement(sb, Matcher.quoteReplacement(tag));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
