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

package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.path;

public class ArtifactStoresDelegate implements SparkController {

    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine templateEngine;

    public ArtifactStoresDelegate(SPAAuthenticationHelper authenticationHelper, TemplateEngine templateEngine) {
        this.authenticationHelper = authenticationHelper;
        this.templateEngine = templateEngine;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ArtifactStoresSPA.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkAdminUserAnd401);
            get("", this::index, templateEngine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        HashMap<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "Artifact Stores");
        }};
        return new ModelAndView(object, "artifact_stores/index.vm");
    }
}
