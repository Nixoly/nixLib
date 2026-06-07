package dev.nixoly.nixlib.config.yaml;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlWriter {

    private static final String INDENT = "  ";

    private final StringBuilder out = new StringBuilder();
    private final Map<String, List<String>> comments;

    private YamlWriter(Map<String, List<String>> comments) {
        this.comments = comments == null ? Map.of() : comments;
    }

    public static String write(Map<String, Object> root, Map<String, List<String>> comments) {
        YamlWriter w = new YamlWriter(comments);
        w.writeMap(root, "", 0);
        return w.out.toString();
    }

    private void writeMap(Map<String, Object> map, String pathPrefix, int depth) {
        String indent = INDENT.repeat(depth);
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullPath = pathPrefix.isEmpty() ? key : pathPrefix + "." + key;

            List<String> commentLines = comments.get(fullPath);
            if (commentLines != null && !commentLines.isEmpty()) {
                if (!first && depth == 0) out.append('\n');
                for (String line : commentLines) {
                    out.append(indent).append("# ").append(line).append('\n');
                }
            }

            out.append(indent).append(escapeKey(key)).append(':');

            if (value instanceof Map<?, ?> nested) {
                if (nested.isEmpty()) {
                    out.append(" {}\n");
                } else {
                    out.append('\n');
                    writeMap(asStringMap(nested), fullPath, depth + 1);
                }
            } else if (value instanceof Collection<?> col) {
                writeList(col, depth);
            } else {
                out.append(' ').append(renderScalar(value)).append('\n');
            }
            first = false;
        }
    }

    private void writeList(Collection<?> col, int depth) {
        if (col.isEmpty()) {
            out.append(" []\n");
            return;
        }
        out.append('\n');
        String itemIndent = INDENT.repeat(depth);
        for (Object item : col) {
            out.append(itemIndent).append("- ");
            if (item instanceof Map<?, ?> nested) {
                if (nested.isEmpty()) {
                    out.append("{}\n");
                } else {
                    out.append('\n');
                    writeMap(asStringMap(nested), "", depth + 2);
                }
            } else if (item instanceof Collection<?>) {
                out.append(renderInlineList((Collection<?>) item)).append('\n');
            } else {
                out.append(renderScalar(item)).append('\n');
            }
        }
    }

    private String renderInlineList(Collection<?> col) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : col) {
            if (!first) sb.append(", ");
            sb.append(renderScalar(item));
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    private static LinkedHashMap<String, Object> asStringMap(Map<?, ?> map) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> e : map.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }

    private static String renderScalar(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        String s = value.toString();
        return needsQuoting(s) ? quote(s) : s;
    }

    private static boolean needsQuoting(String s) {
        if (s.isEmpty()) return true;
        if (s.matches("(?i)^(true|false|null|yes|no|on|off|~)$")) return true;
        if (s.matches("^-?\\d+(\\.\\d+)?$")) return true;
        char first = s.charAt(0);
        if (first == '!' || first == '&' || first == '*' || first == '[' || first == ']'
                || first == '{' || first == '}' || first == ',' || first == '#' || first == '|'
                || first == '>' || first == '\'' || first == '"' || first == '%' || first == '@'
                || first == '`' || first == ' ') {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ':' && (i == s.length() - 1 || s.charAt(i + 1) == ' ')) return true;
            if (c == '\n' || c == '\r' || c == '\t' || c == '\0') return true;
            if (c == '#' && i > 0 && s.charAt(i - 1) == ' ') return true;
        }
        return s.charAt(s.length() - 1) == ' ';
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\0' -> sb.append("\\0");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String escapeKey(String key) {
        if (key.matches("[A-Za-z0-9_\\-]+")) return key;
        return quote(key);
    }
}
