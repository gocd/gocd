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
package com.thoughtworks.go.spark.spa.spring;

import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.analytics.Capabilities;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.SparkController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InitialContextProviderTest {

    private InitialContextProvider initialContextProvider;
    private RailsAssetsService railsAssetsService;
    private WebpackAssetsService webpackAssetsService;
    private SecurityService securityService;
    private VersionInfoService versionInfoService;
    private DefaultPluginInfoFinder pluginInfoFinder;
    private FeatureToggleService featureToggleService;
    private MaintenanceModeService maintenanceModeService;
    private ServerConfigService serverConfigService;

    @BeforeEach
    void setup() {
        railsAssetsService = mock(RailsAssetsService.class);
        webpackAssetsService = mock(WebpackAssetsService.class);
        securityService = mock(SecurityService.class);
        versionInfoService = mock(VersionInfoService.class);
        pluginInfoFinder = mock(DefaultPluginInfoFinder.class);
        featureToggleService = mock(FeatureToggleService.class);
        maintenanceModeService = mock(MaintenanceModeService.class);
        serverConfigService = mock(ServerConfigService.class);
        Toggles.initializeWith(featureToggleService);
        initialContextProvider = new InitialContextProvider(railsAssetsService, webpackAssetsService, securityService,
                versionInfoService, pluginInfoFinder, maintenanceModeService, serverConfigService);
        SessionUtils.setCurrentUser(new GoUserPrinciple("bob", "Bob"));
    }

    @Test
    void shouldShowAnalyticsDashboard() {
        Map<String, Object> modelMap = new HashMap<>();
        when(securityService.isUserAdmin(any(Username.class))).thenReturn(true);
        CombinedPluginInfo combinedPluginInfo = new CombinedPluginInfo(analyticsPluginInfo());
        when(pluginInfoFinder.allPluginInfos(PluginConstants.ANALYTICS_EXTENSION)).thenReturn(Collections.singletonList(combinedPluginInfo));
        Map<String, Object> contect = initialContextProvider.getContext(modelMap, dummySparkController.getClass(), "viewName");
        assertThat(contect.get("showAnalyticsDashboard")).isEqualTo(true);
    }

    @Test
    void shouldNotShowAnalyticsDashboardWhenUserIsNotAdmin() {
        Map<String, Object> modelMap = new HashMap<>();
        when(securityService.isUserAdmin(any(Username.class))).thenReturn(false);
        CombinedPluginInfo combinedPluginInfo = new CombinedPluginInfo(analyticsPluginInfo());
        when(pluginInfoFinder.allPluginInfos(PluginConstants.ANALYTICS_EXTENSION)).thenReturn(Collections.singletonList(combinedPluginInfo));
        Map<String, Object> contect = initialContextProvider.getContext(modelMap, dummySparkController.getClass(), "viewName");
        assertThat(contect.get("showAnalyticsDashboard")).isEqualTo(false);
    }

    @Test
    void shouldNotShowAnalyticsDashboardPluginIsNotPresent() {
        Map<String, Object> modelMap = new HashMap<>();
        when(securityService.isUserAdmin(any(Username.class))).thenReturn(true);
        when(pluginInfoFinder.allPluginInfos(PluginConstants.ANALYTICS_EXTENSION)).thenReturn(Collections.singletonList(new CombinedPluginInfo()));
        Map<String, Object> contect = initialContextProvider.getContext(modelMap, dummySparkController.getClass(), "viewName");
        assertThat(contect.get("showAnalyticsDashboard")).isEqualTo(false);
    }

    private AnalyticsPluginInfo analyticsPluginInfo() {
        AnalyticsPluginInfo analyticsPluginInfo = mock(AnalyticsPluginInfo.class);
        Capabilities capabilities = mock(Capabilities.class);
        when(capabilities.supportsDashboardAnalytics()).thenReturn(true);
        when(analyticsPluginInfo.getCapabilities()).thenReturn(capabilities);
        when(analyticsPluginInfo.getExtensionName()).thenReturn(PluginConstants.ANALYTICS_EXTENSION);
        return analyticsPluginInfo;
    }

    private SparkController dummySparkController = new SparkController() {
        @Override
        public String controllerBasePath() {
            return null;
        }

        @Override
        public void setupRoutes() {

        }
    };
}
