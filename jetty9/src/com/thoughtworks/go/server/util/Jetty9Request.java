package com.thoughtworks.go.server.util;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;

public class Jetty9Request implements ServletRequest {
    private Request request;

    public Jetty9Request(javax.servlet.ServletRequest request) {
        this.request = (Request) request;
    }

    @Override
    public String getUrl() {
        return request.getRootURL().append(getUriAsString()).toString();
    }

    @Override
    public String getUriPath() {
        return request.getUri().getPath();
    }

    @Override
    public String getUriAsString() {
        return request.getUri().toString();
    }
}
