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

import java.util.Arrays;
import java.util.HashMap;

import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.Approval;
import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.AuthConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.util.ReflectionUtil;

public class StageConfigMother {
    public static StageConfig oneBuildPlanWithResourcesAndMaterials(String stageName) {
        return oneBuildPlanWithResourcesAndMaterials(stageName, "Foo");
    }

    public static StageConfig oneBuildPlanWithResourcesAndMaterials(String stageName, String buildName) {
        JobConfigs jobConfigs = new JobConfigs(new JobConfig(
                new CaseInsensitiveString(buildName), new Resources(new Resource("Windows"), new Resource(".NET")), new ArtifactPlans()));
        return stageConfig(stageName, jobConfigs);
    }

    public static StageConfig twoBuildPlansWithResourcesAndMaterials(String stageName) {
        JobConfig windoze = new JobConfig(
                new CaseInsensitiveString("WinBuild"), new Resources(new Resource("Windows"), new Resource(".NET")), new ArtifactPlans(Arrays.asList(new ArtifactPlan(ArtifactType.unit, "junit", "junit")))
        );
        JobConfig linux = new JobConfig(
                new CaseInsensitiveString("NixBuild"), new Resources(new Resource("Linux"), new Resource("java")), new ArtifactPlans(Arrays.asList(new ArtifactPlan(ArtifactType.unit, "junit", "junit")))
        );
        JobConfigs jobConfigs = new JobConfigs(windoze, linux);
        return stageConfig(stageName, jobConfigs);
    }

    public static StageConfig stageConfig(String stageName) {
        return new StageConfig(new CaseInsensitiveString(stageName), new JobConfigs());
    }

   public static StageConfig stageConfig(String stageName, JobConfigs jobConfigs) {
        return new StageConfig(new CaseInsensitiveString(stageName), jobConfigs);
    }

    public static StageConfig custom(String stageName, JobConfigs jobConfigs) {
        return new StageConfig(new CaseInsensitiveString(stageName), jobConfigs);
    }

    public static StageConfig custom(String stageName, String ... buildNames) {
        return new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs(buildNames));
    }

    public static StageConfig custom(String stageName, boolean artifactCleanupProhibited, String ... buildNames) {
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs(buildNames));
        ReflectionUtil.setField(stageConfig, "artifactCleanupProhibited", artifactCleanupProhibited);
        return stageConfig;
    }

    public static StageConfig custom(String stageName, AuthConfig adminsConfig) {
        return custom(stageName, new Approval(adminsConfig));
    }

    public static StageConfig custom(String stageName, Approval approval) {
        return new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.withBuildPlans("default"), approval);
    }

    public static StageConfig custom(String name, boolean fetchMaterials, boolean cleanWorkingDir, JobConfigs jobConfigs, Approval approval) {
        return new StageConfig(new CaseInsensitiveString(name), fetchMaterials, cleanWorkingDir, approval, false, jobConfigs);
    }

    public static StageConfig stageWithTasks(String stageName) {
        JobConfig job = new JobConfig("job");
        job.addTask(new ExecTask("ls", "-la", "pwd"));
        job.addTask(new AntTask());
        return new StageConfig(new CaseInsensitiveString(stageName), new JobConfigs(job));
    }

    public static StageConfig manualStage(String stageName) {
        return custom(stageName, new Approval());
    }

    public static StageConfig stageConfigWithArtifact(String stageName, String jobName , ArtifactType artifactType){
        ArtifactPlans artifactPlansWithTests = new ArtifactPlans();
        artifactPlansWithTests.add(new ArtifactPlan(artifactType, "src", "dest"));
        JobConfig job1 = new JobConfig(new CaseInsensitiveString(jobName), new Resources("abc"), artifactPlansWithTests);
        StageConfig stage = new StageConfig(new CaseInsensitiveString(stageName), new JobConfigs(job1));
        return stage;
    }

    public static void addApprovalWithUsers(StageConfig stage, String... users) {
        Approval approval = stage.getApproval();
        for (String user : users) {
            approval.getAuthConfig().add(new AdminUser(new CaseInsensitiveString(user)));
        }
        stage.updateApproval(approval);
    }

    public static void addApprovalWithRoles(StageConfig stage, String... roles) {
        Approval approval = stage.getApproval();
        for (String role : roles) {
            approval.getAuthConfig().add(new AdminRole(new CaseInsensitiveString(role)));
        }
        stage.updateApproval(approval);
    }

    public static void renameStage(StageConfig stageConfig, String stageName) {
        HashMap attributes = new HashMap();
        attributes.put(StageConfig.NAME, stageName);
        stageConfig.setConfigAttributes(attributes);
    }
}
