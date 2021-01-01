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

import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class TemplateConfigController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;

    public TemplateConfigController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AdminTemplates.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(Routes.AdminTemplates.NAME + "/edit", authenticationHelper::checkAdminOrTemplateAdminAnd403);
            get(Routes.AdminTemplates.NAME + "/edit", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        Map<String, Object> object = new HashMap<>() {{
            put("viewTitle", "Templates");
            put("meta", meta(request.params("template_name")));
        }};

        return new ModelAndView(object, null);
    }

    private Map<String, String> meta(String templateName) {
        final Map<String, String> meta = new HashMap<>();
        meta.put("templateName", templateName);
        return meta;
    }
}
