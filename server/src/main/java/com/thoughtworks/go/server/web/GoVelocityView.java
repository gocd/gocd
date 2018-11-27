/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.DrainModeService;
import com.thoughtworks.go.server.service.RailsAssetsService;
import com.thoughtworks.go.server.service.VersionInfoService;
import com.thoughtworks.go.server.service.WebpackAssetsService;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.servlet.view.velocity.VelocityToolboxView;

import javax.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.util.Set;

public class GoVelocityView extends VelocityToolboxView {
    public static final String PRINCIPAL = "principal";
    public static final String ADMINISTRATOR = "userHasAdministratorRights";
    public static final String TEMPLATE_ADMINISTRATOR = "userHasTemplateAdministratorRights";
    public static final String VIEW_ADMINISTRATOR_RIGHTS = "userHasViewAdministratorRights";
    public static final String TEMPLATE_VIEW_USER = "userHasTemplateViewUserRights";
    public static final String GROUP_ADMINISTRATOR = "userHasGroupAdministratorRights";
    public static final String USE_COMPRESS_JS = "useCompressJS";
    public static final String CONCATENATED_JAVASCRIPT_FILE_PATH = "concatenatedJavascriptFilePath";
    public static final String CONCATENATED_APPLICATION_CSS_FILE_PATH = "concatenatedApplicationCssFilePath";
    public static final String CONCATENATED_DRAIN_MODE_BANNER_CSS_FILE_PATH = "concatenatedDrainModeBannerCssFilePath";
    public static final String CURRENT_GOCD_VERSION = "currentGoCDVersion";
    public static final String CONCATENATED_VM_APPLICATION_CSS_FILE_PATH = "concatenatedVmApplicationCssFilePath";
    public static final String CONCATENATED_CSS_APPLICATION_CSS_FILE_PATH = "concatenatedCssApplicationCssFilePath";
    public static final String CONCATENATED_NEW_THEME_CSS_FILE_PATH = "concatenatedNewThemeCssFilePath";
    public static final String CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH = "concatenatedStageBarCancelledIconFilePath";
    public static final String CONCATENATED_SPINNER_ICON_FILE_PATH = "concatenatedSpinnerIconFilePath";
    public static final String CONCATENATED_CRUISE_ICON_FILE_PATH = "concatenatedCruiseIconFilePath";
    public static final String PATH_RESOLVER = "pathResolver";
    public static final String GO_UPDATE = "goUpdate";
    public static final String GO_UPDATE_CHECK_ENABLED = "goUpdateCheckEnabled";
    public static final String SUPPORTS_ANALYTICS_DASHBOARD = "supportsAnalyticsDashboard";
    public static final String WEBPACK_ASSETS_SERVICE = "webpackAssetsService";
    public static final String IS_SERVER_IN_DRAIN_MODE = "isServerInDrainMode";

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

