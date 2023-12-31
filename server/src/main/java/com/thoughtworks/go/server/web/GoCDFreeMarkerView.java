/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class GoCDFreeMarkerView extends FreeMarkerView {
    public static final String PRINCIPAL = "principal";
    public static final String ADMINISTRATOR = "userHasAdministratorRights";
    public static final String TEMPLATE_ADMINISTRATOR = "userHasTemplateAdministratorRights";
    public static final String VIEW_ADMINISTRATOR_RIGHTS = "userHasViewAdministratorRights";
    public static final String TEMPLATE_VIEW_USER = "userHasTemplateViewUserRights";
    public static final String GROUP_ADMINISTRATOR = "userHasGroupAdministratorRights";
    public static final String USE_COMPRESS_JS = "useCompressJS";
    public static final String CURRENT_GOCD_VERSION = "currentGoCDVersion";
    public static final String CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH = "concatenatedStageBarCancelledIconFilePath";
    public static final String CONCATENATED_CRUISE_ICON_FILE_PATH = "concatenatedCruiseIconFilePath";
    public static final String PATH_RESOLVER = "pathResolver";
    public static final String GO_UPDATE = "goUpdate";
    public static final String GO_UPDATE_CHECK_ENABLED = "goUpdateCheckEnabled";
    public static final String SHOW_ANALYTICS_DASHBOARD = "showAnalyticsDashboard";
    public static final String WEBPACK_ASSETS_SERVICE = "webpackAssetsService";
    public static final String MAINTENANCE_MODE_SERVICE = "maintenanceModeService";
    public static final String IS_ANONYMOUS_USER = "isAnonymousUser";

    private final SystemEnvironment systemEnvironment;

    public GoCDFreeMarkerView() {
        this(new SystemEnvironment());
    }

    public GoCDFreeMarkerView(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public RailsAssetsService getRailsAssetsService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(RailsAssetsService.class);
    }

    public WebpackAssetsService webpackAssetsService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(WebpackAssetsService.class);
    }

    public VersionInfoService getVersionInfoService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(VersionInfoService.class);
    }

    public MaintenanceModeService getMaintenanceModeService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(MaintenanceModeService.class);
    }

    public SecurityService getSecurityService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(SecurityService.class);
    }

    public DefaultPluginInfoFinder getPluginInfoFinder() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(DefaultPluginInfoFinder.class);
    }


    @Override
    protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
        super.exposeHelpers(model, request);

        RailsAssetsService railsAssetsService = getRailsAssetsService();
        VersionInfoService versionInfoService = getVersionInfoService();
        SecurityService securityService = getSecurityService();
        Username username = SessionUtils.getCurrentUser().asUsernameObject();

        model.put(ADMINISTRATOR, securityService.isUserAdmin(username));
        model.put(GROUP_ADMINISTRATOR, securityService.isUserGroupAdmin(username));
        model.put(TEMPLATE_ADMINISTRATOR, securityService.isAuthorizedToViewAndEditTemplates(username));
        model.put(VIEW_ADMINISTRATOR_RIGHTS, securityService.canViewAdminPage(username));
        model.put(TEMPLATE_VIEW_USER, securityService.isAuthorizedToViewTemplates(username));
        model.put(USE_COMPRESS_JS, systemEnvironment.useCompressedJs());

        model.put(CURRENT_GOCD_VERSION, CurrentGoCDVersion.getInstance());
        model.put(CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH, railsAssetsService.getAssetPath("g9/stage_bar_cancelled_icon.png"));
        model.put(CONCATENATED_CRUISE_ICON_FILE_PATH, railsAssetsService.getAssetPath("cruise.ico"));

        model.put(PATH_RESOLVER, railsAssetsService);
        model.put(GO_UPDATE, versionInfoService.getGoUpdate());
        model.put(GO_UPDATE_CHECK_ENABLED, versionInfoService.isGOUpdateCheckEnabled());

        model.put(SHOW_ANALYTICS_DASHBOARD, (securityService.isUserAdmin(username) && supportsAnalyticsDashboard()));
        model.put(WEBPACK_ASSETS_SERVICE, webpackAssetsService());
        model.put(MAINTENANCE_MODE_SERVICE, getMaintenanceModeService());

        if (!SessionUtils.hasAuthenticationToken(request)) {
            return;
        }
        final AuthenticationToken<?> authentication = SessionUtils.getAuthenticationToken(request);

        setPrincipal(model, authentication);
        setAnonymousUser(model, authentication);

    }

    private void setAnonymousUser(Map<String, Object> model, AuthenticationToken<?> authentication) {
        model.put(IS_ANONYMOUS_USER, "anonymous".equalsIgnoreCase(authentication.getUser().getDisplayName()));
    }

    private boolean supportsAnalyticsDashboard() {
        for (CombinedPluginInfo combinedPluginInfo : getPluginInfoFinder().allPluginInfos(PluginConstants.ANALYTICS_EXTENSION)) {
            AnalyticsPluginInfo pluginInfo = (AnalyticsPluginInfo) combinedPluginInfo.extensionFor(PluginConstants.ANALYTICS_EXTENSION);
            if (pluginInfo.getCapabilities().supportsDashboardAnalytics()) {
                return true;
            }
        }
        return false;
    }

    private void setPrincipal(Map<String, Object> model, AuthenticationToken<?> authentication) {
        model.put(PRINCIPAL, authentication.getUser().getDisplayName());
    }

}
