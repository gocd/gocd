/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.service.support.toggle.Toggles;
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

public class AdminAccessTokensController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;

    public AdminAccessTokensController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AdminAccessTokens.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkAdminUserAnd403);
            before("", this::checkForToggle);
            get("", this::index, engine);
        });
    }

    private void checkForToggle(Request request, Response response) {
        if (!Toggles.isToggleOn(Toggles.ENABLE_ADMIN_ACCESS_TOKENS_SPA)) {
            throw authenticationHelper.renderNotFoundResponse();
        }
    }

    public ModelAndView index(Request request, Response response) {
        Map<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "Admin Access Tokens");
        }};
        return new ModelAndView(object, null);
    }
}
