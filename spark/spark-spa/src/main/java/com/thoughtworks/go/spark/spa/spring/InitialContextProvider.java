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
package com.thoughtworks.go.spark.spa.spring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.SiteUrls;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

@Component
public class InitialContextProvider {

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(SiteUrls.class, (JsonSerializer<SiteUrls>) (src, t, c) -> {
            final JsonObject json = new JsonObject();
            String url = src.getSiteUrl().getUrl();
            if (url != null && !url.isBlank()) {
                json.addProperty("site_url", src.getSiteUrl().getUrl());
            }
            String secureUrl = src.getSecureSiteUrl().getUrl();
            if (secureUrl != null && !secureUrl.isBlank()) {
                json.addProperty("secure_site_url", secureUrl);
            }
            return json;
        })
        .create();
    private static final Pattern CONTROLLER_NAME_PATTERN = Pattern.compile("(Delegate|Controller)");

    private final RailsAssetsService railsAssetsService;
    private final WebpackAssetsService webpackAssetsService;
    private final SecurityService securityService;
    private final DefaultPluginInfoFinder pluginInfoFinder;
    private final MaintenanceModeService maintenanceModeService;
    private final ServerConfigService serverConfigService;

    @Autowired
    public InitialContextProvider(RailsAssetsService railsAssetsService, WebpackAssetsService webpackAssetsService,
                                  SecurityService securityService, DefaultPluginInfoFinder pluginInfoFinder,
                                  MaintenanceModeService maintenanceModeService, ServerConfigService serverConfigService) {
        this.railsAssetsService = railsAssetsService;
        this.webpackAssetsService = webpackAssetsService;
        this.securityService = securityService;
        this.pluginInfoFinder = pluginInfoFinder;
        this.maintenanceModeService = maintenanceModeService;
        this.serverConfigService = serverConfigService;
    }

    public Map<String, Object> getContext(Map<String, Object> modelMap, Class<? extends SparkController> controller, String viewName) {
        Map<String, Object> context = new HashMap<>(modelMap);
        context.put("railsAssetsService", railsAssetsService);
        context.put("webpackAssetsService", webpackAssetsService);
        context.put("securityService", securityService);
        context.put("maintenanceModeService", maintenanceModeService);
        context.put("currentUser", SessionUtils.currentUsername());
        context.put("currentVersion", CurrentGoCDVersion.getInstance());
        context.put("controllerName", humanizedControllerName(controller));
        context.put("viewName", viewName);
        context.put("serverTimezoneUTCOffset", TimeZone.getDefault().getOffset(new Date().getTime()));
        context.put("spaRefreshInterval", SystemEnvironment.goSpaRefreshInterval());
        context.put("spaTimeout", SystemEnvironment.goSpaTimeout());
        context.put("showAnalyticsDashboard", showAnalyticsDashboard());
        context.put("devMode", !new SystemEnvironment().useCompressedJs());
        context.put("serverSiteUrls", GSON.toJson(serverConfigService.getServerSiteUrls()));
        return context;
    }

    private String humanizedControllerName(Class<? extends SparkController> controller) {
        return camelCaseToSnakeCase(CONTROLLER_NAME_PATTERN.matcher(controller.getSimpleName()).replaceAll(""));
    }

    static String camelCaseToSnakeCase(String s) {
        return Arrays.stream(splitByCharacterTypeCamelCase(s)).map(String::toLowerCase).collect(Collectors.joining("_"));
    }

    private boolean showAnalyticsDashboard() {
        return securityService.isUserAdmin(SessionUtils.currentUsername()) && supportsAnalyticsDashboard();
    }

    private boolean supportsAnalyticsDashboard() {
        for (CombinedPluginInfo combinedPluginInfo : pluginInfoFinder.allPluginInfos(PluginConstants.ANALYTICS_EXTENSION)) {
            AnalyticsPluginInfo pluginInfo = (AnalyticsPluginInfo) combinedPluginInfo.extensionFor(PluginConstants.ANALYTICS_EXTENSION);
            if (pluginInfo != null && pluginInfo.getCapabilities().supportsDashboardAnalytics()) {
                return true;
            }
        }
        return false;
    }
}
