package com.thoughtworks.go.plugin.api.request;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;

import java.util.HashMap;
import java.util.Map;

public class DefaultGoApiRequest extends GoApiRequest {

    private String api;

    private String apiVersion;

    private GoPluginIdentifier pluginIdentifier;

    private Map<String, String> requestParameters = new HashMap<String, String>();

    private Map<String, String> requestHeaders = new HashMap<String, String>();

    private String requestBody;

    public DefaultGoApiRequest(String api, String apiVersion, GoPluginIdentifier pluginIdentifier) {
        this.api = api;
        this.apiVersion = apiVersion;
        this.pluginIdentifier = pluginIdentifier;
    }


    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public void addRequestParameter(String name, String value) {
        requestParameters.put(name, value);
    }

    public void addRequestHeader(String name, String value) {
        requestHeaders.put(name, value);
    }


    @Override
    public String api() {
        return api;
    }

    @Override
    public String apiVersion() {
        return apiVersion;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return pluginIdentifier;
    }

    @Override
    public Map<String, String> requestParameters() {
        return requestParameters;
    }

    @Override
    public Map<String, String> requestHeaders() {
        return requestHeaders;
    }

    @Override
    public String requestBody() {
        return requestBody;
    }
}
