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

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.service.ElasticAgentPluginService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class StatusReportsController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private ElasticAgentPluginService elasticAgentPluginService;
    private JobInstanceService jobInstanceService;
    public static final String UNKNOWN_ERROR_MESSAGE = "Something went wrong while trying to fetch the Status Report. Please check the server and plugin logs for more details.";
    private static Logger LOGGER = LoggerFactory.getLogger(StatusReportsController.class);

    public StatusReportsController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine,
                                   ElasticAgentPluginService elasticAgentPluginService, JobInstanceService jobInstanceService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.elasticAgentPluginService = elasticAgentPluginService;
        this.jobInstanceService = jobInstanceService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.StatusReports.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkAdminUserAnd403);
            before("/*", authenticationHelper::checkAdminUserAnd403);
            get("/:plugin_id", this::pluginStatusReport, engine);
            get("/:plugin_id/agent/:elastic_agent_id", this::agentStatusReport, engine);
            get("/:plugin_id/cluster/:cluster_profile_id", this::clusterStatusReport, engine);
        });
    }

    public ModelAndView pluginStatusReport(Request request, Response response) {
        String pluginId = request.params("plugin_id");
        try {
            String pluginStatusReport = elasticAgentPluginService.getPluginStatusReport(pluginId);
            Map<Object, Object> object = new HashMap<>() {{
                put("viewTitle", "Plugin Status Report");
                put("viewFromPlugin", pluginStatusReport);
            }};
            return new ModelAndView(object, "status_reports/index.ftlh");
        } catch (RecordNotFoundException e) {
            return errorPage(response, 404, "Plugin Status Report", e.getMessage());
        } catch (UnsupportedOperationException e) {
            String message = String.format("Status Report for plugin with id: '%s' is not found.", pluginId);
            return errorPage(response, 404, "Plugin Status Report", message);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return errorPage(response, 500, "Plugin Status Report", UNKNOWN_ERROR_MESSAGE);
        }
    }

    public ModelAndView agentStatusReport(Request request, Response response) throws Exception {
        String pluginId = request.params("plugin_id");
        String elasticAgentId = parseElasticAgentId(request);
        String jobIdString = request.queryParams("job_id");
        long jobId;
        try {
            jobId = Long.parseLong(jobIdString);
        } catch (NumberFormatException e) {
            return errorPage(response, HttpStatus.UNPROCESSABLE_ENTITY.value(), "Agent Status Report",
                    "Please provide a valid job_id for Agent Status Report.");
        }
        try {
            JobInstance jobInstance = jobInstanceService.buildById(jobId);
            String agentStatusReport = elasticAgentPluginService.getAgentStatusReport(pluginId, jobInstance.getIdentifier(), elasticAgentId);
            Map<Object, Object> object = new HashMap<>();
            object.put("viewTitle", "Agent Status Report");
            object.put("viewFromPlugin", agentStatusReport);
            return new ModelAndView(object, "status_reports/index.ftlh");
        } catch (RecordNotFoundException e) {
            return errorPage(response, 404, "Agent Status Report", e.getMessage());
        } catch (DataRetrievalFailureException | UnsupportedOperationException e) {
            String message = String.format("Status Report for plugin with id: '%s' for agent '%s' is not found.", pluginId, elasticAgentId);
            return errorPage(response, 404, "Agent Status Report", message);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return errorPage(response, 500, "Agent Status Report", UNKNOWN_ERROR_MESSAGE);
        }
    }

    public ModelAndView clusterStatusReport(Request request, Response response) {
        String pluginId = request.params("plugin_id");
        String clusterId = request.params("cluster_profile_id");
        try {
            String clusterStatusReport = elasticAgentPluginService.getClusterStatusReport(pluginId, clusterId);
            Map<Object, Object> object = new HashMap<>() {{
                put("viewTitle", "Cluster Status Report");
                put("viewFromPlugin", clusterStatusReport);
            }};
            return new ModelAndView(object, "status_reports/index.ftlh");
        } catch (RecordNotFoundException e) {
            return errorPage(response, 404, "Cluster Status Report", e.getMessage());
        } catch (DataRetrievalFailureException | UnsupportedOperationException e) {
            String message = String.format("Status Report for plugin with id: '%s' for cluster '%s' is not found.", pluginId, clusterId);
            return errorPage(response, 404, "Cluster Status Report", message);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return errorPage(response, 500, "Agent Status Report", UNKNOWN_ERROR_MESSAGE);
        }
    }

    private ModelAndView errorPage(Response response, int statusCode, String viewTitle, String message) {
        response.status(statusCode);
        return new ModelAndView(Map.of("viewTitle", viewTitle, "message", message), "status_reports/error.ftlh");
    }

    private String parseElasticAgentId(Request request) {
        String elasticAgentId = request.params("elastic_agent_id");
        if ("unassigned".equalsIgnoreCase(elasticAgentId)) {
            return null;
        }
        return elasticAgentId;
    }
}
