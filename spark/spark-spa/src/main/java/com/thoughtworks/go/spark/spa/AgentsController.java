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

import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static spark.Spark.*;

public class AgentsController implements SparkController {

    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private final SecurityService securityService;
    private SystemEnvironment systemEnvironment;
    private final FeatureToggleService featureToggleService;

    public AgentsController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, SecurityService securityService, SystemEnvironment systemEnvironment, FeatureToggleService featureToggleService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.securityService = securityService;
        this.systemEnvironment = systemEnvironment;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AgentsSPA.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkUserAnd403);
            get("", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        return featureToggleService.isToggleOn(Toggles.SHOW_NEW_AGENTS_SPA) ? renderNewAgentSPA() : renderOldAgentSPA();
    }

    private ModelAndView renderOldAgentSPA() {
        Map<Object, Object> object = new HashMap<>() {{
            put("viewTitle", "Agents");
            put("isUserAnAdmin", securityService.isUserAdmin(currentUsername()));
            put("shouldShowAnalyticsIcon", showAnalyticsIcon());
        }};

        return new ModelAndView(object, "agents/index.ftlh");
    }

    private ModelAndView renderNewAgentSPA() {
        Map<Object, Object> object = new HashMap<>() {{
            put("viewTitle", "Agents");
            put("meta", singletonMap("data-should-show-analytics-icon", showAnalyticsIcon()));
        }};

        return new ModelAndView(object, null);
    }

    private boolean showAnalyticsIcon() {
        return !systemEnvironment.enableAnalyticsOnlyForAdmins() || securityService.isUserAdmin(currentUsername());
    }
}