    DrainModeService drainModeService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(DrainModeService.class);
    }

    VersionInfoService getVersionInfoService() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(VersionInfoService.class);
    }

    DefaultPluginInfoFinder getPluginInfoFinder() {
        return this.getApplicationContext().getAutowireCapableBeanFactory().getBean(DefaultPluginInfoFinder.class);
    }

    protected void exposeHelpers(Context velocityContext, HttpServletRequest request) throws Exception {
        RailsAssetsService railsAssetsService = getRailsAssetsService();
        VersionInfoService versionInfoService = getVersionInfoService();

        velocityContext.put(ADMINISTRATOR, true);
        velocityContext.put(GROUP_ADMINISTRATOR, true);
        velocityContext.put(TEMPLATE_ADMINISTRATOR, true);
        velocityContext.put(VIEW_ADMINISTRATOR_RIGHTS, true);
        velocityContext.put(TEMPLATE_VIEW_USER, true);
        velocityContext.put(USE_COMPRESS_JS, systemEnvironment.useCompressedJs());

        velocityContext.put(Toggles.PIPELINE_COMMENT_FEATURE_TOGGLE_KEY, Toggles.isToggleOn(Toggles.PIPELINE_COMMENT_FEATURE_TOGGLE_KEY));
        velocityContext.put(Toggles.QUICK_EDIT_PAGE_DEFAULT, Toggles.isToggleOn(Toggles.QUICK_EDIT_PAGE_DEFAULT));
        velocityContext.put(Toggles.PIPELINE_CONFIG_SINGLE_PAGE_APP, Toggles.isToggleOn(Toggles.PIPELINE_CONFIG_SINGLE_PAGE_APP));
        velocityContext.put(Toggles.CONFIG_REPOS_UI, Toggles.isToggleOn(Toggles.CONFIG_REPOS_UI));

        velocityContext.put(CONCATENATED_JAVASCRIPT_FILE_PATH, railsAssetsService.getAssetPath("application.js"));
        velocityContext.put(CONCATENATED_APPLICATION_CSS_FILE_PATH, railsAssetsService.getAssetPath("application.css"));
        velocityContext.put(CONCATENATED_DRAIN_MODE_BANNER_CSS_FILE_PATH, railsAssetsService.getAssetPath("single_page_apps/drain_mode_banner.css"));
        velocityContext.put(CURRENT_GOCD_VERSION, CurrentGoCDVersion.getInstance());
        velocityContext.put(CONCATENATED_VM_APPLICATION_CSS_FILE_PATH, railsAssetsService.getAssetPath("vm/application.css"));
        velocityContext.put(CONCATENATED_CSS_APPLICATION_CSS_FILE_PATH, railsAssetsService.getAssetPath("css/application.css"));
        velocityContext.put(CONCATENATED_NEW_THEME_CSS_FILE_PATH, railsAssetsService.getAssetPath("new-theme.css"));
        velocityContext.put(CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH, railsAssetsService.getAssetPath("g9/stage_bar_cancelled_icon.png"));
        velocityContext.put(CONCATENATED_SPINNER_ICON_FILE_PATH, railsAssetsService.getAssetPath("spinner.gif"));
        velocityContext.put(CONCATENATED_CRUISE_ICON_FILE_PATH, railsAssetsService.getAssetPath("cruise.ico"));

        velocityContext.put(PATH_RESOLVER, railsAssetsService);
        velocityContext.put(GO_UPDATE, versionInfoService.getGoUpdate());
        velocityContext.put(GO_UPDATE_CHECK_ENABLED, versionInfoService.isGOUpdateCheckEnabled());

        velocityContext.put(SUPPORTS_ANALYTICS_DASHBOARD, supportsAnalyticsDashboard());
        velocityContext.put(WEBPACK_ASSETS_SERVICE, webpackAssetsService());
        velocityContext.put(IS_SERVER_IN_DRAIN_MODE, drainModeService().isDrainMode());

        if (!SessionUtils.hasAuthenticationToken(request)) {
            return;
        }

        final AuthenticationToken<?> authentication = SessionUtils.getAuthenticationToken(request);

        setPrincipal(velocityContext, authentication);
        setAdmininstratorRole(velocityContext, authentication);
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

    private void setAdmininstratorRole(Context velocityContext, AuthenticationToken<?> authentication) {
        final Set<GrantedAuthority> authorities = authentication.getUser().getAuthorities();
        if (authorities == null) {
            return;
        }
        removeAdminFromContextIfNecessary(velocityContext, authorities);
        removeGroupAdminFromContextIfNecessary(velocityContext, authorities);
        removeTemplateAdminFromContextIfNecessary(velocityContext, authorities);
        removeTemplateViewUserFromContextIfNecessary(velocityContext, authorities);
        removeViewAdminRightsFromContextIfNecessary(velocityContext);

    }

    private void removeViewAdminRightsFromContextIfNecessary(Context context) {
        if (!(context.containsKey(ADMINISTRATOR) || context.containsKey(GROUP_ADMINISTRATOR) || context.containsKey(TEMPLATE_ADMINISTRATOR) || context.containsKey(TEMPLATE_VIEW_USER)))
            context.remove(VIEW_ADMINISTRATOR_RIGHTS);
    }

    private void removeGroupAdminFromContextIfNecessary(Context velocityContext, Set<GrantedAuthority> authorities) {
        boolean administrator = false;
        for (GrantedAuthority authority : authorities) {
            if (isGroupAdministrator(authority)) {
                administrator = true;
            }
        }
        if (!administrator) {
            velocityContext.remove(GROUP_ADMINISTRATOR);
        }
    }

    private void removeTemplateAdminFromContextIfNecessary(Context velocityContext, Set<GrantedAuthority> authorities) {
        boolean administrator = false;
        for (GrantedAuthority authority : authorities) {
            if (isTemplateAdministrator(authority)) {
                administrator = true;
            }
        }
        if (!administrator) {
            velocityContext.remove(TEMPLATE_ADMINISTRATOR);
        }
    }

    private void removeTemplateViewUserFromContextIfNecessary(Context velocityContext, Set<GrantedAuthority> authorities) {
        boolean isTemplateViewUser = false;
        for (GrantedAuthority authority : authorities) {
            if (isTemplateViewUser(authority)) {
                isTemplateViewUser = true;
            }
        }

        if (!isTemplateViewUser) {
            velocityContext.remove(TEMPLATE_VIEW_USER);
        }
    }

    private void removeAdminFromContextIfNecessary(Context velocityContext, Set<GrantedAuthority> authorities) {
        boolean administrator = false;
        for (GrantedAuthority authority : authorities) {
            if (isAdministrator(authority)) {
                administrator = true;
            }
        }
        if (!administrator) {
            velocityContext.remove(ADMINISTRATOR);
        }
    }

    private boolean isAdministrator(GrantedAuthority authority) {
        return authority.equals(GoAuthority.ROLE_SUPERVISOR.asAuthority());
    }

    private boolean isGroupAdministrator(GrantedAuthority authority) {
        return authority.equals(GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
    }

    private boolean isTemplateAdministrator(GrantedAuthority authority) {
        return authority.equals(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.asAuthority());
    }

    private boolean isTemplateViewUser(GrantedAuthority authority) {
        return authority.equals(GoAuthority.ROLE_TEMPLATE_VIEW_USER.asAuthority());
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
