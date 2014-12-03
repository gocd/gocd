package com.thoughtworks.go.plugin.access;

import java.util.Map;

public interface PluginInteractionCallback<T> {
    String requestBody(String resolvedExtensionVersion);
    Map<String, Object> requestParams(String resolvedExtensionVersion);

    T onSuccess(String responseBody, String resolvedExtensionVersion);
}
