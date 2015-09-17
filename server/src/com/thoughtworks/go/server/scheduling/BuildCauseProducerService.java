/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.scheduling;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialChecker;
import com.thoughtworks.go.server.materials.MaterialUpdateCompletedMessage;
import com.thoughtworks.go.server.materials.MaterialUpdateFailedMessage;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.materials.MaterialUpdateStatusListener;
import com.thoughtworks.go.server.materials.MaterialUpdateStatusNotifier;
import com.thoughtworks.go.server.materials.SpecificMaterialRevisionFactory;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.AutoBuild;
import com.thoughtworks.go.server.service.BuildType;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.ManualBuild;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.service.NoCompatibleUpstreamRevisionsException;
import com.thoughtworks.go.server.service.NoModificationsPresentForDependentMaterialException;
import com.thoughtworks.go.server.service.PipelineScheduleQueue;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.SchedulingCheckerService;
import com.thoughtworks.go.server.service.TimedBuild;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
public class BuildCauseProducerService {
    private static final Logger LOGGER = Logger.getLogger(BuildCauseProducerService.class);

    private SchedulingCheckerService schedulingChecker;
    private ServerHealthService serverHealthService;
    private PipelineScheduleQueue pipelineScheduleQueue;
    private GoConfigService goConfigService;
    private MaterialChecker materialChecker;
    private MaterialUpdateStatusNotifier materialUpdateStatusNotifier;
    private final MaterialUpdateService materialUpdateService;
    private final SpecificMaterialRevisionFactory specificMaterialRevisionFactory;
    private final PipelineService pipelineService;

    private TriggerMonitor triggerMonitor;
    private final SystemEnvironment systemEnvironment;
    private final MaterialConfigConverter materialConfigConverter;
    private final MaterialExpansionService materialExpansionService;
    private SchedulingPerformanceLogger schedulingPerformanceLogger;

    @Autowired
    public BuildCauseProducerService(
            SchedulingCheckerService schedulingChecker,
            ServerHealthService serverHealthService,
            PipelineScheduleQueue pipelineScheduleQueue,
            GoConfigService goConfigService,
            MaterialRepository materialRepository,
            MaterialUpdateStatusNotifier materialUpdateStatusNotifier,
            MaterialUpdateService materialUpdateService,
            SpecificMaterialRevisionFactory specificMaterialRevisionFactory,
            TriggerMonitor triggerMonitor,
            PipelineService pipelineService,
            SystemEnvironment systemEnvironment,
            MaterialConfigConverter materialConfigConverter,
            MaterialExpansionService materialExpansionService,
            SchedulingPerformanceLogger schedulingPerformanceLogger) {
        this.schedulingChecker = schedulingChecker;
        this.serverHealthService = serverHealthService;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
        this.goConfigService = goConfigService;
        this.materialUpdateStatusNotifier = materialUpdateStatusNotifier;
        this.materialUpdateService = materialUpdateService;
        this.specificMaterialRevisionFactory = specificMaterialRevisionFactory;
        this.pipelineService = pipelineService;
        this.systemEnvironment = systemEnvironment;
        this.materialConfigConverter = materialConfigConverter;
        this.materialExpansionService = materialExpansionService;
        this.schedulingPerformanceLogger = schedulingPerformanceLogger;
        this.materialChecker = new MaterialChecker(materialRepository);
        this.triggerMonitor = triggerMonitor;
    }

    public void autoSchedulePipeline(String pipelineName, ServerHealthStateOperationResult result, long trackingId) {
        schedulingPerformanceLogger.autoSchedulePipelineStart(trackingId, pipelineName);

        try {
            PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
            newProduceBuildCause(pipelineConfig, new AutoBuild(goConfigService, pipelineService, pipelineName, systemEnvironment, materialChecker, serverHealthService), result, trackingId);
        } finally {
            schedulingPerformanceLogger.autoSchedulePipelineFinish(trackingId, pipelineName);
        }
    }

    public void manualSchedulePipeline(Username username, PipelineConfig pipelineConfig, ScheduleOptions scheduleOptions, OperationResult result) {
        long trackingId = schedulingPerformanceLogger.manualSchedulePipelineStart(CaseInsensitiveString.str(pipelineConfig.name()));

        try {
            WaitForPipelineMaterialUpdate update = new WaitForPipelineMaterialUpdate(pipelineConfig, new ManualBuild(username), scheduleOptions);
            update.start(result);
        } finally {
            schedulingPerformanceLogger.manualSchedulePipelineFinish(trackingId, CaseInsensitiveString.str(pipelineConfig.name()));
        }
    }

