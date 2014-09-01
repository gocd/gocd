package com.thoughtworks.go.plugin.api.request;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;

import java.util.HashMap;
import java.util.Map;

public abstract class GoApiRequest {

    public abstract String api();

    public abstract String apiVersion();

    public abstract GoPluginIdentifier pluginIdentifier();

    public abstract Map<String, String> requestParameters();

    public abstract Map<String, String> requestHeaders();

    public abstract String requestBody();
}

