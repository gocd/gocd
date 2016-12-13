/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.google.gson.Gson;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.plugin.access.authentication.JsonMessageHandler;
import com.thoughtworks.go.plugin.access.authentication.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.UserService;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.server.service.plugins.processor.authorization.AuthorizationRequestProcessor.Request.GET_PLUGIN_CONFIG_REQUEST;
import static com.thoughtworks.go.server.service.plugins.processor.authorization.AuthorizationRequestProcessor.Request.GET_ROLE_CONFIG_REQUEST;
import static java.util.Arrays.asList;

@Component
public class AuthorizationRequestProcessor implements GoPluginApiRequestProcessor {
    private static final List<String> goSupportedVersions = asList("1.0");

    private AuthorityGranter authorityGranter;
    private UserService userService;
    private final GoConfigService goConfigService;
    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public AuthorizationRequestProcessor(PluginRequestProcessorRegistry registry, AuthorityGranter authorityGranter, UserService userService, GoConfigService goConfigService) {
        this.authorityGranter = authorityGranter;
        this.userService = userService;
        this.goConfigService = goConfigService;
        this.messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
        registry.registerProcessorFor(GET_PLUGIN_CONFIG_REQUEST.requestName(), this);
        registry.registerProcessorFor(GET_ROLE_CONFIG_REQUEST.requestName(), this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {

        validatePluginRequest(request);
        switch (Request.fromString(request.api())) {
            case GET_PLUGIN_CONFIG_REQUEST:
                return processPluginConfigRequest(pluginDescriptor, request);
            case GET_ROLE_CONFIG_REQUEST:
                return processRoleConfigRequest(pluginDescriptor, request);
            default:
                return DefaultGoApiResponse.error("Illegal api request");
        }
    }

    private GoApiResponse processRoleConfigRequest(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        List<Map<String, Object>> rolesConfigList = getRoleConfigList(pluginDescriptor.id(), request.requestBody());
        DefaultGoApiResponse response = new DefaultGoApiResponse(200);
        response.setResponseBody(new Gson().toJson(rolesConfigList));
        return response;
    }

    private List<Map<String, Object>> getRoleConfigList(String pluginId, String profileId) {
        RolesConfig rolesConfig = goConfigService.serverConfig().security().getRoles();
        List<PluginRoleConfig> pluginRoleConfigs = rolesConfig.getPluginRolesConfig();

        List<Map<String, Object>> rolesConfigList = new ArrayList<>();

        for (PluginRoleConfig pluginRoleConfig : pluginRoleConfigs) {
            if (pluginId.equals(pluginRoleConfig.getPluginId()) && profileId.equals(pluginRoleConfig.getAuthConfigId())) {
                Map<String, Object> roleConfigMap = new HashedMap();
                roleConfigMap.put("profile_id", pluginRoleConfig.getAuthConfigId());
                roleConfigMap.put("name", pluginRoleConfig.getName().toString());
                roleConfigMap.put("configuration", pluginRoleConfig.getConfigurationAsMap(false));
                rolesConfigList.add(roleConfigMap);
            }
        }
        return rolesConfigList;
    }

    private void validatePluginRequest(GoApiRequest goPluginApiRequest) {
        if (!goSupportedVersions.contains(goPluginApiRequest.apiVersion())) {
            throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s", goPluginApiRequest.api(), goPluginApiRequest.apiVersion(), goSupportedVersions));
        }
    }

    private GoApiResponse processPluginConfigRequest(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        DefaultGoApiResponse response = new DefaultGoApiResponse(200);
        response.setResponseBody(new Gson().toJson(getAuthConfigProfiles(pluginDescriptor)));
        return response;
    }

    private Map<String, Map<String, String>> getAuthConfigProfiles(GoPluginDescriptor pluginDescriptor) {
        SecurityAuthConfigs securityAuthConfigs = goConfigService.serverConfig().security().securityAuthConfigs();
        Map<String, Map<String, String>> authConfigs = new HashMap<>();
        for (SecurityAuthConfig securityAuthConfig : securityAuthConfigs.findByPluginId(pluginDescriptor.id())) {
            authConfigs.put(securityAuthConfig.getId(), securityAuthConfig.getConfigurationAsMap(true));
        }
        return authConfigs;
    }

    enum Request {
        GET_PLUGIN_CONFIG_REQUEST("go.processor.authorization.get-profile-config"),
        GET_ROLE_CONFIG_REQUEST("go.processor.authorization.get-role-config");

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
