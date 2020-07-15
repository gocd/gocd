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
package com.thoughtworks.go.spark.spa.spring;

import com.google.common.base.CaseFormat;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Component
public class InitialContextProvider {

    private final RailsAssetsService railsAssetsService;
    private final WebpackAssetsService webpackAssetsService;
    private final SecurityService securityService;
    private final VersionInfoService versionInfoService;
    private final DefaultPluginInfoFinder pluginInfoFinder;
    private MaintenanceModeService maintenanceModeService;

    @Autowired
    public InitialContextProvider(RailsAssetsService railsAssetsService, WebpackAssetsService webpackAssetsService,
                                  SecurityService securityService, VersionInfoService versionInfoService, DefaultPluginInfoFinder pluginInfoFinder,
                                  MaintenanceModeService maintenanceModeService) {
        this.railsAssetsService = railsAssetsService;
        this.webpackAssetsService = webpackAssetsService;
        this.securityService = securityService;
        this.versionInfoService = versionInfoService;
        this.pluginInfoFinder = pluginInfoFinder;
        this.maintenanceModeService = maintenanceModeService;
    }

    public Map<String, Object> getContext(Map<String, Object> modelMap, Class<? extends SparkController> controller, String viewName) {
        HashMap<String, Object> context = new HashMap<>(modelMap);
        context.put("currentGoCDVersion", CurrentGoCDVersion.getInstance().getGocdDistVersion());
        context.put("railsAssetsService", railsAssetsService);
        context.put("webpackAssetsService", webpackAssetsService);
        context.put("securityService", securityService);
        context.put("maintenanceModeService", maintenanceModeService);
        context.put("currentUser", SessionUtils.currentUsername());
        context.put("controllerName", humanizedControllerName(controller));
        context.put("viewName", viewName);
        context.put("currentVersion", CurrentGoCDVersion.getInstance());
        context.put("toggles", Toggles.class);
        context.put("goUpdate", versionInfoService.getGoUpdate());
        context.put("goUpdateCheckEnabled", versionInfoService.isGOUpdateCheckEnabled());
        context.put("serverTimezoneUTCOffset", TimeZone.getDefault().getOffset(new Date().getTime()));
        context.put("spaRefreshInterval", SystemEnvironment.goSpaRefreshInterval());
        context.put("spaTimeout", SystemEnvironment.goSpaTimeout());
        context.put("showAnalyticsDashboard", showAnalyticsDashboard());
        context.put("devMode", !new SystemEnvironment().useCompressedJs());
        context.put(Toggles.SHOW_MATERIALS_SPA,Toggles.isToggleOn(Toggles.SHOW_MATERIALS_SPA));
        return context;
    }

    private String humanizedControllerName(Class<? extends SparkController> controller) {
        return CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(controller.getSimpleName().replaceAll("(Delegate|Controller)", ""));
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
