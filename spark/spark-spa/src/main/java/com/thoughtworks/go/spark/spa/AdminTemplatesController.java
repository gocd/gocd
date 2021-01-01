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
package com.thoughtworks.go.spark.spa;

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.Map;

import static spark.Spark.*;

public class AdminTemplatesController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private final FeatureToggleService featureToggleService;

    public AdminTemplatesController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, FeatureToggleService featureToggleService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AdminTemplates.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkViewAccessToTemplateAnd403);
            get("", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        Map<String, Object> meta = ImmutableMap.<String, Object>builder()
                .put("showRailsTemplateAuthorization", featureToggleService.isToggleOn(Toggles.USE_RAILS_TEMPLATE_AUTHORIZATION_PAGE))
                .build();
        Map<String, Object> object = ImmutableMap.<String, Object>builder()
                .put("viewTitle", "AdminTemplates")
                .put("meta", meta)
                .build();
        return new ModelAndView(object, null);
    }
}
