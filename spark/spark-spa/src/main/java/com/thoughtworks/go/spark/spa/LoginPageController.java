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
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static com.thoughtworks.go.server.newsecurity.utils.SessionUtils.isAnonymousAuthenticationToken;
import static spark.Spark.get;

public class LoginPageController implements SparkController {
    private final TemplateEngine engine;
    private final LoginLogoutHelper loginLogoutHelper;
    private final SecurityService securityService;
    private final Clock clock;
    private final SystemEnvironment systemEnvironment;

    public LoginPageController(TemplateEngine engine, LoginLogoutHelper loginLogoutHelper, SecurityService securityService, Clock clock, SystemEnvironment systemEnvironment) {
        this.engine = engine;
        this.loginLogoutHelper = loginLogoutHelper;
        this.securityService = securityService;
        this.clock = clock;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String controllerBasePath() {
        return Routes.LoginPage.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        get(controllerBasePath(), this::show, engine);
    }

    public ModelAndView show(Request request, Response response) {
        if (securityIsDisabledOrAlreadyLoggedIn(request.raw())) {
            response.redirect("/go/pipelines");
            return null;
        }

        Map<String, Object> meta = loginLogoutHelper.buildMeta(request);

        Map<String, Object> object = ImmutableMap.<String, Object>builder()
                .put("viewTitle", "Login")
                .put("meta", meta)
                .build();

        return new ModelAndView(object, null);
    }

    private boolean securityIsDisabledOrAlreadyLoggedIn(HttpServletRequest request) {
        boolean securityEnabled = securityService.isSecurityEnabled();
        boolean anonymousAuthenticationToken = isAnonymousAuthenticationToken(request);
        boolean authenticated = SessionUtils.isAuthenticated(request, clock, systemEnvironment);
        return !securityEnabled || (!anonymousAuthenticationToken && authenticated);
    }

}
