package com.thoughtworks.go.plugin.api.response;


import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class DefaultGoApiResponse extends GoApiResponse {

    public static final int SUCCESS_RESPONSE_CODE = 200;

    private int responseCode;
    private Map<String, String> responseHeaders = new HashMap<String, String>();
    private String responseBody;

    public DefaultGoApiResponse(int responseCode) {
        this.responseCode = responseCode;
    }

    public static DefaultGoApiResponse incompleteRequest(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(412);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
    }

    public static DefaultGoApiResponse badRequest(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(400);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
    }

    public static DefaultGoApiResponse error(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(500);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
    }

    public static DefaultGoApiResponse success(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(SUCCESS_RESPONSE_CODE);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
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
        return unmodifiableMap(responseHeaders);
    }

    @Override
    public String responseBody() {
        return responseBody;
    }
}
