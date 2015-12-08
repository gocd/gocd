/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.util.Node;
import org.apache.commons.lang.NotImplementedException;

import java.util.Set;

public class PipelineConfigSaveValidationContext implements ValidationContext {
    private final Boolean isPipelineBeingCreated;
    private final String groupName;
    private final Validatable immediateParent;
    private CruiseConfig cruiseConfig;
    private PipelineConfig pipelineBeingValidated;
    private boolean isPipeline = true;//Does not support template editing yet
    private final PipelineConfigSaveValidationContext parentContext;
    private PipelineConfig pipeline;
    private StageConfig stage;
    private JobConfig job;

    private PipelineConfigSaveValidationContext(Boolean isPipelineBeingCreated, String groupName, Validatable immediateParent) {
        this.isPipelineBeingCreated = isPipelineBeingCreated;
        this.groupName = groupName;
        this.immediateParent = immediateParent;
        this.parentContext = null;
    }

    private PipelineConfigSaveValidationContext(Validatable immediateParent, PipelineConfigSaveValidationContext parentContext) {
        this.isPipelineBeingCreated = parentContext.isPipelineBeingCreated();
        this.groupName = parentContext.groupName;
        this.immediateParent = immediateParent;
        this.parentContext = parentContext;
        if (immediateParent instanceof CruiseConfig) {
            this.cruiseConfig = (CruiseConfig) immediateParent;
        } else if (parentContext.cruiseConfig != null) {
            this.cruiseConfig = parentContext.cruiseConfig;
        }
        if (immediateParent instanceof PipelineConfig) {
            this.pipeline = (PipelineConfig) immediateParent;
        } else if (parentContext.pipeline != null) {
            this.pipeline = parentContext.pipeline;
        }
        if (immediateParent instanceof JobConfig) {
            this.job = (JobConfig) immediateParent;
        } else if (parentContext.getJob() != null) {
            this.job = parentContext.job;
        }
        if (immediateParent instanceof StageConfig) {
            this.stage = (StageConfig) immediateParent;
        } else if (parentContext.stage != null) {
            this.stage = parentContext.stage;
        }
        this.pipelineBeingValidated = parentContext.pipelineBeingValidated;
        if (parentContext.pipelineBeingValidated == null) {
            this.pipelineBeingValidated = this.pipeline;
        }
    }

    public static PipelineConfigSaveValidationContext forChain(Boolean isPipelineBeingCreated, String groupName, Validatable... validatables) {
        PipelineConfigSaveValidationContext tail = new PipelineConfigSaveValidationContext(isPipelineBeingCreated, groupName, null);
        for (Validatable validatable : validatables) {
            tail = tail.withParent(validatable);
        }
        return tail;
    }

    public PipelineConfigSaveValidationContext withParent(Validatable current) {
        return new PipelineConfigSaveValidationContext(current, this);
    }

    @Override
    public ConfigReposConfig getConfigRepos() {
        throw new NotImplementedException();
    }

    public JobConfig getJob() {
        return this.job;
    }

    public StageConfig getStage() {
        return this.stage;
    }

    public PipelineConfig getPipeline() {
        return this.pipeline;
    }

    public PipelineTemplateConfig getTemplate() {
        throw new NotImplementedException();
    }

    public String getParentDisplayName() {
        return getParent().getClass().getAnnotation(ConfigTag.class).value();
    }

    public Validatable getParent() {
        return immediateParent;
    }

    public boolean isWithinTemplates() {
        return !isPipeline;
    }

    public boolean isWithinPipelines() {
        return isPipeline;
    }

    public PipelineConfigs getPipelineGroup() {
        if (cruiseConfig.hasPipelineGroup(groupName))
            return cruiseConfig.findGroup(groupName);
        else return null;
    }


    public Node getDependencyMaterialsFor(CaseInsensitiveString pipelineName) {
        return PipelineConfigurationCache.getInstance().getDependencyMaterialsFor(pipelineName);
    }

    @Override
    public String toString() {
        return "ValidationContext{" +
                "immediateParent=" + immediateParent +
                ", parentContext=" + parentContext +
                '}';
    }

    public MaterialConfigs getAllMaterialsByFingerPrint(String fingerprint) {
        return PipelineConfigurationCache.getInstance().getMatchingMaterialsFromConfig(fingerprint);
    }

    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        if (pipelineBeingValidated.name().equals(pipelineName)) return pipelineBeingValidated;
        return PipelineConfigurationCache.getInstance().getPipelineConfig(pipelineName.toString());
    }

    @Override
    public boolean shouldCheckConfigRepo() {
        return false;
    }

    @Override
    public SecurityConfig getServerSecurityConfig() {
        return cruiseConfig.server().security();
    }

    public boolean doesTemplateExist(CaseInsensitiveString template) {
        return cruiseConfig.getTemplates().hasTemplateNamed(template);
    }

    @Override
    public SCM findScmById(String scmID) {
        return cruiseConfig.getSCMs().find(scmID);
    }

    @Override
    public PackageRepository findPackageById(String packageId) {
        return cruiseConfig.getPackageRepositories().findPackageRepositoryHaving(packageId) ;
    }

    public Set<CaseInsensitiveString> getPipelinesWithDependencyMaterials() {
        return PipelineConfigurationCache.getInstance().getPipelinesWithDependencyMaterials();
    }

    public PipelineGroups getGroups() {
        return cruiseConfig.getGroups();
    }

    public boolean isPipelineBeingCreated() {
        return isPipelineBeingCreated;
    }
}
