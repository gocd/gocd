package com.thoughtworks.go.server.util;

import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.Request;

public class Jetty6Request implements ServletRequest {
    private Request request;

    public Jetty6Request(javax.servlet.ServletRequest request) {
        this.request = (Request) request;
    }

    @Override
    public String getUrl() {
        HttpURI uri = request.getUri();
        return request.getRootURL().append(uri.toString()).toString();
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