    public void timerSchedulePipeline(PipelineConfig pipelineConfig, ServerHealthStateOperationResult result) {
        long trackingId = schedulingPerformanceLogger.timerSchedulePipelineStart(CaseInsensitiveString.str(pipelineConfig.name()));

        try {
            newProduceBuildCause(pipelineConfig, new TimedBuild(), result, trackingId);
        } finally {
            schedulingPerformanceLogger.timerSchedulePipelineFinish(trackingId, CaseInsensitiveString.str(pipelineConfig.name()));
        }
    }

    boolean markPipelineAsAlreadyTriggered(PipelineConfig pipelineConfig) {
        return triggerMonitor.markPipelineAsAlreadyTriggered(pipelineConfig);
    }

    void markPipelineAsCanBeTriggered(PipelineConfig pipelineConfig) {
        triggerMonitor.markPipelineAsCanBeTriggered(pipelineConfig);
    }

    ServerHealthState newProduceBuildCause(PipelineConfig pipelineConfig, BuildType buildType, ServerHealthStateOperationResult result, long trackingId) {
        final HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        final HashMap<String, String> stringStringHashMap1 = new HashMap<String, String>();
        return newProduceBuildCause(pipelineConfig, buildType, new ScheduleOptions(stringStringHashMap, stringStringHashMap1, new HashMap<String, String>()), result, trackingId);
    }

    ServerHealthState newProduceBuildCause(PipelineConfig pipelineConfig, BuildType buildType, ScheduleOptions scheduleOptions, ServerHealthStateOperationResult result, long trackingId) {
        buildType.canProduce(pipelineConfig, schedulingChecker, serverHealthService, result);
        if (!result.canContinue()) {
            return result.getServerHealthState();
        }
        String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("start producing build cause:" + pipelineName);
        }

        try {
            MaterialRevisions peggedRevisions = specificMaterialRevisionFactory.create(pipelineName, scheduleOptions.getSpecifiedRevisions());
            BuildCause previousBuild = pipelineScheduleQueue.mostRecentScheduled(pipelineName);

            Materials materials = materialConfigConverter.toMaterials(pipelineConfig.materialConfigs());
            MaterialConfigs expandedMaterialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());
            Materials expandedMaterials = materialConfigConverter.toMaterials(expandedMaterialConfigs);
            BuildCause buildCause = null;
            boolean materialConfigurationChanged = hasConfigChanged(previousBuild, expandedMaterials);
            if (previousBuild.hasNeverRun() || materialConfigurationChanged) {
                LOGGER.debug("Using latest modifications from respository for " + pipelineConfig.name());
                MaterialRevisions revisions = materialChecker.findLatestRevisions(peggedRevisions, materials);
                if (!revisions.isMissingModifications()) {
                    buildCause = buildType.onModifications(revisions, materialConfigurationChanged, null);
                    if (buildCause != null) {
                        if (!buildCause.materialsMatch(expandedMaterialConfigs)) {
                            LOGGER.warn("Error while scheduling pipeline: " + pipelineName + ". Possible Reasons: (1) Upstream pipelines have not been built yet."
                                    + " (2) Materials do not match between configuration and build-cause.");
                            return ServerHealthState.success(HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
                        }
                    }
                }
            } else {
                LOGGER.debug("Checking if materials are different for " + pipelineConfig.name());
                MaterialRevisions latestRevisions = materialChecker.findLatestRevisions(peggedRevisions, materials);
                if (!latestRevisions.isMissingModifications()) {
                    MaterialRevisions original = previousBuild.getMaterialRevisions();
                    MaterialRevisions revisions = materialChecker.findRevisionsSince(peggedRevisions, expandedMaterials, original, latestRevisions);
                    if (!revisions.hasChangedSince(original) || (buildType.shouldCheckWhetherOlderRunsHaveRunWithLatestMaterials() && materialChecker.hasPipelineEverRunWith(pipelineName, latestRevisions))) {
                        LOGGER.debug("Repository for [" + pipelineName + "] not modified");
                        buildCause = buildType.onEmptyModifications(pipelineConfig, latestRevisions);
                    } else {
                        LOGGER.debug("Repository for [" + pipelineName + "] modified; scheduling...");
                        buildCause = buildType.onModifications(revisions, materialConfigurationChanged, original);
                    }
                }
            }
            if (buildCause != null) {
                buildCause.addOverriddenVariables(scheduleOptions.getVariables());
                updateChangedRevisions(pipelineConfig.name(), buildCause);
                if (materialConfigurationChanged || buildType.isValidBuildCause(pipelineConfig, buildCause)) {
                    pipelineScheduleQueue.schedule(pipelineName, buildCause);

                    schedulingPerformanceLogger.sendingPipelineToTheToBeScheduledQueue(trackingId, pipelineName);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(format("scheduling pipeline %s with build-cause %s", pipelineName, buildCause));
                    }
                } else {
                    buildType.notifyPipelineNotScheduled(pipelineConfig);
                }
            } else {
                buildType.notifyPipelineNotScheduled(pipelineConfig);
            }

