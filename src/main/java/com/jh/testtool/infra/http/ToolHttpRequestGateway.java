package com.jh.testtool.infra.http;

import com.jh.testtool.application.http.HttpRequestGateway;
import com.jh.testtool.domain.http.HttpRequestCommand;
import com.jh.testtool.domain.http.HttpResponseResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class ToolHttpRequestGateway implements HttpRequestGateway {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public HttpResponseResult execute(HttpRequestCommand request) {
        try {
            var builder = HttpRequest.newBuilder()
                    .uri(URI.create(request.url()))
                    .timeout(Duration.ofSeconds(30));

            request.headers().forEach((name, values) ->
                    values.forEach(value -> builder.header(name, value))
            );

            var bodyPublisher = request.body().isBlank()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(request.body());

            var response = httpClient.send(
                    builder.method(request.method(), bodyPublisher).build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            return new HttpResponseResult(response.statusCode(), response.headers().map(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException("HTTP request failed.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request was interrupted.", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid HTTP request: " + e.getMessage(), e);
        }
    }
}
