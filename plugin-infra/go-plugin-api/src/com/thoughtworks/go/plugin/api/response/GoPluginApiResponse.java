package com.thoughtworks.go.plugin.api.response;

import java.util.Map;

public abstract class GoPluginApiResponse {

    public abstract int responseCode();

    public abstract Map<String, String> responseHeaders();

    public abstract String responseBody();
}

