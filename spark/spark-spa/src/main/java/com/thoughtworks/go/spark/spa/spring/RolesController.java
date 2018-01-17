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

import com.thoughtworks.go.spark.spa.RolesControllerDelegate;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RolesController implements SparkSpringController {
    private final RolesControllerDelegate delegate;

    @Autowired
    public RolesController(SPAAuthenticationHelper authenticationHelper, VelocityTemplateEngineFactory templateEngineFactory) {
        this.delegate = new RolesControllerDelegate(authenticationHelper, templateEngineFactory.create(RolesControllerDelegate.class, "layouts/single_page_app.vm"));
    }

    @Override
    public void setupRoutes() {
        delegate.setupRoutes();
    }
}
