/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spa.*;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.TemplateEngine;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaControllers implements SparkSpringController {
    private static final String DEFAULT_LAYOUT_PATH = "layouts/single_page_app.ftlh";
    private static final String COMPONENT_LAYOUT_PATH = "layouts/component_layout.ftlh";

    private final List<SparkController> sparkControllers = new ArrayList<>();

    @Autowired
    public SpaControllers(SPAAuthenticationHelper authenticationHelper, FreemarkerTemplateEngineFactory templateEngineFactory,
                          SecurityService securityService, PipelineConfigService pipelineConfigService,
                          SystemEnvironment systemEnvironment, AnalyticsExtension analyticsExtension,
                          GoConfigService goConfigService,
                          AuthorizationExtensionCacheService authorizationExtensionCacheService,
                          SecurityAuthConfigService securityAuthConfigService,
                          BackupService backupService,
                          Clock clock, ArtifactsDirHolder artifactsDirHolder, SystemService systemService,
                          GoCache goCache) {
        LayoutTemplateProvider defaultTemplate = () -> DEFAULT_LAYOUT_PATH;
        LayoutTemplateProvider componentTemplate = () -> COMPONENT_LAYOUT_PATH;

        sparkControllers.add(new AgentJobRunHistoryController(authenticationHelper, templateEngineFactory.create(AgentJobRunHistoryController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AdminTemplatesController(authenticationHelper, templateEngineFactory.create(AdminTemplatesController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new PipelineActivityController(authenticationHelper, templateEngineFactory.create(PipelineActivityController.class, () -> COMPONENT_LAYOUT_PATH), goConfigService, securityService));
        sparkControllers.add(new AdminPipelinesController(authenticationHelper, templateEngineFactory.create(AdminPipelinesController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ServerConfigurationController(authenticationHelper, templateEngineFactory.create(ServerConfigurationController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new NewEnvironmentsController(authenticationHelper, templateEngineFactory.create(NewEnvironmentsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ElasticAgentsController(authenticationHelper, templateEngineFactory.create(ElasticAgentsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new PipelineConfigsController(authenticationHelper, templateEngineFactory.create(PipelineConfigsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AgentsController(systemEnvironment, securityService, authenticationHelper, templateEngineFactory.create(AgentsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ServerInfoController(authenticationHelper, templateEngineFactory.create(ServerInfoController.class, () -> COMPONENT_LAYOUT_PATH), artifactsDirHolder, systemService, pipelineConfigService));
        sparkControllers.add(new LogoutPageController(templateEngineFactory.create(LogoutPageController.class, () -> COMPONENT_LAYOUT_PATH), new LoginLogoutHelper(goConfigService, AuthorizationMetadataStore.instance())));
        sparkControllers.add(new LoginPageController(templateEngineFactory.create(LoginPageController.class, () -> COMPONENT_LAYOUT_PATH), new LoginLogoutHelper(goConfigService, AuthorizationMetadataStore.instance()), securityService, clock, systemEnvironment));
        sparkControllers.add(new AccessTokensController(authenticationHelper, authorizationExtensionCacheService, securityAuthConfigService, templateEngineFactory.create(AccessTokensController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AdminAccessTokensController(authenticationHelper, templateEngineFactory.create(AdminAccessTokensController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ArtifactStoresController(authenticationHelper, templateEngineFactory.create(ArtifactStoresController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AuthConfigsController(authenticationHelper, templateEngineFactory.create(AuthConfigsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new UsersController(authenticationHelper, templateEngineFactory.create(UsersController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new RolesController(authenticationHelper, templateEngineFactory.create(RolesController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new NewDashboardController(authenticationHelper, templateEngineFactory.create(NewDashboardController.class, defaultTemplate), securityService, systemEnvironment, pipelineConfigService));
        sparkControllers.add(new AnalyticsController(authenticationHelper, templateEngineFactory.create(AnalyticsController.class, defaultTemplate), systemEnvironment, analyticsExtension, pipelineConfigService));
        sparkControllers.add(new DataSharingSettingsController(authenticationHelper, templateEngineFactory.create(DataSharingSettingsController.class, defaultTemplate)));
        sparkControllers.add(new MaintenanceModeController(authenticationHelper, templateEngineFactory.create(MaintenanceModeController.class, componentTemplate)));
        sparkControllers.add(new ConfigReposController(authenticationHelper, templateEngineFactory.create(ConfigReposController.class, componentTemplate)));
        sparkControllers.add(new KitchenSinkController(templateEngineFactory.create(KitchenSinkController.class, componentTemplate)));
        sparkControllers.add(new PluginsController(authenticationHelper, templateEngineFactory.create(PluginsController.class, componentTemplate)));
        sparkControllers.add(new ElasticAgentConfigurationsController(authenticationHelper, templateEngineFactory.create(ElasticAgentConfigurationsController.class, componentTemplate)));
        sparkControllers.add(new BackupsController(authenticationHelper, templateEngineFactory.create(BackupsController.class, componentTemplate), backupService));
        sparkControllers.add(new PipelinesController(authenticationHelper, templateEngineFactory.create(PipelinesController.class, componentTemplate), goCache));
        sparkControllers.add(new PipelinesAsCodeController(authenticationHelper, templateEngineFactory.create(PipelinesAsCodeController.class, componentTemplate)));
        sparkControllers.add(new SecretConfigsController(authenticationHelper, templateEngineFactory.create(SecretConfigsController.class, componentTemplate)));
    }

    @Override
    public void setupRoutes() {
        for (SparkController sparkController : sparkControllers) {
            sparkController.setupRoutes();
        }
    }
}
