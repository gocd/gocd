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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.materials.StaleMaterialsOnBuildCause;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.utils.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * @understands loads scheduled pipeline for creating work
 */
@Component
public class ScheduledPipelineLoader {
    final private PipelineSqlMapDao pipelineDao;
    final private GoConfigService goConfigService;
    final private JobInstanceService jobInstanceService;
    final private ServerHealthService serverHealthService;
    final private ArtifactsService artifactsService;
    final private TransactionSynchronizationManager transactionSynchronizationManager;
    private final ScheduleService scheduleService;
    private MaterialExpansionService materialExpansionService;

    @Autowired
    public ScheduledPipelineLoader(TransactionSynchronizationManager transactionSynchronizationManager, PipelineSqlMapDao pipelineDao, GoConfigService goConfigService,
                                   JobInstanceService jobInstanceService, ServerHealthService serverHealthService, ArtifactsService artifactsService, ScheduleService scheduleService,
                                   MaterialExpansionService materialExpansionService) {
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.pipelineDao = pipelineDao;
        this.goConfigService = goConfigService;
        this.jobInstanceService = jobInstanceService;
        this.serverHealthService = serverHealthService;
        this.artifactsService = artifactsService;
        this.scheduleService = scheduleService;
        this.materialExpansionService = materialExpansionService;
    }

    //TODO: Do we need to do this differently than PipelineService#fullPipeline?
    public Pipeline pipelineWithPasswordAwareBuildCauseByBuildId(final long buildId) {
        Pipeline pipeline = pipelineDao.pipelineWithMaterialsAndModsByBuildId(buildId);
        MaterialRevisions scheduledRevs = pipeline.getBuildCause().getMaterialRevisions();
        MaterialConfigs knownMaterials = knownMaterials(scheduledRevs);
        for (MaterialRevision materialRevision : scheduledRevs) {
            MaterialConfig materialConfig = materialFrom(knownMaterials, materialRevision);
            Material usedMaterial = materialRevision.getMaterial();
            if (materialConfig == null) {
                final JobInstance jobInstance = jobInstanceService.buildByIdWithTransitions(buildId);
                scheduleService.failJob(jobInstance);
                final String message = "Cannot load job '" + jobInstance.buildLocator() + "' because material " + usedMaterial.config() + " was not found in config.";
                final String description = "Job for pipeline '" + jobInstance.buildLocator() + "' has been failed as one or more material configurations were either changed or removed.";
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        final ServerHealthState error = ServerHealthState.error(message, description, HealthStateType.general(HealthStateScope.forJob(jobInstance.getPipelineName(), jobInstance.getStageName(), jobInstance.getName())));
                        error.setTimeout(Timeout.FIVE_MINUTES);
                        serverHealthService.update(error);
                        appendToConsoleLog(jobInstance, message);
                        appendToConsoleLog(jobInstance, description);
                    }
                });
                throw new StaleMaterialsOnBuildCause(message);
            }
            if (materialConfig instanceof PasswordAwareMaterial) {
                PasswordAwareMaterial configuredMaterial = (PasswordAwareMaterial) materialConfig;
                ((PasswordAwareMaterial) usedMaterial).setPassword(configuredMaterial.getPassword());
            }
        }
        return pipeline;
    }

    private MaterialConfig materialFrom(MaterialConfigs knownMaterials, MaterialRevision materialRevision) {
        for (MaterialConfig knownMaterial : knownMaterials) {
            if (knownMaterial.getFingerprint().equals(materialRevision.getMaterial().getFingerprint())) {
                return knownMaterial;
            }
        }
        return null;
    }

    private MaterialConfigs knownMaterials(MaterialRevisions scheduledRevs) {
        CruiseConfig currentConfig = goConfigService.getCurrentConfig();
        MaterialConfigs configuredMaterials = new MaterialConfigs();
        for (MaterialRevision revision : scheduledRevs) {
            MaterialConfig configuredMaterial = currentConfig.materialConfigFor(revision.getMaterial().getFingerprint());
            if (configuredMaterial != null) {
                configuredMaterials.add(configuredMaterial);
            }
        }
        MaterialConfigs knownMaterials = new MaterialConfigs();
        for (MaterialConfig configuredMaterial : configuredMaterials) {
            materialExpansionService.expandForScheduling(configuredMaterial, knownMaterials);
        }
        return knownMaterials;
    }

    private void appendToConsoleLog(JobInstance jobInstance, String message) {
        try {
            artifactsService.appendToConsoleLog(jobInstance.getIdentifier(), "\n" + message + "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
