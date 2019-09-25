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
package com.thoughtworks.go.helper;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;

public class PipelineConfigMother {
    public static PipelineConfigs studiosAndEvolve() {
        return new BasicPipelineConfigs(pipelineConfig("studios"), pipelineConfig("evolve"));
    }

    public static PipelineConfig pipelineConfig(String pipelineName, String stageName, MaterialConfigs materialConfigs, String... buildNames) {
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs,
                new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs(buildNames)));
    }

    public static PipelineConfig createPipelineConfig(String pipelineName, String stageName, String... buildNames) {
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), MaterialConfigsMother.defaultMaterialConfigs(),
                new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs(buildNames)));
    }

    public static PipelineConfig createPipelineConfigWithStages(String pipelineName, String... stageNames) {
        StageConfig[] configs = new StageConfig[stageNames.length];
        int i = 0;
        for (String stageName : stageNames) {
            configs[i++] = new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs("dev"));
        }
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), MaterialConfigsMother.defaultMaterialConfigs(), configs);
    }

    public static PipelineConfig createPipelineConfigWithStage(String pipelineName, String stageName) {
        return createPipelineConfigWithStages(pipelineName, stageName);
    }

    public static PipelineConfig pipelineConfig(String name) {
        return new PipelineConfig(new CaseInsensitiveString(name), MaterialConfigsMother.defaultMaterialConfigs(), new StageConfig(new CaseInsensitiveString("mingle"), new JobConfigs()));
    }

    public static PipelineConfig pipelineConfig(String name, MaterialConfig materialConfig, JobConfigs jobConfigs) {
        return pipelineConfig(name, new MaterialConfigs(materialConfig), jobConfigs);
    }

    public static PipelineConfig pipelineConfig(String name, MaterialConfigs materialConfigs) {
        return pipelineConfig(name, materialConfigs, new JobConfigs());
    }

    public static PipelineConfig createPipelineConfigWithJobConfigs(String name) {
        return pipelineConfig(name, new MaterialConfigs(), new JobConfigs(JobConfigMother.createJobConfigWithJobNameAndEmptyResources()));
    }

    public static PipelineConfig pipelineConfig(String name, MaterialConfigs materialConfigs, JobConfigs jobConfigs) {
        return new PipelineConfig(new CaseInsensitiveString(name), materialConfigs, new StageConfig(new CaseInsensitiveString("mingle"), jobConfigs));
    }

    public static PipelineConfigs createGroup(String groupName, PipelineConfig... pipelineConfigs) {
        return new BasicPipelineConfigs(groupName, new Authorization(), pipelineConfigs);
    }

    public static PipelineConfigs createGroup(String groupName, String... pipelineConfigs) {
        List<PipelineConfig> configs = new ArrayList<>();
        for (String name : pipelineConfigs) {
            configs.add(pipelineConfig(name));
        }
        return createGroup(groupName, configs.toArray(new PipelineConfig[0]));
    }

    public static List<PipelineConfigs> createGroups(String... groupNames) {
        List<PipelineConfigs> configs = new ArrayList<>();
        int i = 0;
        for (String groupName : groupNames) {
            configs.add(createGroup(groupName, "pipeline_" + i++, "pipeline_" + i++));
        }
        return configs;
    }

    public static PipelineConfig pipelineConfig(String name, StageConfig... stageConfigs) {
        return new PipelineConfig(new CaseInsensitiveString(name), MaterialConfigsMother.defaultMaterialConfigs(), stageConfigs);
    }

    public static PipelineConfig pipelineConfigWithTimer(String name, String cronSpec) {
        return pipelineConfigWithTimer(name, cronSpec, false);
    }

    public static PipelineConfig pipelineConfigWithTimer(String name, String timerSpec, boolean timerShouldTriggerOnlyOnMaterialChanges) {
        List<StageConfig> stages = asList(new StageConfig(new CaseInsensitiveString("mingle"), new JobConfigs()));
        return new PipelineConfig(new CaseInsensitiveString(name), PipelineLabel.COUNT_TEMPLATE, timerSpec, timerShouldTriggerOnlyOnMaterialChanges, MaterialConfigsMother.defaultMaterialConfigs(), stages);
    }

    public static PipelineConfigs groupWithOperatePermission(PipelineConfig pipelineConfig, String... users) {
        Authorization authorization = new Authorization();
        for (String user : users) {
            authorization.getOperationConfig().add(new AdminUser(new CaseInsensitiveString(user)));
        }
        return new BasicPipelineConfigs("defaultGroup", authorization, pipelineConfig);
    }

    public static PipelineConfig pipelineConfigWithTemplate(String name, String templateName) {
        PipelineConfig pipelineWithTemplate = new PipelineConfig(new CaseInsensitiveString(name), MaterialConfigsMother.defaultMaterialConfigs());
        pipelineWithTemplate.setTemplateName(new CaseInsensitiveString(templateName));
        return pipelineWithTemplate;
    }

    public static PipelineConfig renamePipeline(PipelineConfig oldConfig, String newPipelineName) {
        PipelineConfig newConfig = new Cloner().deepClone(oldConfig);
        HashMap attributes = new HashMap();
        attributes.put(PipelineConfig.NAME, newPipelineName);
        newConfig.setConfigAttributes(attributes);
        return newConfig;
    }

    public static PipelineConfig pipelineWithElasticJob(String... elasticProfileIds) {
        PipelineConfig pipelineConfig = pipelineConfig(UUID.randomUUID().toString());
        pipelineConfig.first().getJobs().clear();
        for (String elasticProfileId : elasticProfileIds) {
            pipelineConfig.first().getJobs().add(JobConfigMother.elasticJob(elasticProfileId));
        }
        return pipelineConfig;
    }

    public static PipelineConfig pipelineWithElasticJobs(String elasticProfileId, String pipelineName, String stageName, String... jobNames) {
        final PipelineConfig pipelineConfig = createPipelineConfig(pipelineName, stageName, jobNames);
        final StageConfig stage = pipelineConfig.getStage(stageName);

        for (JobConfig job : stage.getJobs()) {
            job.setElasticProfileId(elasticProfileId);
        }

        return pipelineConfig;
    }

    public static PipelineConfig pipelineWithSecretParams(SecretParam secretParam, String pipelineName, String stageName, String... jobNames) {
        final PipelineConfig pipelineConfig = createPipelineConfig(pipelineName, stageName, jobNames);
        final StageConfig stage = pipelineConfig.getStage(stageName);

        for (JobConfig job : stage.getJobs()) {
            job.setVariables(
                    new EnvironmentVariablesConfig(
                            asList(
                                    new EnvironmentVariableConfig(new GoCipher(), secretParam.getKey(), secretParam.asString(), true)
                            )
                    )
            );
        }

        return pipelineConfig;
    }

    public static PipelineConfig templateBasedPipelineWithElasticJobs(String templateName, String elasticProfileId, String pipelineName, String stageName, String... jobNames) {
        final StageConfig stageConfig = new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs(jobNames));
        for (JobConfig job : stageConfig.getJobs()) {
            job.setElasticProfileId(elasticProfileId);
        }

        final PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setName(pipelineName);
        pipelineConfig.setTemplateName(new CaseInsensitiveString(templateName));
        pipelineConfig.usingTemplate(new PipelineTemplateConfig(new CaseInsensitiveString(templateName), stageConfig));
        pipelineConfig.setOrigin(new FileConfigOrigin());

        return pipelineConfig;
    }

    public static PipelineConfig templateBasedPipelineWithSecretParams(String templateName, SecretParam secretParam, String pipelineName, String stageName, String... jobNames) {
        final StageConfig stageConfig = new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs(jobNames));
        for (JobConfig job : stageConfig.getJobs()) {
            job.setVariables(
                    new EnvironmentVariablesConfig(
                            asList(
                                    new EnvironmentVariableConfig(new GoCipher(), secretParam.getKey(), secretParam.asString(), true)
                            )
                    )
            );
        }

        final PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setName(pipelineName);
        pipelineConfig.setTemplateName(new CaseInsensitiveString(templateName));
        pipelineConfig.usingTemplate(new PipelineTemplateConfig(new CaseInsensitiveString(templateName), stageConfig));
        pipelineConfig.setOrigin(new FileConfigOrigin());

        return pipelineConfig;
    }

    public static PipelineConfig createManualTriggerPipelineConfig(MaterialConfig materialConfig, String pipelineName, String stageName, String... buildNames) {
        PipelineConfig pipelineConfig = createPipelineConfig(pipelineName, stageName, buildNames);
        pipelineConfig.materialConfigs().clear();
        materialConfig.setName(new CaseInsensitiveString(String.format("%s-%s", pipelineName, materialConfig.getType())));
        materialConfig.setAutoUpdate(false);
        pipelineConfig.materialConfigs().add(materialConfig);
        pipelineConfig.first().setApproval(Approval.manualApproval());
        return pipelineConfig;
    }

    public static PipelineConfig pipelineConfigWithExternalArtifact(String pipelineName, ArtifactTypeConfig artifactTypeConfig) {
        final String stageName = pipelineName + ".stage";
        final String jobName = pipelineName + ".job";
        PipelineConfig pipelineConfig = createPipelineConfig(pipelineName, stageName, jobName);
        final JobConfig jobConfig = pipelineConfig.getStage(stageName).jobConfigByConfigName(jobName);
        jobConfig.artifactConfigs().add(artifactTypeConfig);
        return pipelineConfig;
    }
}

