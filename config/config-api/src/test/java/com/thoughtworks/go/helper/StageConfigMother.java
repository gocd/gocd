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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class StageConfigMother {
    public static StageConfig oneBuildPlanWithResourcesAndMaterials(String stageName) {
        return oneBuildPlanWithResourcesAndMaterials(stageName, "Foo");
    }

    public static StageConfig oneBuildPlanWithResourcesAndMaterials(String stageName, String buildName) {
        JobConfigs jobConfigs = new JobConfigs(new JobConfig(
                new CaseInsensitiveString(buildName), new ResourceConfigs(new ResourceConfig("Windows"), new ResourceConfig(".NET")), new ArtifactTypeConfigs()));
        return stageConfig(stageName, jobConfigs);
    }

    public static StageConfig twoBuildPlansWithResourcesAndMaterials(String stageName) {
        JobConfig windoze = new JobConfig(
                new CaseInsensitiveString("WinBuild"), new ResourceConfigs(new ResourceConfig("Windows"), new ResourceConfig(".NET")), new ArtifactTypeConfigs(Arrays.asList(new TestArtifactConfig("junit", "junit")))
        );
        JobConfig linux = new JobConfig(
                new CaseInsensitiveString("NixBuild"), new ResourceConfigs(new ResourceConfig("Linux"), new ResourceConfig("java")), new ArtifactTypeConfigs(Arrays.asList(new TestArtifactConfig("junit", "junit")))
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

    public static StageConfig custom(String stageName, String... buildNames) {
        return new StageConfig(new CaseInsensitiveString(stageName), BuildPlanMother.jobConfigs(buildNames));
    }

    public static StageConfig custom(String stageName, boolean artifactCleanupProhibited, String... buildNames) {
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

    public static StageConfig stageConfigWithEnvironmentVariable(String stageName) {
        StageConfig stageConfig = StageConfigMother.stageConfig(stageName);
        stageConfig.setVariables(EnvironmentVariablesConfigMother.environmentVariables());
        stageConfig.getJobs().add(JobConfigMother.jobConfig());
        return stageConfig;
    }

    public static StageConfig stageConfigWithParams(String stageName, String paramName) {
        StageConfig stageConfig = StageConfigMother.stageConfig(stageName);
        ArrayList<EnvironmentVariableConfig> environmentVariableConfigs = new ArrayList<>();
        environmentVariableConfigs.add(new EnvironmentVariableConfig("env1", "#{" + paramName + "}"));
        stageConfig.setVariables(new EnvironmentVariablesConfig(environmentVariableConfigs));
        stageConfig.getJobs().add(JobConfigMother.jobConfig());
        return stageConfig;
    }

    public static StageConfig stageConfigWithArtifact(String stageName, String jobName, ArtifactType artifactType) {
        ArtifactTypeConfigs artifactConfigsWithTests = new ArtifactTypeConfigs();
        artifactConfigsWithTests.add(createArtifactConfig(artifactType));
        JobConfig job1 = new JobConfig(new CaseInsensitiveString(jobName), new ResourceConfigs("abc"), artifactConfigsWithTests);
        StageConfig stage = new StageConfig(new CaseInsensitiveString(stageName), new JobConfigs(job1));
        return stage;
    }

    private static ArtifactTypeConfig createArtifactConfig(ArtifactType artifactType) {
        if (artifactType == ArtifactType.external) {
            return createPluggableArtifactConfig();
        } else {
            return createBuiltInArtifactConfig(artifactType, "src", "dest");
        }
    }

    private static ArtifactTypeConfig createBuiltInArtifactConfig(ArtifactType artifactType, String src, String dest) {
        if (artifactType == ArtifactType.build) {
            return new BuildArtifactConfig(src, dest);
        } else if (artifactType == ArtifactType.test) {
            return new TestArtifactConfig(src, dest);
        } else {
            throw bomb("ArtifactType not specified");
        }
    }

    private static PluggableArtifactConfig createPluggableArtifactConfig() {
        return new PluggableArtifactConfig("installer", "s3", create("src", false, "build/libs.*.zip"));
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
