package com.thoughtworks.go.plugin.access;

import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;

import java.util.List;

import static java.lang.String.format;

public class PluginRequestHelper {
    protected PluginManager pluginManager;
    private List<String> goSupportedVersions;
    private String extensionName;

    public PluginRequestHelper(PluginManager pluginManager, List<String> goSupportedVersions, String extensionName) {
        this.pluginManager = pluginManager;
        this.goSupportedVersions = goSupportedVersions;
        this.extensionName = extensionName;
    }

    public <T> T submitRequest(String pluginId, String requestName, PluginInteractionCallback<T> pluginInteractionCallback) {
        try {
            String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, goSupportedVersions);
            DefaultGoPluginApiRequest apiRequest = new DefaultGoPluginApiRequest(extensionName, resolvedExtensionVersion, requestName);
            apiRequest.setRequestBody(pluginInteractionCallback.requestBody(resolvedExtensionVersion));
            apiRequest.setRequestParams(pluginInteractionCallback.requestParams(resolvedExtensionVersion));
            GoPluginApiResponse response = pluginManager.submitTo(pluginId, apiRequest);
            if (DefaultGoApiResponse.SUCCESS_RESPONSE_CODE == response.responseCode()) {
                return pluginInteractionCallback.onSuccess(response.responseBody(), resolvedExtensionVersion);
            }
            throw new RuntimeException(format("Unsuccessful response code from plugin %s with body %s", response.responseCode(), response.responseBody()));
        } catch (Exception e) {
            throw new RuntimeException(format("Exception while interacting with plugin id %s, extension %s, request %s", pluginId, extensionName, requestName), e);
        }
    }
}
