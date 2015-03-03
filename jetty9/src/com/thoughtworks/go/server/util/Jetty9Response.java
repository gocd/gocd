package com.thoughtworks.go.server.util;

import org.eclipse.jetty.server.Response;

public class Jetty9Response implements ServletResponse {
    private javax.servlet.ServletResponse servletResponse;

    public Jetty9Response(javax.servlet.ServletResponse servletResponse) {

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
