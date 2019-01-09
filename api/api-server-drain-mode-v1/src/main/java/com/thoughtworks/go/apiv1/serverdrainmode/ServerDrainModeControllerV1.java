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

package com.thoughtworks.go.apiv1.serverdrainmode;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.apiv1.serverdrainmode.representers.DrainModeInfoRepresenter;
import com.thoughtworks.go.config.InvalidPluginTypeException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.server.dashboard.GoDashboardCache;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.ServerDrainMode;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.DrainModeService;
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
public class ServerDrainModeControllerV1 extends ApiController implements SparkSpringController {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private GoDashboardCache dashboardCache;
    private AgentService agentService;
    private final DrainModeService drainModeService;
    private Clock clock;

    @Autowired
    public ServerDrainModeControllerV1(ApiAuthenticationHelper apiAuthenticationHelper,
                                       GoDashboardCache dashboardCache,
                                       AgentService agentService,
                                       DrainModeService drainModeService,
                                       Clock clock) {
        super(ApiVersion.v1);

        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.dashboardCache = dashboardCache;
        this.agentService = agentService;
        this.drainModeService = drainModeService;
        this.clock = clock;
    }

    @Override
    public String controllerBasePath() {
        return Routes.DrainMode.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);
            before("", this::verifyContentType);
            before("/*", this::verifyContentType);

            before(Routes.DrainMode.ENABLE, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);
            before(Routes.DrainMode.DISABLE, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            before(Routes.DrainMode.INFO, mimeType, apiAuthenticationHelper::checkAdminUserAnd403);

            post(Routes.DrainMode.ENABLE, mimeType, this::enableDrainModeState);
            post(Routes.DrainMode.DISABLE, mimeType, this::disableDrainModeState);

            get(Routes.DrainMode.INFO, mimeType, this::getDrainModeInfo);

            exception(RecordNotFoundException.class, this::notFound);
        });
    }

    public String enableDrainModeState(Request req, Response res) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerDrainMode existingDrainModeState = drainModeService.get();
        if (existingDrainModeState.isDrainMode()) {
            result.conflict("Failed to enable server drain mode. Server is already in drain state.");
            return renderHTTPOperationResult(result, req, res);
        }

        drainModeService.update(new ServerDrainMode(true, currentUserLoginName().toString(), clock.currentTime()));

        res.status(204);
        return NOTHING;
    }

    public String disableDrainModeState(Request req, Response res) throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerDrainMode existingDrainModeState = drainModeService.get();
        if (!existingDrainModeState.isDrainMode()) {
            result.conflict("Failed to disable server drain mode. Server is not in drain state.");
            return renderHTTPOperationResult(result, req, res);
        }

        drainModeService.update(new ServerDrainMode(false, currentUserLoginName().toString(), clock.currentTime()));

        res.status(204);
        return NOTHING;
    }

    public String getDrainModeInfo(Request req, Response res) throws InvalidPluginTypeException, IOException {
        ServerDrainMode serverDrainMode = drainModeService.get();

        if (serverDrainMode.isDrainMode()) {
            Collection<DrainModeService.MaterialPerformingMDU> runningMDUs = drainModeService.getRunningMDUs();
            List<ArrayList<JobInstance>> allRunningJobs = getRunningJobs();
            ArrayList<JobInstance> buildingJobs = allRunningJobs.get(0);
            ArrayList<JobInstance> scheduledJobs = allRunningJobs.get(1);

            boolean isServerCompletelyDrained = runningMDUs.isEmpty() && buildingJobs.isEmpty();
            return writerForTopLevelObject(req, res, writer -> {
                DrainModeInfoRepresenter.toJSON(writer, serverDrainMode, isServerCompletelyDrained, runningMDUs, buildingJobs, scheduledJobs);
            });
        } else {
            return writerForTopLevelObject(req, res, writer -> {
                DrainModeInfoRepresenter.toJSON(writer, serverDrainMode, false, null, null, null);
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
        AgentInstances agents = agentService.agents();

        HashMap<String, String> buildToAgentUUIDMap = new HashMap<>();
        for (AgentInstance agent : agents) {
            if (agent.isBuilding()) {
                buildToAgentUUIDMap.put(agent.getBuildLocator(), agent.getUuid());
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
