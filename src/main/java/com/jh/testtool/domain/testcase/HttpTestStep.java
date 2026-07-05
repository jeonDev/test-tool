package com.jh.testtool.domain.testcase;

import com.jh.testtool.domain.http.HttpRequestCommand;

public record HttpTestStep(
        String name,
        HttpRequestCommand request
) {
    public HttpTestStep {
        name = name == null || name.isBlank() ? "Step" : name.trim();
    }
}
