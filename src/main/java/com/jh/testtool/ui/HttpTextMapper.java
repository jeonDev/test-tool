package com.jh.testtool.ui;

import com.jh.testtool.domain.http.HttpRequestCommand;
import com.jh.testtool.domain.http.HttpResponseResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HttpTextMapper {

    private HttpTextMapper() {
    }

    static HttpRequestCommand toRequest(String method, String url, String headerText, String body) {
        return new HttpRequestCommand(method, normalizeUrl(url), parseHeaders(headerText), body);
    }

    static HttpRequestCommand toRequest(String method, String url, Map<String, List<String>> headers, String body) {
        return new HttpRequestCommand(method, normalizeUrl(url), withDefaultJsonContentType(headers, body), body);
    }

    static String toJsonBody(Map<String, String> bodyFields) {
        if (bodyFields == null || bodyFields.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : bodyFields.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append('"')
                    .append(escapeJson(entry.getKey().trim()))
                    .append("\":\"")
                    .append(escapeJson(entry.getValue() == null ? "" : entry.getValue()))
                    .append('"');
            first = false;
        }
        builder.append('}');
        return first ? "" : builder.toString();
    }

    static Map<String, List<String>> parseHeaders(String headerText) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        if (headerText == null || headerText.isBlank()) {
            return headers;
        }

        String[] lines = headerText.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isBlank()) {
                continue;
            }
            int delimiter = line.indexOf(':');
            if (delimiter <= 0) {
                throw new IllegalArgumentException("Invalid header at line " + (index + 1) + ". Use 'Name: value'.");
            }
            String name = line.substring(0, delimiter).trim();
            String value = line.substring(delimiter + 1).trim();
            headers.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
        return headers;
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        if (trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return trimmed;
        }
        return "http://" + trimmed;
    }

    private static Map<String, List<String>> withDefaultJsonContentType(Map<String, List<String>> headers, String body) {
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        if (headers != null) {
            normalized.putAll(headers);
        }
        boolean hasContentType = normalized.keySet().stream()
                .anyMatch(name -> name.equalsIgnoreCase("Content-Type"));
        if (!hasContentType && body != null && !body.isBlank()) {
            normalized.put("Content-Type", List.of("application/json"));
        }
        return normalized;
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static String formatResponse(HttpResponseResult response) {
        StringBuilder builder = new StringBuilder();
        builder.append("Status: ").append(response.statusCode()).append(System.lineSeparator());
        response.headers().forEach((name, values) ->
                values.forEach(value -> builder
                        .append(name)
                        .append(": ")
                        .append(value)
                        .append(System.lineSeparator()))
        );
        builder.append(System.lineSeparator()).append(response.body());
        return builder.toString();
    }

    static String formatHeaders(Map<String, List<String>> headers) {
        StringBuilder builder = new StringBuilder();
        headers.forEach((name, values) ->
                values.forEach(value -> builder
                        .append(name)
                        .append(": ")
                        .append(value)
                        .append(System.lineSeparator()))
        );
        return builder.toString();
    }
}
