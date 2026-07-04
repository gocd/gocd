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

import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.server.caching.GoCache;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.spark.GlobalExceptionMapper;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spa.*;
import com.thoughtworks.go.spark.spring.SpaAuthorizationHelper;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaControllers implements SparkSpringController {
    private static final String DEFAULT_LAYOUT_PATH = "layouts/single_page_app.ftlh";
    private static final String COMPONENT_LAYOUT_PATH = "layouts/component_layout.ftlh";
    private static final String RAILS_COMPATIBLE_PAGE_LAYOUT_PATH = "layouts/rails_compatible_page.ftlh";

    private final List<SparkController> sparkControllers = new ArrayList<>();

    @Autowired
    public SpaControllers(SpaAuthorizationHelper authorizationHelper,
                          FreemarkerTemplateEngineFactory templateEngineFactory,
                          SecurityService securityService,
                          PipelineConfigService pipelineConfigService,
                          SystemEnvironment systemEnvironment,
                          AnalyticsExtension analyticsExtension,
                          GoConfigService goConfigService,
                          AuthorizationExtensionCacheService authorizationExtensionCacheService,
                          SecurityAuthConfigService securityAuthConfigService,
                          BackupService backupService,
                          Clock clock,
                          ArtifactsDirHolder artifactsDirHolder,
                          GoCache goCache,
                          ElasticAgentPluginService elasticAgentPluginService,
                          JobInstanceService jobInstanceService,
                          PipelineService pipelineService,
                          FeatureToggleService featureToggleService) {
        LayoutTemplateProvider defaultTemplate = () -> DEFAULT_LAYOUT_PATH;
        LayoutTemplateProvider componentTemplate = () -> COMPONENT_LAYOUT_PATH;
        LayoutTemplateProvider railsCompatibleTemplate = () -> RAILS_COMPATIBLE_PAGE_LAYOUT_PATH;

        sparkControllers.add(new NewPreferencesController(authorizationHelper, templateEngineFactory.create(NewPreferencesController.class, () -> COMPONENT_LAYOUT_PATH), goConfigService));
		sparkControllers.add(new MaterialsController(authorizationHelper, templateEngineFactory.create(MaterialsController.class, () -> COMPONENT_LAYOUT_PATH)));
		sparkControllers.add(new TemplateConfigController(authorizationHelper, templateEngineFactory.create(TemplateConfigController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new PluggableScmsController(authorizationHelper, templateEngineFactory.create(PluggableScmsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new PackageRepositoriesController(authorizationHelper, templateEngineFactory.create(PackageRepositoriesController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ClickyPipelineConfigController(authorizationHelper, goConfigService, templateEngineFactory.create(ClickyPipelineConfigController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new StatusReportsController(authorizationHelper, templateEngineFactory.create(
                StatusReportsController.class, railsCompatibleTemplate), elasticAgentPluginService, jobInstanceService));
        sparkControllers.add(new CompareController(authorizationHelper, templateEngineFactory.create(CompareController.class, () -> COMPONENT_LAYOUT_PATH), pipelineService));
        sparkControllers.add(new AgentJobRunHistoryController(authorizationHelper, templateEngineFactory.create(AgentJobRunHistoryController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AdminTemplatesController(authorizationHelper, templateEngineFactory.create(AdminTemplatesController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new PipelineActivityController(authorizationHelper, templateEngineFactory.create(PipelineActivityController.class, () -> COMPONENT_LAYOUT_PATH), goConfigService, securityService));
        sparkControllers.add(new AdminPipelinesController(authorizationHelper, templateEngineFactory.create(AdminPipelinesController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ServerConfigurationController(authorizationHelper, templateEngineFactory.create(ServerConfigurationController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new NewEnvironmentsController(authorizationHelper, templateEngineFactory.create(NewEnvironmentsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AgentsController(systemEnvironment, securityService, authorizationHelper, templateEngineFactory.create(AgentsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ServerInfoController(authorizationHelper, templateEngineFactory.create(ServerInfoController.class, () -> COMPONENT_LAYOUT_PATH), artifactsDirHolder, pipelineConfigService));
        sparkControllers.add(new LogoutPageController(templateEngineFactory.create(LogoutPageController.class, () -> COMPONENT_LAYOUT_PATH), new LoginLogoutHelper(goConfigService, AuthorizationMetadataStore.instance())));
        sparkControllers.add(new LoginPageController(templateEngineFactory.create(LoginPageController.class, () -> COMPONENT_LAYOUT_PATH), new LoginLogoutHelper(goConfigService, AuthorizationMetadataStore.instance()), securityService, clock, systemEnvironment));
        sparkControllers.add(new AccessTokensController(authorizationHelper, authorizationExtensionCacheService, securityAuthConfigService, templateEngineFactory.create(AccessTokensController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AdminAccessTokensController(authorizationHelper, templateEngineFactory.create(AdminAccessTokensController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new ArtifactStoresController(authorizationHelper, templateEngineFactory.create(ArtifactStoresController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new AuthConfigsController(authorizationHelper, templateEngineFactory.create(AuthConfigsController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new UsersController(authorizationHelper, templateEngineFactory.create(UsersController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new RolesController(authorizationHelper, templateEngineFactory.create(RolesController.class, () -> COMPONENT_LAYOUT_PATH)));
        sparkControllers.add(new NewDashboardController(authorizationHelper, templateEngineFactory.create(NewDashboardController.class, defaultTemplate), securityService, systemEnvironment, pipelineConfigService));
        sparkControllers.add(new AnalyticsController(authorizationHelper, templateEngineFactory.create(AnalyticsController.class, defaultTemplate), systemEnvironment, analyticsExtension, pipelineConfigService));
        sparkControllers.add(new MaintenanceModeController(authorizationHelper, templateEngineFactory.create(MaintenanceModeController.class, componentTemplate)));
        sparkControllers.add(new ConfigReposController(authorizationHelper, templateEngineFactory.create(ConfigReposController.class, componentTemplate)));
        sparkControllers.add(new KitchenSinkController(templateEngineFactory.create(KitchenSinkController.class, componentTemplate)));
        sparkControllers.add(new PluginsController(authorizationHelper, templateEngineFactory.create(PluginsController.class, componentTemplate)));
        sparkControllers.add(new ElasticAgentConfigurationsController(authorizationHelper, templateEngineFactory.create(ElasticAgentConfigurationsController.class, componentTemplate)));
        sparkControllers.add(new BackupsController(authorizationHelper, templateEngineFactory.create(BackupsController.class, componentTemplate), backupService));
        sparkControllers.add(new PipelinesController(authorizationHelper, templateEngineFactory.create(PipelinesController.class, componentTemplate)));
        sparkControllers.add(new PipelinesAsCodeController(authorizationHelper, templateEngineFactory.create(PipelinesAsCodeController.class, componentTemplate)));
        sparkControllers.add(new SecretConfigsController(authorizationHelper, templateEngineFactory.create(SecretConfigsController.class, componentTemplate)));
    }

    @Override
    public void setupRoutes(GlobalExceptionMapper exceptionMapper) {
        for (SparkController sparkController : sparkControllers) {
            sparkController.setupRoutes(exceptionMapper);
        }
    }
}
