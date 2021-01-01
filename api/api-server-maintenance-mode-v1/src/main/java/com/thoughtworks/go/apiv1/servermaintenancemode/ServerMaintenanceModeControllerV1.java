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
package com.thoughtworks.go.apiv1.servermaintenancemode;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.servermaintenancemode.representers.MaintenanceModeInfoRepresenter;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.server.dashboard.GoDashboardCache;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.ServerMaintenanceMode;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;

import static spark.Spark.*;

@Component
public class ServerMaintenanceModeControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private GoDashboardCache dashboardCache;
    private AgentService agentService;
    private final MaintenanceModeService maintenanceModeService;
    private Clock clock;

    @Autowired
    public ServerMaintenanceModeControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                             GoDashboardCache dashboardCache,
                                             AgentService agentService,
                                             MaintenanceModeService maintenanceModeService,
                                             Clock clock) {
        super(ApiVersion.v1);

        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.dashboardCache = dashboardCache;
        this.agentService = agentService;
        this.maintenanceModeService = maintenanceModeService;
        this.clock = clock;
    }

    @Override
    public String controllerBasePath() {
        return Routes.MaintenanceMode.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", mimeType, this::verifyContentType);
            before("/*", mimeType, this::verifyContentType);

            before(Routes.MaintenanceMode.ENABLE, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before(Routes.MaintenanceMode.DISABLE, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            before(Routes.MaintenanceMode.INFO, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            post(Routes.MaintenanceMode.ENABLE, mimeType, this::enableMaintenanceModeState);
            post(Routes.MaintenanceMode.DISABLE, mimeType, this::disableMaintenanceModeState);

            get(Routes.MaintenanceMode.INFO, mimeType, this::getMaintenanceModeInfo);
        });
    }

    public String enableMaintenanceModeState(Request req, Response res) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerMaintenanceMode existingMaintenanceModeState = maintenanceModeService.get();
        if (existingMaintenanceModeState.isMaintenanceMode()) {
            result.conflict("Failed to enable server maintenance mode. Server is already in maintenance mode.");
            return renderHTTPOperationResult(result, req, res);
        }

        maintenanceModeService.update(new ServerMaintenanceMode(true, currentUsernameString(), clock.currentTime()));

        res.status(204);
        return NOTHING;
    }

    public String disableMaintenanceModeState(Request req, Response res) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerMaintenanceMode existingMaintenanceModeState = maintenanceModeService.get();
        if (!existingMaintenanceModeState.isMaintenanceMode()) {
            result.conflict("Failed to disable server maintenance mode. Server is not in maintenance mode.");
            return renderHTTPOperationResult(result, req, res);
        }

        maintenanceModeService.update(new ServerMaintenanceMode(false, currentUsernameString(), clock.currentTime()));

        res.status(204);
        return NOTHING;
    }

    public String getMaintenanceModeInfo(Request req, Response res) throws IOException {
        ServerMaintenanceMode serverMaintenanceMode = maintenanceModeService.get();

        if (serverMaintenanceMode.isMaintenanceMode()) {
            Collection<MaintenanceModeService.MaterialPerformingMDU> runningMDUs = maintenanceModeService.getRunningMDUs();
            List<ArrayList<JobInstance>> allRunningJobs = getRunningJobs();
            ArrayList<JobInstance> buildingJobs = allRunningJobs.get(0);
            ArrayList<JobInstance> scheduledJobs = allRunningJobs.get(1);

            boolean hasNoRunningSystems = runningMDUs.isEmpty() && buildingJobs.isEmpty();
            return writerForTopLevelObject(req, res, writer -> {
                MaintenanceModeInfoRepresenter.toJSON(writer, serverMaintenanceMode, hasNoRunningSystems, runningMDUs, buildingJobs, scheduledJobs);
            });
        } else {
            return writerForTopLevelObject(req, res, writer -> {
                MaintenanceModeInfoRepresenter.toJSON(writer, serverMaintenanceMode, false, null, null, null);
            });
        }
    }

    private List<ArrayList<JobInstance>> getRunningJobs() {
        Collection<GoDashboardPipeline> pipelines = dashboardCache.allEntries().getPipelines();
        HashMap<String, String> buildToAgentUUIDMap = getBuildLocatorToAgentUUIDMap();

        ArrayList<JobInstance> buildingJobs = new ArrayList<>();
        ArrayList<JobInstance> scheduledJobs = new ArrayList<>();

        for (GoDashboardPipeline pipeline : pipelines) {
            for (PipelineInstanceModel pipelineInstance : pipeline.model().getActivePipelineInstances()) {
                String pipelineName = pipelineInstance.getName();
                int pipelineCounter = pipelineInstance.getCounter();
                String pipelineLabel = pipelineInstance.getLabel();

                if (!pipelineInstance.isAnyStageActive()) {
                    continue;
                }

                StageInstanceModel runningStage = pipelineInstance.activeStage();
                for (JobHistoryItem job : runningStage.getBuildHistory()) {
                    String stageName = runningStage.getName();
                    String stageCounter = runningStage.getCounter();
                    if (job.isRunning()) {
                        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, pipelineCounter, pipelineLabel, stageName, stageCounter, job.getName());
                        String agentUUID = buildToAgentUUIDMap.get(jobIdentifier.buildLocator());
                        if (agentUUID != null) {
                            buildingJobs.add(createJobInstance(job, jobIdentifier, agentUUID));
                        } else {
                            scheduledJobs.add(createJobInstance(job, jobIdentifier, null));
                        }
                    }
                }
            }
        }

        return Arrays.asList(buildingJobs, scheduledJobs);
    }

    private HashMap<String, String> getBuildLocatorToAgentUUIDMap() {
        AgentInstances agentInstances = agentService.getAgentInstances();

        HashMap<String, String> buildToAgentUUIDMap = new HashMap<>();
        for (AgentInstance agentInstance : agentInstances) {
            if (agentInstance.isBuilding()) {
                buildToAgentUUIDMap.put(agentInstance.getBuildLocator(), agentInstance.getUuid());
            }
        }
        return buildToAgentUUIDMap;
    }

    private JobInstance createJobInstance(JobHistoryItem job, JobIdentifier jobIdentifier, String agentUUID) {
        JobInstance jobInstance = new JobInstance(job.getName());

        jobInstance.setIdentifier(jobIdentifier);
        jobInstance.setState(job.getState());
        jobInstance.setScheduledDate(job.getScheduledDate());
        jobInstance.setAgentUuid(agentUUID);

        return jobInstance;
    }
}
