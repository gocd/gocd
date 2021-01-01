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
package com.thoughtworks.go.spark.spa;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.UrlEscapers;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.GoConfigService;
import spark.Request;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
                .collect(Collectors.toList());


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
                .collect(Collectors.toList());

        ImmutableMap.Builder<String, Object> metaBuilder = ImmutableMap.<String, Object>builder()
                .put("hasWebBasedPlugins", !webBasedAuthenticationPlugins.isEmpty())
                .put("hasPasswordPlugins", !passwordBasedAuthenticationPlugins.isEmpty())
                .put("webBasedPlugins", webBasedAuthenticationPlugins.stream().map(authorizationPluginInfo -> ImmutableMap.builder()
                        .put("pluginName", authorizationPluginInfo.getDescriptor().about().name())
                        .put("imageUrl", authorizationPluginInfo.getImage().toDataURI())
                        .put("redirectUrl", "/go/plugin/" + UrlEscapers.urlPathSegmentEscaper().escape(authorizationPluginInfo.getDescriptor().id()) + "/login")
                        .build()).collect(Collectors.toList()));


        if (isNotBlank(SessionUtils.getAuthenticationError(request.raw()))) {
            metaBuilder.put("loginError", SessionUtils.getAuthenticationError(request.raw()));
        }

        return metaBuilder.build();
    }


}
