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

import com.google.gson.Gson;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsExtension;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsData;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.spark.Routes.AnalyticsSPA.SHOW_PATH;
import static java.lang.String.format;
import static spark.Spark.*;

public class AnalyticsController implements SparkController {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AnalyticsController.class);
    private static final Gson GSON = new Gson();

    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private final SystemEnvironment systemEnvironment;
    private final AnalyticsExtension analyticsExtension;
    private final PipelineConfigService pipelineConfigService;

    public AnalyticsController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, SystemEnvironment systemEnvironment, AnalyticsExtension analyticsExtension, PipelineConfigService pipelineConfigService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.systemEnvironment = systemEnvironment;
        this.analyticsExtension = analyticsExtension;
        this.pipelineConfigService = pipelineConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.AnalyticsSPA.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkAdminUserAnd403);
            get("", this::index, engine);
        });

        path(controllerPath(SHOW_PATH), () -> {
            before("", this::checkPipelineExists, this::checkPermissions);
            before("/*", this::checkPipelineExists, this::checkPermissions);
            get("", this::showAnalytics);
            post("", this::showAnalytics);
        });
    }

    public ModelAndView index(Request request, Response response) {
        HashMap<String, String> locals = new HashMap<String, String>() {{
            List<String> pipelines = new ArrayList<>();
            pipelineConfigService.viewableGroupsFor(SessionUtils.currentUsername()).forEach(
                    (PipelineConfigs config) -> config.getPipelines().forEach(
                            (p) -> pipelines.add(p.name().toString())
                    )
            );
            put("viewTitle", "Analytics");
            put("pipelines", GSON.toJson(pipelines));
        }};
        return new ModelAndView(locals, "analytics/index.ftlh");
    }

    public String showAnalytics(Request request, Response response) {
        try {
            final AnalyticsData analytics = analyticsExtension.getAnalytics(
                    request.params(":plugin_id"),
                    request.params(":type"),
                    request.params(":id"),
                    getQueryParams(request));

            response.type("application/json");
            return GSON.toJson(analytics.toMap());
        } catch (Exception e) {
            LOG.error("Encountered error while fetching analytics", e);
            throw halt(500, format("Error generating analytics from plugin - %s", request.params(":plugin_id")));
        }
    }

    private void checkPipelineExists(Request request, Response response) {
        if (isPipelineRequest(request)) {
            if (null == pipelineConfigService.pipelineConfigNamed(request.queryParams("pipeline_name"))) {
                throw halt(404, format("Cannot generate analytics. Pipeline with name: '%s' not found.", request.queryParams("pipeline_name")));
            }
        }
    }

    private void checkPermissions(Request request, Response response) {
        if (isAnalyticsEnabledOnlyForAdmins()) {
            authenticationHelper.checkAdminUserAnd403(request, response);
            return;
        }

        if (isPipelineRequest(request)) {
            authenticationHelper.checkPipelineViewPermissionsAnd403(request, response);
        } else if (isDashboardRequest(request)) {
            authenticationHelper.checkAdminUserAnd403(request, response);
        } else {
            authenticationHelper.checkUserAnd403(request, response);
        }
    }

    private boolean isPipelineRequest(Request request) {
        return "pipeline".equals(request.params(":type"));
    }

    private boolean isDashboardRequest(Request request) {
        return "dashboard".equalsIgnoreCase(request.params(":type"));
    }

    private boolean isAnalyticsEnabledOnlyForAdmins() {
        return systemEnvironment.enableAnalyticsOnlyForAdmins();
    }

    /**
     * Gets query parameters from request, ignoring duplicates. This
     * is different from Spark's queryMap().toMap()
     *
     * @param request the spark request
     * @return the params as a {@link Map<String, String>}
     */
    private Map<String, String> getQueryParams(Request request) {
        final Map<String, String> queryParams = new HashMap<>();
        request.queryMap().toMap().forEach((k, v) -> {
            queryParams.put(k, v[0]);
        });
        return queryParams;
    }
}
