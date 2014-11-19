package com.thoughtworks.go.plugin.api.response;

import java.util.HashMap;
import java.util.Map;

public class DefaultGoPluginApiResponse extends GoPluginApiResponse {

    public static final int SUCCESS_RESPONSE_CODE = 200;

    private int responseCode;

    private Map<String, String> responseHeaders = new HashMap<String, String>();

    private String responseBody;

    public DefaultGoPluginApiResponse(int responseCode) {
        this.responseCode = responseCode;
    }

    public static DefaultGoPluginApiResponse incompleteRequest(String responseBody) {
        DefaultGoPluginApiResponse defaultGoPluginApiResponse = new DefaultGoPluginApiResponse(412);
        defaultGoPluginApiResponse.setResponseBody(responseBody);
        return defaultGoPluginApiResponse;
    }

    public static DefaultGoPluginApiResponse badRequest(String responseBody) {
        DefaultGoPluginApiResponse defaultGoPluginApiResponse = new DefaultGoPluginApiResponse(400);
        defaultGoPluginApiResponse.setResponseBody(responseBody);
        return defaultGoPluginApiResponse;
    }

    public static DefaultGoPluginApiResponse error(String responseBody) {
        DefaultGoPluginApiResponse defaultGoPluginApiResponse = new DefaultGoPluginApiResponse(500);
        defaultGoPluginApiResponse.setResponseBody(responseBody);
        return defaultGoPluginApiResponse;
    }

    public static DefaultGoPluginApiResponse success(String responseBody) {
        DefaultGoPluginApiResponse defaultGoPluginApiResponse = new DefaultGoPluginApiResponse(200);
        defaultGoPluginApiResponse.setResponseBody(responseBody);
        return defaultGoPluginApiResponse;
    }


    public void addResponseHeader(String name, String value) {
        responseHeaders.put(name, value);
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    @Override
    public int responseCode() {
        return responseCode;
    }

    @Override
    public Map<String, String> responseHeaders() {
        return responseHeaders;
    }

    @Override
    public String responseBody() {
        return responseBody;
    }
}
