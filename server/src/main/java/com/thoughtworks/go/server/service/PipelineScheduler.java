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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.scheduling.*;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthServiceUpdatingOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class PipelineScheduler implements ConfigChangedListener, GoMessageListener<ScheduleCheckCompletedMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineScheduler.class);

    private GoConfigService goConfigService;
    private ServerHealthService serverHealthService;
    private SchedulingCheckerService schedulingChecker;
    private BuildCauseProducerService buildCauseProducerService;
    private ScheduleCheckQueue scheduleCheckQueue;
    private ScheduleCheckCompletedTopic scheduleCheckCompletedTopic;
    private SchedulingPerformanceLogger schedulingPerformanceLogger;
    private final Map<String, ScheduleCheckState> pipelines = new HashMap<>();

    protected PipelineScheduler() {
    }

    @Autowired
    PipelineScheduler(GoConfigService goConfigService,
                      ServerHealthService serverHealthService,
                      SchedulingCheckerService schedulingChecker,
                      BuildCauseProducerService buildCauseProducerService,
                      ScheduleCheckQueue scheduleCheckQueue,
                      ScheduleCheckCompletedTopic scheduleCheckCompletedTopic,
                      SchedulingPerformanceLogger schedulingPerformanceLogger) {
        this.goConfigService = goConfigService;
        this.serverHealthService = serverHealthService;
        this.schedulingChecker = schedulingChecker;
        this.buildCauseProducerService = buildCauseProducerService;
        this.scheduleCheckQueue = scheduleCheckQueue;
        this.scheduleCheckCompletedTopic = scheduleCheckCompletedTopic;
        this.schedulingPerformanceLogger = schedulingPerformanceLogger;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
        goConfigService.register(configRepoConfigChangedListener());
        scheduleCheckCompletedTopic.addListener(this);
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                synchronized (pipelines) {
                    addPipelineIfNotPresent(pipelineConfig, pipelines);
                    if (!goConfigService.getAllPipelineConfigs().contains(pipelineConfig)) {
                        pipelines.remove(pipelineConfig.name().toString());
                    }
                }
            }
        };
    }

    protected EntityConfigChangedListener<ConfigRepoConfig> configRepoConfigChangedListener() {
        final Consumer<CruiseConfig> configChangeConsumer = this::onConfigChange;
        return new EntityConfigChangedListener<ConfigRepoConfig>() {
            @Override
            public void onEntityConfigChange(ConfigRepoConfig configRepoConfig) {
                configChangeConsumer.accept(goConfigService.cruiseConfig());
            }
        };
    }

    //NOTE: This is called on a thread by Spring
    public void onTimer() {
        autoProduceBuildCauseAndSave();
    }

    private void autoProduceBuildCauseAndSave() {
        try {
            OperationResult result = new ServerHealthServiceUpdatingOperationResult(serverHealthService);
            if (!schedulingChecker.canSchedule(result)) {
                return;
            }

            removeLicenseInvalidFromLog();
            checkPipelines();
        } catch (Exception e) {
            LOGGER.error("Error autoScheduling pipelines", e);
        }

    }

    void checkPipelines() {
        synchronized (pipelines) {
            for (Map.Entry<String, ScheduleCheckState> entry : pipelines.entrySet()) {
                if (entry.getValue().equals(ScheduleCheckState.IDLE)) {
                    long trackingId = schedulingPerformanceLogger.pipelineSentToScheduleCheckQueue(entry.getKey());

                    scheduleCheckQueue.post(new ScheduleCheckMessage(entry.getKey(), trackingId));
                    pipelines.put(entry.getKey(), ScheduleCheckState.BUSY);

                    LOGGER.trace("try to schedule pipeline {}, current pipeline state: {}", entry.getKey(), pipelines);
                } else {
                    LOGGER.trace("skipping scheduling pipeline {} because it's busy scheduling, current pipelines state: {}", entry.getKey(), pipelines);
                }
            }
        }
    }

    public void manualProduceBuildCauseAndSave(String pipelineName, Username username, ScheduleOptions scheduleOptions, OperationResult result) {
        LOGGER.info("[Pipeline Schedule] [Requested] Manual trigger of pipeline '{}' requested by {}", pipelineName, CaseInsensitiveString.str(username.getUsername()));
        if (pipelineNotFound(pipelineName, result)) {
            return;
        }
        if (materialNotFound(pipelineName, scheduleOptions.getSpecifiedRevisions(), result)) {
            return;
        }
        if (revisionInvalid(scheduleOptions.getSpecifiedRevisions(), result)) {
            return;
        }
        if (hasUsedUnconfiguredVariable(pipelineName, scheduleOptions.getVariables(), result)) {
            return;
        }

        LOGGER.info("[Pipeline Schedule] [Accepted] Manual trigger of pipeline '{}' accepted for user {}", pipelineName, CaseInsensitiveString.str(username.getUsername()));
        removeLicenseInvalidFromLog();
        buildCauseProducerService.manualSchedulePipeline(username, new CaseInsensitiveString(pipelineName), scheduleOptions, result);
        LOGGER.info("[Pipeline Schedule] [Processed] Manual trigger of pipeline '{}' processed with result '{}'", pipelineName, result.getServerHealthState());
    }

    private boolean hasUsedUnconfiguredVariable(String pipelineName, EnvironmentVariablesConfig environmentVariables, OperationResult result) {
        for (EnvironmentVariableConfig variable : environmentVariables) {
            if (!goConfigService.hasVariableInScope(pipelineName, variable.getName())) {
                String variableUnconfiguredMessage = String.format("Variable '%s' has not been configured for pipeline '%s'", variable.getName(), pipelineName);
                result.notFound(variableUnconfiguredMessage, variableUnconfiguredMessage, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
                return true;
            }
        }
        return false;
    }

    private boolean revisionInvalid(Map<String, String> revisions, OperationResult result) {
        for (Map.Entry<String, String> entry : revisions.entrySet()) {
            if (StringUtils.isEmpty(entry.getValue())) {
                String message = String.format("material with fingerprint [%s] has empty revision", entry.getKey());
                result.notAcceptable(message, HealthStateType.general(HealthStateScope.GLOBAL));
                return true;
            }
        }
        return false;
    }

    private boolean materialNotFound(String pipelineName, Map<String, String> revisions, OperationResult result) {
        for (String pipelineFingerprint : revisions.keySet()) {
            if (goConfigService.findMaterial(new CaseInsensitiveString(pipelineName), pipelineFingerprint) == null) {
                String message = String.format("material with fingerprint [%s] not found in pipeline [%s]", pipelineFingerprint, pipelineName);
                result.notFound(message, message, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
                return true;
            }
        }
        return false;
    }

    private boolean pipelineNotFound(String pipelineName, OperationResult result) {
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound(String.format("Pipeline '%s' not found", pipelineName),
                    String.format("Pipeline '%s' not found", pipelineName),
                    HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return true;
        }
        return false;
    }

    private void removeLicenseInvalidFromLog() {
        serverHealthService.update(ServerHealthState.success(
                HealthStateType.invalidLicense(HealthStateScope.GLOBAL)));
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        synchronized (pipelines) {
            newCruiseConfig.accept((PiplineConfigVisitor) pipelineConfig -> addPipelineIfNotPresent(pipelineConfig, pipelines));

            List<String> deletedPipeline = new ArrayList<>();
            for (String pipelineName : pipelines.keySet()) {
                if (!newCruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
                    deletedPipeline.add(pipelineName);
                }
            }

            for (String pipelineName : deletedPipeline) {
                pipelines.remove(pipelineName);
            }
        }
    }

    private void addPipelineIfNotPresent(PipelineConfig pipelineConfig, Map<String, ScheduleCheckState> pipelines) {
        if (!pipelines.containsKey(CaseInsensitiveString.str(pipelineConfig.name()))) {
            pipelines.put(CaseInsensitiveString.str(pipelineConfig.name()), ScheduleCheckState.IDLE);
            LOGGER.debug("[Configuration Changed] Marking new pipeline {} as IDLE", pipelineConfig.name());
        }
    }

    @Override
    public void onMessage(ScheduleCheckCompletedMessage message) {
        synchronized (pipelines) {
            pipelines.put(message.getPipelineName(), ScheduleCheckState.IDLE);

            schedulingPerformanceLogger.completionMessageForScheduleCheckReceived(message.trackingId(), message.getPipelineName());
            LOGGER.trace("marked pipeline {} as IDLE, current pipelines state: {}", message.getPipelineName(), pipelines);
        }
    }
}
