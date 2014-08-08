package com.thoughtworks.go.plugin.api.request;

import java.util.Map;

public abstract class GoPluginApiRequest {

    public abstract String extension();

    public abstract String extensionVersion();

    public abstract String requestName();

    public abstract Map requestParameters();

    public abstract Map<String, String> requestHeaders();

    public abstract String requestBody();
}
