package com.jh.testtool.domain.http;

import java.util.List;
import java.util.Map;

public record HttpRequestCommand(
        String method,
        String url,
        Map<String, List<String>> headers,
        String body
) {
    public HttpRequestCommand {
        method = method == null || method.isBlank() ? "GET" : method.trim().toUpperCase();
        url = url == null ? "" : url.trim();
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? "" : body;
    }
}
