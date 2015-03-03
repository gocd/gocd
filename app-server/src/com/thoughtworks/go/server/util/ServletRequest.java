package com.thoughtworks.go.server.util;

public interface ServletRequest {
    String getUrl();
    String getUriPath();
    String getUriAsString();
}
