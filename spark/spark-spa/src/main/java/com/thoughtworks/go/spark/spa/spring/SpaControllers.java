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
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleListener;
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
    private final List<SparkController> sparkControllers = new ArrayList<>();

    @Autowired
    public SpaControllers(SPAAuthenticationHelper authenticationHelper, VelocityTemplateEngineFactory templateEngineFactory,
                          SecurityService securityService, PipelineConfigService pipelineConfigService,
                          SystemEnvironment systemEnvironment, AnalyticsExtension analyticsExtension,
                          FeatureToggleService featureToggleService) {

        sparkControllers.add(new RolesControllerDelegate(authenticationHelper, templateEngineFactory.create(RolesControllerDelegate.class, "layouts/single_page_app.vm")));
        sparkControllers.add(new AuthConfigsDelegate(authenticationHelper, templateEngineFactory.create(AuthConfigsDelegate.class, "layouts/single_page_app.vm")));
        sparkControllers.add(new AgentsControllerDelegate(authenticationHelper, templateEngineFactory.create(AgentsControllerDelegate.class, "layouts/single_page_app.vm"), securityService, systemEnvironment));
        if (featureToggleService.isToggleOn(Toggles.COMPONENTS)) {
            sparkControllers.add(new PluginsDelegate(authenticationHelper, templateEngineFactory.create(PluginsDelegate.class, "layouts/component_layout.vm"), securityService));
        } else {
            sparkControllers.add(new PluginsDelegate(authenticationHelper, templateEngineFactory.create(PluginsDelegate.class, "layouts/single_page_app.vm"), securityService));
        }
        sparkControllers.add(new ElasticProfilesDelegate(authenticationHelper, templateEngineFactory.create(ElasticProfilesDelegate.class, "layouts/single_page_app.vm")));
        sparkControllers.add(new NewDashboardDelegate(authenticationHelper, templateEngineFactory.create(NewDashboardDelegate.class, "layouts/single_page_app.vm"), securityService, systemEnvironment, pipelineConfigService));
        sparkControllers.add(new ArtifactStoresDelegate(authenticationHelper, templateEngineFactory.create(ArtifactStoresDelegate.class, "layouts/single_page_app.vm")));
        sparkControllers.add(new AnalyticsDelegate(authenticationHelper, templateEngineFactory.create(AnalyticsDelegate.class, "layouts/single_page_app.vm"), systemEnvironment, analyticsExtension, pipelineConfigService));
        sparkControllers.add(new DataSharingSettingsDelegate(authenticationHelper, templateEngineFactory.create(DataSharingSettingsDelegate.class, "layouts/single_page_app.vm")));
        sparkControllers.add(new ConfigReposDelegate(authenticationHelper, templateEngineFactory.create(ConfigReposDelegate.class, "layouts/single_page_app.vm")));
    }

    @Override
    public void setupRoutes() {
        for (SparkController sparkController : sparkControllers) {
            sparkController.setupRoutes();
        }
    }
}
