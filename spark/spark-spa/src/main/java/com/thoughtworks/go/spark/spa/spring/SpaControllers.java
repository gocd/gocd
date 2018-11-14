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

package com.thoughtworks.go.spark.spa.spring;

import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spa.*;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaControllers implements SparkSpringController {
    private static final String DEFAULT_LAYOUT_PATH = "layouts/single_page_app.vm";
    private static final String COMPONENT_LAYOUT_PATH = "layouts/component_layout.vm";

    private final List<SparkController> sparkControllers = new ArrayList<>();

    @Autowired
    public SpaControllers(SPAAuthenticationHelper authenticationHelper, VelocityTemplateEngineFactory templateEngineFactory,
                          SecurityService securityService, PipelineConfigService pipelineConfigService,
                          SystemEnvironment systemEnvironment, AnalyticsExtension analyticsExtension,
                          FeatureToggleService featureToggleService) {

        LayoutTemplateProvider componentAware = () -> featureToggleService.isToggleOn(Toggles.COMPONENTS) ? COMPONENT_LAYOUT_PATH : DEFAULT_LAYOUT_PATH;
        LayoutTemplateProvider defaultTemplate = () -> DEFAULT_LAYOUT_PATH;

        final LayoutTemplateProvider elasticProfileSPAPath = () -> featureToggleService.isToggleOn(Toggles.USE_OLD_ELASTIC_PROFILE_SPA) ? DEFAULT_LAYOUT_PATH : COMPONENT_LAYOUT_PATH;

        sparkControllers.add(new RolesControllerDelegate(authenticationHelper, templateEngineFactory.create(RolesControllerDelegate.class, defaultTemplate)));
        sparkControllers.add(new AuthConfigsDelegate(authenticationHelper, templateEngineFactory.create(AuthConfigsDelegate.class, defaultTemplate)));
        sparkControllers.add(new AgentsControllerDelegate(authenticationHelper, templateEngineFactory.create(AgentsControllerDelegate.class, defaultTemplate), securityService, systemEnvironment));
        sparkControllers.add(new PluginsDelegate(authenticationHelper, templateEngineFactory.create(PluginsDelegate.class, componentAware), securityService));
        sparkControllers.add(new ElasticProfilesDelegate(authenticationHelper, templateEngineFactory.create(ElasticProfilesDelegate.class, elasticProfileSPAPath)));
        sparkControllers.add(new NewDashboardDelegate(authenticationHelper, templateEngineFactory.create(NewDashboardDelegate.class, defaultTemplate), securityService, systemEnvironment, pipelineConfigService));
        sparkControllers.add(new ArtifactStoresDelegate(authenticationHelper, templateEngineFactory.create(ArtifactStoresDelegate.class, defaultTemplate)));
        sparkControllers.add(new AnalyticsDelegate(authenticationHelper, templateEngineFactory.create(AnalyticsDelegate.class, defaultTemplate), systemEnvironment, analyticsExtension, pipelineConfigService));
        sparkControllers.add(new DataSharingSettingsDelegate(authenticationHelper, templateEngineFactory.create(DataSharingSettingsDelegate.class, defaultTemplate)));
        sparkControllers.add(new ConfigReposDelegate(authenticationHelper, templateEngineFactory.create(ConfigReposDelegate.class, () -> COMPONENT_LAYOUT_PATH), featureToggleService));
        sparkControllers.add(new KitchenSinkDelegate(templateEngineFactory.create(KitchenSinkDelegate.class, () -> COMPONENT_LAYOUT_PATH)));
    }

    @Override
    public void setupRoutes() {
        for (SparkController sparkController : sparkControllers) {
            sparkController.setupRoutes();
        }
    }
}
