package com.jh.testtool.application.testcase;

import com.jh.testtool.application.http.ExecuteHttpRequestUseCase;
import com.jh.testtool.domain.testcase.HttpTestCase;
import com.jh.testtool.domain.testcase.StepExecutionResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExecuteSequentialHttpTestCaseUseCase {

    private final ExecuteHttpRequestUseCase executeHttpRequestUseCase;
    private final ResponseVariableResolver responseVariableResolver;

    public ExecuteSequentialHttpTestCaseUseCase(
            ExecuteHttpRequestUseCase executeHttpRequestUseCase,
            ResponseVariableResolver responseVariableResolver
    ) {
        this.executeHttpRequestUseCase = executeHttpRequestUseCase;
        this.responseVariableResolver = responseVariableResolver;
    }

    public List<StepExecutionResult> execute(HttpTestCase testCase) {
        var results = new ArrayList<StepExecutionResult>();
        for (int index = 0; index < testCase.steps().size(); index++) {
            var step = testCase.steps().get(index);
            var resolvedRequest = responseVariableResolver.resolve(step.request(), results);
            var response = executeHttpRequestUseCase.execute(resolvedRequest);
            results.add(new StepExecutionResult(index + 1, step.name(), resolvedRequest, response));
        }
        return List.copyOf(results);
    }
}
