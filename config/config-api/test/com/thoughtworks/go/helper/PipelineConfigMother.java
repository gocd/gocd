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

package com.thoughtworks.go.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;

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
        return pipelineConfig(name, new MaterialConfigs(),new JobConfigs(JobConfigMother.createJobConfigWithJobNameAndEmptyResources()));
    }

    public static PipelineConfig pipelineConfig(String name, MaterialConfigs materialConfigs, JobConfigs jobConfigs) {
        return new PipelineConfig(new CaseInsensitiveString(name), materialConfigs, new StageConfig(new CaseInsensitiveString("mingle"), jobConfigs));
    }

    public static PipelineConfigs createGroup(String groupName, PipelineConfig... pipelineConfigs) {
        return new BasicPipelineConfigs(groupName, new Authorization(), pipelineConfigs);
    }

    public static PipelineConfigs createGroup(String groupName, String... pipelineConfigs) {
        List<PipelineConfig> configs = new ArrayList<PipelineConfig>();
        for (String name : pipelineConfigs) {
            configs.add(pipelineConfig(name));
        }
        return createGroup(groupName, configs.toArray(new PipelineConfig[0]));
    }

    public static List<PipelineConfigs> createGroups(String... groupNames) {
        List<PipelineConfigs> configs = new ArrayList<PipelineConfigs>();
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
        List<StageConfig> stages = Arrays.asList(new StageConfig(new CaseInsensitiveString("mingle"), new JobConfigs()));
        return new PipelineConfig(new CaseInsensitiveString(name), PipelineLabel.COUNT_TEMPLATE, timerSpec, timerShouldTriggerOnlyOnMaterialChanges, MaterialConfigsMother.defaultMaterialConfigs(), stages);
    }

    public static PipelineConfig pipelineConfigWithTrackingTool(String name, String trackingToolUrl, String trackingToolRegex) {
        PipelineConfig pipelineConfig = pipelineConfig(name);
        TrackingTool trackingTool = new TrackingTool(trackingToolUrl, trackingToolRegex);
        pipelineConfig.setTrackingTool(trackingTool);
        return pipelineConfig;
    }

    public static PipelineConfig pipelineConfigWithMingleConfiguration(String name, String mingleUrl, String mingleProjectId, String mql) {
        PipelineConfig pipelineConfig = pipelineConfig(name);
        MingleConfig mingleConfig = new MingleConfig(mingleUrl, mingleProjectId, mql);
        pipelineConfig.setMingleConfig(mingleConfig);
        return pipelineConfig;
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
}

