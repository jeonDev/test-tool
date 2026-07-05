package com.jh.testtool.application.http;

import com.jh.testtool.domain.http.HttpRequestCommand;
import com.jh.testtool.domain.http.HttpResponseResult;
import org.springframework.stereotype.Service;

@Service
public class ExecuteHttpRequestUseCase {

    private final HttpRequestGateway httpRequestGateway;

    public ExecuteHttpRequestUseCase(HttpRequestGateway httpRequestGateway) {
        this.httpRequestGateway = httpRequestGateway;
    }

    public HttpResponseResult execute(HttpRequestCommand request) {
        if (request.url().isBlank()) {
            throw new IllegalArgumentException("URL is required.");
        }
        return httpRequestGateway.execute(request);
    }
}
