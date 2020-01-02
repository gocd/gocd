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
package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;

import static com.thoughtworks.go.spark.Routes.PipelineConfig.SPA_AS_CODE;
import static com.thoughtworks.go.spark.Routes.PipelineConfig.SPA_BASE;
import static spark.Spark.*;

public class PipelinesAsCodeController implements SparkController {
    private SPAAuthenticationHelper authenticationHelper;
    private TemplateEngine engine;

    public PipelinesAsCodeController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
    }

    @Override
    public String controllerBasePath() {
        return SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(SPA_AS_CODE, authenticationHelper::checkAdminUserAnd403);
            get(SPA_AS_CODE, this::asCode, engine);
        });
    }

    public ModelAndView asCode(Request req, Response res) {
        HashMap<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "New Pipeline as Code");
        }};

        return new ModelAndView(object, null);
    }
}
