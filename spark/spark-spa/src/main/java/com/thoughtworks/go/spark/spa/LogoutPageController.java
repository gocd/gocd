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

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.Map;

import static spark.Spark.get;

public class LogoutPageController implements SparkController {
    private final TemplateEngine engine;
    private final LoginLogoutHelper loginLogoutHelper;

    public LogoutPageController(TemplateEngine engine, LoginLogoutHelper loginLogoutHelper) {
        this.engine = engine;
        this.loginLogoutHelper = loginLogoutHelper;
    }

    @Override
    public String controllerBasePath() {
        return Routes.LogoutPage.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        get(controllerBasePath(), this::show, engine);
    }

    public ModelAndView show(Request request, Response response) {
        SessionUtils.recreateSessionWithoutCopyingOverSessionState(request.raw());

        Map<String, Object> meta = loginLogoutHelper.buildMeta(request);

        Map<String, Object> object = ImmutableMap.<String, Object>builder()
                .put("viewTitle", "Logout")
                .put("meta", meta)
                .build();

        return new ModelAndView(object, null);
    }
}
