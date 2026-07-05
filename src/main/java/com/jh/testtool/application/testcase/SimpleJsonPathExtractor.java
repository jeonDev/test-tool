package com.jh.testtool.application.testcase;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SimpleJsonPathExtractor {

    private SimpleJsonPathExtractor() {
    }

    static String extract(String json, String path) {
        if (json == null || json.isBlank() || path == null || path.isBlank()) {
            return "";
        }

        String current = json.trim();
        for (String segment : path.split("\\.")) {
            current = extractSegment(current, segment);
            if (current.isBlank()) {
                return "";
            }
        }
        return unquote(current.trim());
    }

    private static String extractSegment(String json, String segment) {
        String key = segment;
        Integer arrayIndex = null;
        var arrayMatcher = Pattern.compile("(.+)\\[(\\d+)]").matcher(segment);
        if (arrayMatcher.matches()) {
            key = arrayMatcher.group(1);
            arrayIndex = Integer.parseInt(arrayMatcher.group(2));
        }

        String value = findObjectValue(json, key);
        if (arrayIndex != null) {
            value = findArrayValue(value, arrayIndex);
        }
        return value;
    }

    private static String findObjectValue(String json, String key) {
        String needle = "\"" + Pattern.quote(key) + "\"\\s*:";
        Matcher matcher = Pattern.compile(needle).matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return readJsonValue(json, matcher.end()).trim();
    }

    private static String findArrayValue(String json, int targetIndex) {
        String trimmed = json.trim();
        if (!trimmed.startsWith("[")) {
            return "";
        }

        int index = 0;
        int offset = 1;
        while (offset < trimmed.length()) {
            offset = skipWhitespaceAndCommas(trimmed, offset);
            if (offset >= trimmed.length() || trimmed.charAt(offset) == ']') {
                return "";
            }
            String value = readJsonValue(trimmed, offset);
            if (index == targetIndex) {
                return value;
            }
            offset += value.length();
            index++;
        }
        return "";
    }

    private static int skipWhitespaceAndCommas(String text, int offset) {
        int cursor = offset;
        while (cursor < text.length()) {
            char c = text.charAt(cursor);
            if (!Character.isWhitespace(c) && c != ',') {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private static String readJsonValue(String json, int start) {
        int cursor = skipWhitespaceAndCommas(json, start);
        if (cursor >= json.length()) {
            return "";
        }

        char first = json.charAt(cursor);
        if (first == '"') {
            return readQuoted(json, cursor);
        }
        if (first == '{' || first == '[') {
            return readStructured(json, cursor, first == '{' ? '}' : ']');
        }
        int end = cursor;
        while (end < json.length() && !List.of(',', '}', ']').contains(json.charAt(end))) {
            end++;
        }
        return json.substring(cursor, end);
    }

    private static String readQuoted(String json, int start) {
        boolean escaped = false;
        for (int cursor = start + 1; cursor < json.length(); cursor++) {
            char c = json.charAt(cursor);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return json.substring(start, cursor + 1);
            }
        }
        return "";
    }

    private static String readStructured(String json, int start, char close) {
        char open = json.charAt(start);
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int cursor = start; cursor < json.length(); cursor++) {
            char c = json.charAt(cursor);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return json.substring(start, cursor + 1);
                }
            }
        }
        return "";
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return trimmed;
    }
}
