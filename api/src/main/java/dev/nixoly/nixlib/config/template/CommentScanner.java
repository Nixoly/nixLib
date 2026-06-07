package dev.nixoly.nixlib.config.template;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CommentScanner {

    private static final Pattern KEY_LINE = Pattern.compile("^(?<indent>[ \\t]*)(?<key>(?:\"[^\"]*\"|'[^']*'|[^#\\s:][^:]*?))\\s*:(?=\\s|$).*");

    private CommentScanner() {}

    static Map<String, List<String>> extract(String templateText) {
        if (templateText == null || templateText.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        List<String> buffer = new ArrayList<>();
        List<Frame> stack = new ArrayList<>();

        for (String raw : templateText.split("\\r?\\n", -1)) {
            String line = stripBom(raw);
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.startsWith("#")) {
                buffer.add(stripCommentMarker(trimmed));
                continue;
            }

            if (trimmed.startsWith("- ") || trimmed.equals("-")) {
                buffer.clear();
                continue;
            }

            Matcher m = KEY_LINE.matcher(line);
            if (!m.matches()) {
                buffer.clear();
                continue;
            }

            int indent = expandIndent(m.group("indent"));
            String key = unquote(m.group("key").trim());

            while (!stack.isEmpty() && stack.get(stack.size() - 1).indent >= indent) {
                stack.remove(stack.size() - 1);
            }
            stack.add(new Frame(indent, key));

            String path = joinPath(stack);
            if (!buffer.isEmpty()) {
                result.put(path, List.copyOf(buffer));
                buffer.clear();
            }
        }

        return result;
    }

    private static String stripBom(String s) {
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static String stripCommentMarker(String trimmed) {
        String body = trimmed.substring(1);
        if (!body.isEmpty() && body.charAt(0) == ' ') {
            body = body.substring(1);
        }
        return body;
    }

    private static int expandIndent(String indent) {
        int width = 0;
        for (int i = 0; i < indent.length(); i++) {
            width += indent.charAt(i) == '\t' ? 4 : 1;
        }
        return width;
    }

    private static String unquote(String key) {
        if (key.length() >= 2) {
            char first = key.charAt(0);
            char last = key.charAt(key.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return key.substring(1, key.length() - 1);
            }
        }
        return key;
    }

    private static String joinPath(List<Frame> stack) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) sb.append('.');
            sb.append(stack.get(i).key);
        }
        return sb.toString();
    }

    private record Frame(int indent, String key) {}
}
