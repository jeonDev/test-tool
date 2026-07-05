package com.jh.testtool.domain.testcase;

import com.jh.testtool.domain.http.HttpRequestCommand;
import com.jh.testtool.domain.http.HttpResponseResult;

public record StepExecutionResult(
        int stepNumber,
        String stepName,
        HttpRequestCommand resolvedRequest,
        HttpResponseResult response
) {
}
