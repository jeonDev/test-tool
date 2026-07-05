package com.jh.testtool.domain.testcase;

import java.util.List;

public record HttpTestCase(List<HttpTestStep> steps) {
    public HttpTestCase {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
