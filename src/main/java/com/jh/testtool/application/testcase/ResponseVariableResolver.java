package com.jh.testtool.application.testcase;

import com.jh.testtool.domain.http.HttpRequestCommand;
import com.jh.testtool.domain.http.HttpResponseResult;
import com.jh.testtool.domain.testcase.StepExecutionResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ResponseVariableResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");

    public HttpRequestCommand resolve(HttpRequestCommand request, List<StepExecutionResult> previousResults) {
        return new HttpRequestCommand(
                request.method(),
                resolveText(request.url(), previousResults),
                resolveHeaders(request.headers(), previousResults),
                resolveText(request.body(), previousResults)
        );
    }

    private Map<String, List<String>> resolveHeaders(
            Map<String, List<String>> headers,
            List<StepExecutionResult> previousResults
    ) {
        var resolved = new LinkedHashMap<String, List<String>>();
        headers.forEach((name, values) -> resolved.put(
                resolveText(name, previousResults),
                values.stream().map(value -> resolveText(value, previousResults)).toList()
        ));
        return resolved;
    }

    public String resolveText(String text, List<StepExecutionResult> previousResults) {
        if (text == null || text.isBlank() || previousResults.isEmpty()) {
            return text == null ? "" : text;
        }

        var matcher = VARIABLE_PATTERN.matcher(text);
        var resolved = new StringBuilder();
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(resolveVariable(variable, previousResults)));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private String resolveVariable(String variable, List<StepExecutionResult> previousResults) {
        var response = selectResponse(variable, previousResults);
        var key = stripSelector(variable);

        if ("status".equals(key)) {
            return String.valueOf(response.statusCode());
        }
        if ("body".equals(key)) {
            return response.body();
        }
        if (key.startsWith("header.")) {
            var headerName = key.substring("header.".length());
            return response.headers().entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
                    .findFirst()
                    .flatMap(entry -> entry.getValue().stream().findFirst())
                    .orElse("");
        }
        if (key.startsWith("json.")) {
            return SimpleJsonPathExtractor.extract(response.body(), key.substring("json.".length()));
        }
        return "";
    }

    private HttpResponseResult selectResponse(String variable, List<StepExecutionResult> previousResults) {
        if (variable.startsWith("step.")) {
            var parts = variable.split("\\.", 3);
            if (parts.length >= 3) {
                int stepNumber = Integer.parseInt(parts[1]);
                return previousResults.stream()
                        .filter(result -> result.stepNumber() == stepNumber)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown response variable: " + variable))
                        .response();
            }
        }
        return previousResults.get(previousResults.size() - 1).response();
    }

    private String stripSelector(String variable) {
        if (variable.startsWith("last.")) {
            return variable.substring("last.".length());
        }
        if (variable.startsWith("step.")) {
            String[] parts = variable.split("\\.", 3);
            return parts.length == 3 ? parts[2] : "";
        }
        return variable;
    }
}
