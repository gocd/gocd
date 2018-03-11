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

import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spa.AgentsControllerDelegate;
import com.thoughtworks.go.spark.spa.RolesControllerDelegate;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaControllers implements SparkSpringController {
    private final List<SparkController> sparkControllers = new ArrayList<>();

    @Autowired
    public SpaControllers(SPAAuthenticationHelper authenticationHelper, VelocityTemplateEngineFactory templateEngineFactory, SecurityService securityService) {
        sparkControllers.add(new RolesControllerDelegate(authenticationHelper, templateEngineFactory.create(RolesControllerDelegate.class, "layouts/single_page_app.vm")));
        sparkControllers.add(new AgentsControllerDelegate(authenticationHelper, templateEngineFactory.create(AgentsControllerDelegate.class, "layouts/single_page_app.vm"), securityService));
    }

    @Override
    public void setupRoutes() {
        for (SparkController sparkController : sparkControllers) {
            sparkController.setupRoutes();
        }
    }
}
