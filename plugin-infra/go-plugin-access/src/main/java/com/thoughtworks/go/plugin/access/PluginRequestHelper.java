/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.access;

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
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
        if (!pluginManager.isPluginOfType(extensionName, pluginId)) {
            throw new RecordNotFoundException(format("Did not find '%s' plugin with id '%s'. Looks like plugin is missing", extensionName, pluginId));
        }
        try {
            String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, extensionName, goSupportedVersions);
            DefaultGoPluginApiRequest apiRequest = new DefaultGoPluginApiRequest(extensionName, resolvedExtensionVersion, requestName);
            apiRequest.setRequestBody(pluginInteractionCallback.requestBody(resolvedExtensionVersion));
            apiRequest.setRequestParams(pluginInteractionCallback.requestParams(resolvedExtensionVersion));
            apiRequest.setRequestHeaders(pluginInteractionCallback.requestHeaders(resolvedExtensionVersion));
            GoPluginApiResponse response = pluginManager.submitTo(pluginId, extensionName, apiRequest);
            if (response == null) {
                throw new RuntimeException("The plugin sent a null response");
            }
            if (DefaultGoApiResponse.SUCCESS_RESPONSE_CODE == response.responseCode()) {
                return pluginInteractionCallback.onSuccess(response.responseBody(), response.responseHeaders(), resolvedExtensionVersion);
            }
            pluginInteractionCallback.onFailure(response.responseCode(), response.responseBody(), resolvedExtensionVersion);

            throw new RuntimeException(format("The plugin sent a response that could not be understood by Go. Plugin returned with code '%s' and the following response: '%s'", response.responseCode(), response.responseBody()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(format("Interaction with plugin with id '%s' implementing '%s' extension failed while requesting for '%s'. Reason: [%s]", pluginId, extensionName, requestName, e.getMessage()), e);
        }
    }

}
