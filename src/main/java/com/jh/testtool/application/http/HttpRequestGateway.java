package com.jh.testtool.application.http;

import com.jh.testtool.domain.http.HttpRequestCommand;
import com.jh.testtool.domain.http.HttpResponseResult;

public interface HttpRequestGateway {
    HttpResponseResult execute(HttpRequestCommand request);
}
