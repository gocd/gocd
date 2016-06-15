/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.materials.MaterialChecker;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;

/**
 * @understands a build that was triggered by a change in some external materials
 */
public class AutoBuild implements BuildType {
    private final GoConfigService goConfigService;
    private final PipelineService pipelineService;
    private final String pipelineName;
    private final SystemEnvironment systemEnvironment;
    private final MaterialChecker materialChecker;
    private final ServerHealthService serverHealthService;
    private static final Logger LOGGER = Logger.getLogger(AutoBuild.class);

    public AutoBuild(GoConfigService goConfigService, PipelineService pipelineService, String pipelineName, SystemEnvironment systemEnvironment, MaterialChecker materialChecker,
                     ServerHealthService serverHealthService) {
        this.goConfigService = goConfigService;
        this.pipelineService = pipelineService;
        this.pipelineName = pipelineName;
        this.systemEnvironment = systemEnvironment;
        this.materialChecker = materialChecker;
        this.serverHealthService = serverHealthService;
    }

    public BuildCause onModifications(MaterialRevisions originalMaterialRevisions, boolean materialConfigurationChanged, MaterialRevisions previousMaterialRevisions) {
        if (originalMaterialRevisions == null || originalMaterialRevisions.isEmpty()) {
            throw new RuntimeException("Cannot find modifications, please check your SCM setting or environment.");
        }

        if (!originalMaterialRevisions.hasDependencyMaterials()) {
            return BuildCause.createWithModifications(originalMaterialRevisions, GoConstants.DEFAULT_APPROVED_BY);
        }

        CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();

        MaterialRevisions recomputedBasedOnDependencies;
        if (systemEnvironment.enforceRevisionCompatibilityWithUpstream() && systemEnvironment.enforceFanInFallbackBehaviour()) {
            recomputedBasedOnDependencies = fanInOnWithFallback(originalMaterialRevisions, cruiseConfig, new CaseInsensitiveString(pipelineName));
        } else if (systemEnvironment.enforceRevisionCompatibilityWithUpstream()) {
            recomputedBasedOnDependencies = fanInOn(originalMaterialRevisions, cruiseConfig, new CaseInsensitiveString(pipelineName));
        } else {
            recomputedBasedOnDependencies = fanInOffTriangleDependency(originalMaterialRevisions, cruiseConfig);
        }
        if (recomputedBasedOnDependencies != null && canRunWithRecomputedRevisions(materialConfigurationChanged, previousMaterialRevisions, recomputedBasedOnDependencies)) {
            return BuildCause.createWithModifications(recomputedBasedOnDependencies, GoConstants.DEFAULT_APPROVED_BY);
        }
        return null;
    }

    public BuildCause onEmptyModifications(PipelineConfig pipelineConfig, MaterialRevisions materialRevisions) {
        return null;
    }

    public void canProduce(PipelineConfig pipelineConfig, SchedulingCheckerService schedulingChecker, ServerHealthService serverHealthService, OperationResult operationResult) {
        schedulingChecker.canAutoTriggerProducer(pipelineConfig, operationResult);
    }

    public boolean isValidBuildCause(PipelineConfig pipelineConfig, BuildCause buildCause) {
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            if (materialRevision.isChanged()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldCheckWhetherOlderRunsHaveRunWithLatestMaterials() {
        return true;
    }

    @Override
    public void notifyPipelineNotScheduled(PipelineConfig pipelineConfig) {
    }

    private MaterialRevisions fanInOnWithFallback(MaterialRevisions originalMaterialRevisions, CruiseConfig cruiseConfig, CaseInsensitiveString targetPipelineName) {
        try {
            MaterialRevisions materialRevisions = fanInOn(originalMaterialRevisions, cruiseConfig, targetPipelineName);
            serverHealthService.removeByScope(HealthStateScope.forFanin(pipelineName));
            return materialRevisions;
        } catch (NoCompatibleUpstreamRevisionsException | NoModificationsPresentForDependentMaterialException e) {
            throw e;
        } catch (RuntimeException e) {
            serverHealthService.update(ServerHealthState.warning("Turning off Fan-In for pipeline: '" + pipelineName + "'", "Error occurred during Fan-In resolution for the pipeline.",
                    HealthStateType.general(HealthStateScope.forFanin(pipelineName))));
            LOGGER.info("[Revision Resolution] Error occurred during Fan-In resolution for the pipeline: '" + pipelineName + "'. Switching to Triangle Resolution");
            return fanInOffTriangleDependency(originalMaterialRevisions, cruiseConfig);
        }
    }

    private MaterialRevisions fanInOn(MaterialRevisions originalMaterialRevisions, CruiseConfig cruiseConfig, CaseInsensitiveString targetPipelineName) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Revision Resolution] Fan-In Resolution is active for pipeline " + pipelineName);
        }
        return pipelineService.getRevisionsBasedOnDependencies(originalMaterialRevisions, cruiseConfig, targetPipelineName);
    }

    private MaterialRevisions fanInOffTriangleDependency(MaterialRevisions originalMaterialRevisions, CruiseConfig cruiseConfig) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Revision Resolution] Triangle Resolution is active for pipeline " + pipelineName);
        }
        PipelineConfigDependencyGraph dependencyGraph = goConfigService.upstreamDependencyGraphOf(pipelineName, cruiseConfig);
        if (hasAnyUnsharedMaterialChanged(dependencyGraph, originalMaterialRevisions) || dependencyGraph.isRevisionsOfSharedMaterialsIgnored(originalMaterialRevisions)) {
            return pipelineService.getRevisionsBasedOnDependencies(dependencyGraph, originalMaterialRevisions);
        }
        return null;
    }

    private boolean canRunWithRecomputedRevisions(boolean materialConfigurationChanged, MaterialRevisions previousMaterialRevisions, MaterialRevisions recomputedBasedOnDependencies) {
        return materialConfigurationChanged || previousMaterialRevisions == null ||
                (recomputedBasedOnDependencies.hasChangedSince(previousMaterialRevisions)) && !materialChecker.hasPipelineEverRunWith(pipelineName, recomputedBasedOnDependencies);
    }

    /* TRIANGLE BEGIN */

    boolean hasAnyUnsharedMaterialChanged(PipelineConfigDependencyGraph dependencyGraph, MaterialRevisions originalMaterialRevisions) {
        for (MaterialConfig materialConfig : dependencyGraph.unsharedMaterialConfigs()) {
            MaterialRevision revision = originalMaterialRevisions.findRevisionFor(materialConfig);
            if (revision == null) {
                String message = String.format("Couldn't find material-revision for material '%s' while auto-scheduling pipeline named '%s'", materialConfig, pipelineName);
                RuntimeException exception = new NullPointerException(message);
                LOGGER.error(message, exception);
                throw exception;
            }
            if (revision.isChanged()) {
                return true;
            }
        }
        return false;
    }

    /* TRIANGLE END */
}
