/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.plugins.processor.authorization;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.PluginRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.thoughtworks.go.server.service.plugins.processor.authorization.AuthorizationRequestProcessor.Request.INVALIDATE_CACHE_REQUEST;
import static java.util.Arrays.asList;

@Component
public class AuthorizationRequestProcessor implements GoPluginApiRequestProcessor {
    private static final List<String> goSupportedVersions = asList("1.0");

    private final PluginRoleService pluginRoleService;

    @Autowired
    public AuthorizationRequestProcessor(PluginRequestProcessorRegistry registry, PluginRoleService pluginRoleService) {
        this.pluginRoleService = pluginRoleService;
        registry.registerProcessorFor(INVALIDATE_CACHE_REQUEST.requestName(), this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {

        validatePluginRequest(request);
        switch (Request.fromString(request.api())) {
            case INVALIDATE_CACHE_REQUEST:
                return processInvalidateCacheRequest(pluginDescriptor);
            default:
                return DefaultGoApiResponse.error("Illegal api request");
        }
    }

    private GoApiResponse processInvalidateCacheRequest(GoPluginDescriptor pluginDescriptor) {
        pluginRoleService.invalidateRolesFor(pluginDescriptor.id());
        return DefaultGoApiResponse.success(null);
    }

    private void validatePluginRequest(GoApiRequest goPluginApiRequest) {
        if (!goSupportedVersions.contains(goPluginApiRequest.apiVersion())) {
            throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s", goPluginApiRequest.api(), goPluginApiRequest.apiVersion(), goSupportedVersions));
        }
    }

    enum Request {
        INVALIDATE_CACHE_REQUEST("go.processor.authorization.invalidate-cache");

        private final String requestName;

        Request(String requestName) {
            this.requestName = requestName;
        }

        public static Request fromString(String requestName) {
            if (requestName != null) {
                for (Request request : Request.values()) {
                    if (requestName.equalsIgnoreCase(request.requestName)) {
                        return request;
                    }
                }
            }

            return null;
        }

        public String requestName() {
            return requestName;
        }
    }
}
