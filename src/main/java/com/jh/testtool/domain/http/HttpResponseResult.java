package com.jh.testtool.domain.http;

import java.util.List;
import java.util.Map;

public record HttpResponseResult(
        int statusCode,
        Map<String, List<String>> headers,
        String body
) {
    public HttpResponseResult {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? "" : body;
    }
}