            serverHealthService.removeByScope(HealthStateScope.forPipeline(pipelineName));
            LOGGER.debug("finished producing buildcause for " + pipelineName);
            return ServerHealthState.success(HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
        } catch (NoCompatibleUpstreamRevisionsException ncure) {
            String message = "Error while scheduling pipeline: " + pipelineName + " as no compatible revisions were identified.";
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(message, ncure);
            }
            return showError(pipelineName, message, ncure.getMessage());
        } catch (NoModificationsPresentForDependentMaterialException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error(e.getMessage(), e);
            }
            return ServerHealthState.success(HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
        } catch (Exception e) {
            String message = "Error while scheduling pipeline: " + pipelineName;
            LOGGER.error(message, e);
            return showError(pipelineName, message, e.getMessage());
        }
    }

    private void updateChangedRevisions(CaseInsensitiveString pipelineName, BuildCause buildCause) {
        materialChecker.updateChangedRevisions(pipelineName, buildCause);
    }

    private ServerHealthState showError(String pipelineName, String message, String desc) {
        if (desc == null) {
            desc = "Details not available, please check server logs.";
        }
        ServerHealthState serverHealthState = ServerHealthState.error(message, desc, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
        serverHealthService.update(serverHealthState);
        return serverHealthState;
    }

    private boolean hasConfigChanged(BuildCause previous, Materials materials) {
        return !materials.equals(previous.getMaterialRevisions().getMaterials());
    }

    private class WaitForPipelineMaterialUpdate implements MaterialUpdateStatusListener {
        private final PipelineConfig pipelineConfig;
        private final BuildType buildType;

        private final ConcurrentMap<String, Material> pendingMaterials;
        private boolean failed;
        private ScheduleOptions scheduleOptions;

        private WaitForPipelineMaterialUpdate(PipelineConfig pipelineConfig, BuildType buildType, ScheduleOptions scheduleOptions) {
            this.pipelineConfig = pipelineConfig;
            this.buildType = buildType;
            this.scheduleOptions = scheduleOptions;
            pendingMaterials = new ConcurrentHashMap<String, Material>();
            for (MaterialConfig materialConfig : pipelineConfig.materialConfigs()) {
                pendingMaterials.put(materialConfig.getFingerprint(), materialConfigConverter.toMaterial(materialConfig));
            }
        }

        public void start(OperationResult result) {
            try {
                buildType.canProduce(pipelineConfig, schedulingChecker, serverHealthService, result);
                if (!result.canContinue()) {
                    return;
                }
                if (!markPipelineAsAlreadyTriggered(pipelineConfig)) {
                    result.conflict("Failed to force pipeline: " + pipelineConfig.name(),
                            "Pipeline already forced",
                            HealthStateType.general(HealthStateScope.forPipeline(CaseInsensitiveString.str(pipelineConfig.name()))));
                    return;
                }
                materialUpdateStatusNotifier.registerListenerFor(pipelineConfig, this);
                for (Material material : pendingMaterials.values()) {
                    materialUpdateService.updateMaterial(material);
                }
                result.accepted(format("Request to schedule pipeline %s accepted", pipelineConfig.name()), "", HealthStateType.general(HealthStateScope.forPipeline(
                        CaseInsensitiveString.str(pipelineConfig.name()))));
            } catch (RuntimeException e) {
                markPipelineAsCanBeTriggered(pipelineConfig);
                materialUpdateStatusNotifier.removeListenerFor(pipelineConfig);
                throw e;
            }
        }

        public void onMaterialUpdate(MaterialUpdateCompletedMessage message) {
            Material material = message.getMaterial();
            pendingMaterials.remove(material.getFingerprint());
            if (message instanceof MaterialUpdateFailedMessage) {
                String failureReason = ((MaterialUpdateFailedMessage) message).getReason();
                LOGGER.error(format("not scheduling pipeline %s after manual-trigger because update of material failed with reason %s", pipelineConfig.name(), failureReason));
                showError(CaseInsensitiveString.str(pipelineConfig.name()), format("Could not trigger pipeline '%s'", pipelineConfig.name()),
                        format("Material update failed for material '%s' because: %s", material.getDisplayName(), failureReason));
                failed = true;
            }
            if (pendingMaterials.isEmpty()) {
                materialUpdateStatusNotifier.removeListenerFor(pipelineConfig);
                markPipelineAsCanBeTriggered(pipelineConfig);
                if (!failed) {
                    newProduceBuildCause(pipelineConfig, buildType, scheduleOptions, new ServerHealthStateOperationResult(), message.trackingId());
                }
            }
        }

        public boolean isListeningFor(Material material) {
            return pendingMaterials.containsKey(material.getFingerprint());
        }
    }
}
