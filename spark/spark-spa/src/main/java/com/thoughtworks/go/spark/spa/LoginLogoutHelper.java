/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.web.util.UriUtils;
import spark.Request;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public class LoginLogoutHelper {
    private final GoConfigService goConfigService;
    private final AuthorizationMetadataStore authorizationMetadataStore;

    public LoginLogoutHelper(GoConfigService goConfigService, AuthorizationMetadataStore authorizationMetadataStore) {
        this.goConfigService = goConfigService;
        this.authorizationMetadataStore = authorizationMetadataStore;
    }

    public Map<String, Object> buildMeta(Request request) {
        SecurityAuthConfigs securityAuthConfigs = goConfigService.security().securityAuthConfigs();

        List<AuthorizationPluginInfo> webBasedAuthenticationPlugins = securityAuthConfigs
                .stream()
                .map(configurationProperties -> {
                    if (authorizationMetadataStore.doesPluginSupportWebBasedAuthentication(configurationProperties.getPluginId())) {
                        return authorizationMetadataStore.getPluginInfo(configurationProperties.getPluginId());
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        List<AuthorizationPluginInfo> passwordBasedAuthenticationPlugins = securityAuthConfigs
                .stream()
                .map(configurationProperties -> {
                    if (authorizationMetadataStore.doesPluginSupportPasswordBasedAuthentication(configurationProperties.getPluginId())) {
                        return authorizationMetadataStore.getPluginInfo(configurationProperties.getPluginId());
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, Object> metaBuilder = new HashMap<>(4);
        metaBuilder.put("hasWebBasedPlugins", !webBasedAuthenticationPlugins.isEmpty());
        metaBuilder.put("hasPasswordPlugins", !passwordBasedAuthenticationPlugins.isEmpty());
        metaBuilder.put("webBasedPlugins", webBasedAuthenticationPlugins.stream().map(authorizationPluginInfo -> Map.of(
                "pluginName", authorizationPluginInfo.getDescriptor().about().name(),
                "imageUrl", authorizationPluginInfo.getImage().toDataURI(),
                "redirectUrl", "/go/plugin/" + getEncodePathSegment(authorizationPluginInfo.getDescriptor().id()) + "/login"
            )).collect(Collectors.toList())
        );

        String authenticationError = SessionUtils.getAuthenticationError(request.raw());
        if (authenticationError != null && !authenticationError.isBlank()) {
            metaBuilder.put("loginError", authenticationError);
        }

        return Collections.unmodifiableMap(metaBuilder);
    }

    private static String getEncodePathSegment(String value) {
        try {
            return UriUtils.encodePathSegment(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


}
