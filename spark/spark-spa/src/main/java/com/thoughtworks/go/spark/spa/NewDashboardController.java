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

import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.SecurityService;
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

import static spark.Spark.*;

public class NewDashboardController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private final SecurityService securityService;
    private SystemEnvironment systemEnvironment;
    private PipelineConfigService pipelineConfigService;

    public NewDashboardController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, SecurityService securityService,
                                  SystemEnvironment systemEnvironment, PipelineConfigService pipelineConfigService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.securityService = securityService;
        this.systemEnvironment = systemEnvironment;
        this.pipelineConfigService = pipelineConfigService;
    }


    @Override
    public String controllerBasePath() {
        return Routes.NewDashboardSPA.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkUserAnd403);
            get("", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        Username username = currentUsername();
        if (pipelineConfigService.viewableGroupsFor(username).isEmpty() && securityService.canCreatePipelines(username)) {
            response.redirect("/go/admin/pipelines/create?group=defaultGroup");
            return null;
        }
        HashMap<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "Dashboard");
            put("showEmptyPipelineGroups", Toggles.isToggleOn(Toggles.ALLOW_EMPTY_PIPELINE_GROUPS_DASHBOARD));
            put("shouldShowAnalyticsIcon", showAnalyticsIcon());
            put("testDrive", Toggles.isToggleOn(Toggles.TEST_DRIVE));
        }};
        return new ModelAndView(object, "new_dashboard/index.ftlh");
    }

    private boolean showAnalyticsIcon() {
        return systemEnvironment.enableAnalyticsOnlyForAdmins() ? securityService.isUserAdmin(currentUsername()) : true;
    }
}

