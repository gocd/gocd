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

import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;

import static spark.Spark.*;

public class DrainModeController implements SparkController {
    private SPAAuthenticationHelper authenticationHelper;
    private TemplateEngine engine;
    private FeatureToggleService features;

    public DrainModeController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, FeatureToggleService features) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.features = features;
    }

    @Override
    public String controllerBasePath() {
        return Routes.DrainMode.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkAdminUserAnd403);
            before("", this::featureToggleGuard);
            get("", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        HashMap<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "Server Drain Mode");
        }};
        return new ModelAndView(object, "drain_mode/index.vm");
    }

    private void featureToggleGuard(Request req, Response res) {
        if (!features.isToggleOn(Toggles.SERVER_DRAIN_MODE_API_TOGGLE_KEY)) {
            res.redirect("/");
        }
    }
}
