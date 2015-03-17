package com.thoughtworks.go.server.util;

import org.mortbay.jetty.Response;

public class Jetty6Response implements ServletResponse {
    private javax.servlet.ServletResponse servletResponse;

    public Jetty6Response(javax.servlet.ServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    @Override
    public int getStatus() {
        return ((Response) servletResponse).getStatus();
    }

    @Override
    public long getContentCount() {
        return ((Response) servletResponse).getContentCount();
    }
}
