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
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.springframework.web.servlet.view.velocity.VelocityToolboxView;

import javax.servlet.http.HttpServletRequest;
import java.io.StringWriter;

public class GoVelocityView extends VelocityToolboxView {
    public static final String PRINCIPAL = "principal";
    public static final String ADMINISTRATOR = "userHasAdministratorRights";
    public static final String TEMPLATE_ADMINISTRATOR = "userHasTemplateAdministratorRights";
    public static final String VIEW_ADMINISTRATOR_RIGHTS = "userHasViewAdministratorRights";
    public static final String TEMPLATE_VIEW_USER = "userHasTemplateViewUserRights";
    public static final String GROUP_ADMINISTRATOR = "userHasGroupAdministratorRights";
    public static final String USE_COMPRESS_JS = "useCompressJS";
    public static final String CURRENT_GOCD_VERSION = "currentGoCDVersion";
    public static final String CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH = "concatenatedStageBarCancelledIconFilePath";
    public static final String CONCATENATED_SPINNER_ICON_FILE_PATH = "concatenatedSpinnerIconFilePath";
    public static final String CONCATENATED_CRUISE_ICON_FILE_PATH = "concatenatedCruiseIconFilePath";
    public static final String PATH_RESOLVER = "pathResolver";
    public static final String GO_UPDATE = "goUpdate";
    public static final String GO_UPDATE_CHECK_ENABLED = "goUpdateCheckEnabled";
    public static final String SHOW_ANALYTICS_DASHBOARD = "showAnalyticsDashboard";
    public static final String WEBPACK_ASSETS_SERVICE = "webpackAssetsService";
    public static final String MAINTENANCE_MODE_SERVICE = "maintenanceModeService";
    public static final String IS_ANONYMOUS_USER = "isAnonymousUser";

    private final SystemEnvironment systemEnvironment;

    public GoVelocityView() {
        this(new SystemEnvironment());
    }

    public GoVelocityView(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    RailsAssetsService getRailsAssetsService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(RailsAssetsService.class);
    }

    WebpackAssetsService webpackAssetsService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(WebpackAssetsService.class);
    }

    VersionInfoService getVersionInfoService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(VersionInfoService.class);
    }

    MaintenanceModeService getMaintenanceModeService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(MaintenanceModeService.class);
    }

    SecurityService getSecurityService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(SecurityService.class);
    }

    DefaultPluginInfoFinder getPluginInfoFinder() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(DefaultPluginInfoFinder.class);
    }

    @Override
    protected void exposeHelpers(Context velocityContext, HttpServletRequest request) throws Exception {
        RailsAssetsService railsAssetsService = getRailsAssetsService();
        VersionInfoService versionInfoService = getVersionInfoService();
        SecurityService securityService = getSecurityService();
        Username username = SessionUtils.getCurrentUser().asUsernameObject();

        velocityContext.put(ADMINISTRATOR, securityService.isUserAdmin(username));
        velocityContext.put(GROUP_ADMINISTRATOR, securityService.isUserGroupAdmin(username));
        velocityContext.put(TEMPLATE_ADMINISTRATOR, securityService.isAuthorizedToViewAndEditTemplates(username));
        velocityContext.put(VIEW_ADMINISTRATOR_RIGHTS, securityService.canViewAdminPage(username));
        velocityContext.put(TEMPLATE_VIEW_USER, securityService.isAuthorizedToViewTemplates(username));
        velocityContext.put(USE_COMPRESS_JS, systemEnvironment.useCompressedJs());

        velocityContext.put(CURRENT_GOCD_VERSION, CurrentGoCDVersion.getInstance());
        velocityContext.put(CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH, railsAssetsService.getAssetPath("g9/stage_bar_cancelled_icon.png"));
        velocityContext.put(CONCATENATED_SPINNER_ICON_FILE_PATH, railsAssetsService.getAssetPath("spinner.gif"));
        velocityContext.put(CONCATENATED_CRUISE_ICON_FILE_PATH, railsAssetsService.getAssetPath("cruise.ico"));

        velocityContext.put(PATH_RESOLVER, railsAssetsService);
        velocityContext.put(GO_UPDATE, versionInfoService.getGoUpdate());
        velocityContext.put(GO_UPDATE_CHECK_ENABLED, versionInfoService.isGOUpdateCheckEnabled());

        velocityContext.put(SHOW_ANALYTICS_DASHBOARD, (securityService.isUserAdmin(username) && supportsAnalyticsDashboard()));
        velocityContext.put(WEBPACK_ASSETS_SERVICE, webpackAssetsService());
        velocityContext.put(MAINTENANCE_MODE_SERVICE, getMaintenanceModeService());

        if (!SessionUtils.hasAuthenticationToken(request)) {
            return;
        }
        final AuthenticationToken<?> authentication = SessionUtils.getAuthenticationToken(request);

        setPrincipal(velocityContext, authentication);
        setAnonymousUser(velocityContext, authentication);
    }

    private void setAnonymousUser(Context velocityContext, AuthenticationToken<?> authentication) {
        velocityContext.put(IS_ANONYMOUS_USER, "anonymous".equalsIgnoreCase(authentication.getUser().getDisplayName()));
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

    private void setPrincipal(Context velocityContext, AuthenticationToken<?> authentication) {
        velocityContext.put(PRINCIPAL, authentication.getUser().getDisplayName());
    }

    public String getContentAsString() {
        try {
            Template template = getTemplate();
            StringWriter writer = new StringWriter();
            template.merge(null, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
