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
 *
 */

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.util.TimeProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;

public class PipelineMother {
    public static final String NAME_SEPARATOR = "-";

    public static PipelineConfig withSingleStageWithMaterials(String pipelineName, String stageName, JobConfigs jobConfigs) {
        MaterialConfigs materialConfigs = com.thoughtworks.go.helper.MaterialConfigsMother.defaultMaterialConfigs();
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs, new StageConfig(new CaseInsensitiveString(stageName), jobConfigs));
    }

    public static Pipeline passedPipelineInstance(String pipelineName, String stageName, String buildName) {
        return pipeline(pipelineName, com.thoughtworks.go.helper.StageMother.passedStageInstance(stageName, buildName, pipelineName));
    }

    public static Pipeline pipeline(String pipelineName, Stage... stages) {
        Materials materials = com.thoughtworks.go.helper.MaterialsMother.defaultMaterials();
        return new Pipeline(pipelineName, BuildCause.createWithModifications(com.thoughtworks.go.helper.ModificationsMother.modifyOneFile(materials, com.thoughtworks.go.helper.ModificationsMother.nextRevision()), ""), stages);
    }

    public static Pipeline pipelineWithAllTypesOfMaterials(String pipelineName, String stageName, String jobName) {
        GitMaterial gitMaterial = MaterialsMother.gitMaterial("http://user:password@gitrepo.com", null, "branch");
        HgMaterial hgMaterial = MaterialsMother.hgMaterial("http://user:password@hgrepo.com");
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial("http://user:password@svnrepo.com", null, "username", "password", false, null);
        TfsMaterial tfsMaterial = MaterialsMother.tfsMaterial("http://user:password@tfsrepo.com");
        P4Material p4Material = MaterialsMother.p4Material("127.0.0.1:1666", "username", "password", "view", false);
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial();
        PackageMaterial packageMaterial = MaterialsMother.packageMaterial();
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        Materials materials = new Materials(gitMaterial, hgMaterial, svnMaterial, tfsMaterial, p4Material, dependencyMaterial, packageMaterial, pluggableSCMMaterial);

        return new Pipeline(pipelineName, BuildCause.createWithModifications(ModificationsMother.modifyOneFile(materials, ModificationsMother.nextRevision()), ""), StageMother.passedStageInstance(stageName, jobName, pipelineName));
    }

    public static Pipeline schedule(PipelineConfig pipelineConfig, BuildCause cause) {
        String approvedBy = "cruise";
        if (pipelineConfig.getFirstStageConfig().getApproval().isManual()) {
            approvedBy = "test";
        }
        return createPipelineInstance(pipelineConfig, cause, approvedBy);
    }

    public static Pipeline scheduleWithContext(PipelineConfig pipelineConfig, BuildCause buildCause, DefaultSchedulingContext context) {
        return createPipelineInstance(pipelineConfig, buildCause, context.getApprovedBy());
    }

    private static Pipeline createPipelineInstance(PipelineConfig pipelineConfig, BuildCause cause, String approvedBy) {
        return new InstanceFactory().createPipelineInstance(pipelineConfig, cause, new DefaultSchedulingContext(approvedBy), "md5-test", new TimeProvider());
    }

    public static Pipeline building(PipelineConfig pipelineConfig) {
        return withState(pipelineConfig, JobState.Building, modifyOneFile(pipelineConfig));
    }

    public static Pipeline completed(PipelineConfig pipelineConfig) {
        return withState(pipelineConfig, JobState.Completed, modifyOneFile(pipelineConfig));
    }

    public static Pipeline buildingWithRevisions(PipelineConfig pipelineConfig, MaterialRevisions materialRevisions) {
        return withState(pipelineConfig, JobState.Building, materialRevisions);
    }

    public static Pipeline preparing(PipelineConfig pipelineConfig) {
        return withState(pipelineConfig, JobState.Preparing, modifyOneFile(pipelineConfig));
    }

    private static Pipeline withState(PipelineConfig pipelineConfig, JobState state, MaterialRevisions revisions) {
        Pipeline pipeline = schedule(pipelineConfig, BuildCause.createWithModifications(revisions, ""));
        for (JobInstance instance : pipeline.getStages().first().getJobInstances()) {
            instance.changeState(state, new Date());
            instance.setAgentUuid("uuid");
        }
        return pipeline;
    }

    public static Pipeline completedPipelineWithStagesAndBuilds(String pipelineName, List<String> baseStageNames, List<String> baseBuildNames) {
        return pipeline(pipelineName, toStageArray(stagesAndBuildsWithEndState(JobState.Completed, JobResult.Passed, baseStageNames, baseBuildNames)));
    }

    private static Stage[] toStageArray(Stages stages) {
        return stages.toArray(new Stage[stages.size()]);
    }

    private static Stages stagesAndBuildsWithEndState(JobState endState, JobResult result, List<String> baseStageNames, List<String> baseBuildNames) {
        Stages stages = new Stages();
        for (String baseStageName : baseStageNames) {
            String stageName = baseStageName;
            stages.add(com.thoughtworks.go.helper.StageMother.stageWithNBuildsHavingEndState(endState, result,
                    stageName, baseBuildNames));
        }
        return stages;
    }

    public static Pipeline firstStageBuildingAndSecondStageScheduled(String pipelineName, List<String> stageNames, List<String> buildNames) {
        if (stageNames.size() < 1) {
            throw new IllegalArgumentException("stageNames is empty!");
        }

        Stages stages = new Stages();
        stages.add(com.thoughtworks.go.helper.StageMother.stageWithNBuildsHavingEndState(JobState.Building, null, stageNames.get(0), buildNames));
        List<String> remainder = stageNames.subList(1, stageNames.size());


        stages.addAll(stagesAndBuildsWithEndState(JobState.Scheduled, null, buildNames, remainder));
        return pipeline(pipelineName, toStageArray(stages));
    }


    public static Pipeline completedFailedStageInstance(String pipelineName, String stageName, String planName,
                                                        Date date) {
        return pipeline(pipelineName, com.thoughtworks.go.helper.StageMother.completedFailedStageInstance(pipelineName, stageName, planName, date));
    }

    public static PipelineConfig twoBuildPlansWithResourcesAndMaterials(String pipelineName, String... stageNames) {
        MaterialConfigs materials = com.thoughtworks.go.helper.MaterialConfigsMother.defaultMaterialConfigs();
        return createPipelineConfig(pipelineName, materials, stageNames);
    }

    public static PipelineConfig createPipelineConfig(String pipelineName, MaterialConfigs materialConfigs, String... stageNames) {
        List<StageConfig> stages = new ArrayList<StageConfig>();
        for (String stageName : stageNames) {
            stages.add(com.thoughtworks.go.helper.StageConfigMother.twoBuildPlansWithResourcesAndMaterials(stageName));
        }
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs, stages.toArray(new StageConfig[0]));
    }

    public static PipelineConfig twoBuildPlansWithResourcesAndSvnMaterialsAtUrl(String pipeline, String stageName, String svnUrl) {
        MaterialConfigs materials = com.thoughtworks.go.helper.MaterialConfigsMother.defaultSvnMaterialConfigsWithUrl(svnUrl);
        return createPipelineConfig(pipeline, materials, stageName);
    }

    public static PipelineConfig twoBuildPlansWithResourcesAndHgMaterialsAtUrl(String pipeline, String stageName, String hgUrl) {
        return twoBuildPlansWithResourcesAndHgMaterialsAtUrl(pipeline, stageName, hgUrl, "hgdir");
    }

    public static PipelineConfig twoBuildPlansWithResourcesAndHgMaterialsAtUrl(String pipeline, String stageName, String hgUrl, String materialDir) {
        MaterialConfig materials = com.thoughtworks.go.helper.MaterialConfigsMother.hgMaterialConfig(hgUrl, materialDir);
        return createPipelineConfig(pipeline, new MaterialConfigs(materials), stageName);
    }

    public static PipelineConfig withMaterials(String pipelineName, String stageName, JobConfigs jobConfigs) {
        MaterialConfigs materialConfigs = com.thoughtworks.go.helper.MaterialConfigsMother.defaultMaterialConfigs();
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs, com.thoughtworks.go.helper.StageConfigMother.stageConfig(stageName, jobConfigs));
    }

    public static PipelineConfig custom(String pipelineName, String stageName, JobConfigs jobConfigs, MaterialConfigs materials) {
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), materials, com.thoughtworks.go.helper.StageConfigMother.custom(stageName, jobConfigs));
    }

    public static PipelineConfig withTwoStagesOneBuildEach(String pipelineName, String stageName, String stageName2) {
        StageConfig stageConfig1 = com.thoughtworks.go.helper.StageConfigMother.oneBuildPlanWithResourcesAndMaterials(stageName);
        StageConfig stageConfig2 = com.thoughtworks.go.helper.StageConfigMother.oneBuildPlanWithResourcesAndMaterials(stageName2);
        MaterialConfigs materialConfigs = MaterialConfigsMother.defaultMaterialConfigs();
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs, stageConfig1, stageConfig2);
    }
}
