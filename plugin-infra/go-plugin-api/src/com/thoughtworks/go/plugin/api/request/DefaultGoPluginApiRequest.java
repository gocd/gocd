package com.thoughtworks.go.plugin.api.request;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class DefaultGoPluginApiRequest extends GoPluginApiRequest {

    private String extension;

    private String extensionVersion;

    private String requestName;

    private Map<String, String> requestParameters = new HashMap<String, String>();

    private Map<String, String> requestHeaders = new HashMap<String, String>();

    private String requestBody;

    public DefaultGoPluginApiRequest(String extension, String extensionVersion, String requestName) {
        this.extension = extension;
        this.extensionVersion = extensionVersion;
        this.requestName = requestName;
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
    public String extension() {
        return extension;
    }

    @Override
    public String extensionVersion() {
        return extensionVersion;
    }

    @Override
    public String requestName() {
        return requestName;
    }

    @Override
    public Map<String, String> requestParameters() {
        return unmodifiableMap(requestParameters);
    }

    @Override
    public Map<String, String> requestHeaders() {
        return unmodifiableMap(requestHeaders);
    }

    @Override
    public String requestBody() {
        return requestBody;
    }
}
