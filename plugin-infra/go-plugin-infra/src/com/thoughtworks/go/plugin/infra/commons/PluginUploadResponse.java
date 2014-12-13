package com.thoughtworks.go.plugin.infra.commons;

import java.util.HashMap;
import java.util.Map;

public class PluginUploadResponse {

    private Map<Integer, String> errors;
    private String successMessage;

    public PluginUploadResponse(String successMessage, Map<Integer, String> errors) {
        this.errors = errors;
        this.successMessage = successMessage;
    }

    public static PluginUploadResponse create(boolean isSuccess, String successMessage, Map<Integer, String> errors) {
        if (isSuccess) return new PluginUploadResponse(successMessage, new HashMap<Integer, String>());
        return new PluginUploadResponse("", errors);
    }

    public String success() {
        return successMessage;
    }

    public Map<Integer, String> errors() {
        return errors;
    }

}
