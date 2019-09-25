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

import java.util.Arrays;
import java.util.UUID;

public class JobConfigMother {
    public static JobConfig job() {
        JobConfig jobConfig = createJobConfigWithJobNameAndEmptyResources();
        addTask(jobConfig);
        return jobConfig;
    }

    private static void addTask(JobConfig jobConfig) {
        jobConfig.setVariables(EnvironmentVariablesConfigMother.environmentVariables());
        AntTask task = new AntTask();
        task.setBuildFile("build-file");
        task.setTarget("target");
        task.setWorkingDirectory("working-directory");
        jobConfig.addTask(task);
    }

    public static JobConfig createJobConfigWithJobNameAndEmptyResources() {
        return new JobConfig(new CaseInsensitiveString("defaultJob"), new ResourceConfigs(new ResourceConfig("Linux"), new ResourceConfig()), new ArtifactTypeConfigs());
    }

    public static JobConfig jobConfig() {
        JobConfig job = createJobConfigWithResourceAndArtifactPlans();
        addTask(job);
        job.setTimeout("100");
        job.setRunInstanceCount(3);
        job.artifactConfigs().clear();
        job.artifactConfigs().add(new BuildArtifactConfig("target/dist.jar", "pkg"));
        job.artifactConfigs().add(new TestArtifactConfig("target/reports/**/*Test.xml", "reports"));
        job.addTab("coverage", "Jcoverage/index.html");
        job.addTab("something", "something/path.html");
        return job;
    }

    public static JobConfig createJobConfigWithResourceAndArtifactPlans() {
        return new JobConfig(new CaseInsensitiveString("defaultJob"), new ResourceConfigs(new ResourceConfig("Linux"), new ResourceConfig("Java")), new ArtifactTypeConfigs(Arrays.asList(new BuildArtifactConfig("src", "dest"))));
    }

    public static JobConfig elasticJob(String elasticProfileId) {
        JobConfig jobConfig = jobWithNoResourceRequirement();
        jobConfig.setElasticProfileId(elasticProfileId);
        return jobConfig;
    }

    public static JobConfig jobWithNoResourceRequirement() {
        JobConfig jobConfig = jobConfig();
        jobConfig.setName(UUID.randomUUID().toString());
        jobConfig.setRunInstanceCount((String)null);
        jobConfig.resourceConfigs().clear();
        return jobConfig;
    }
}
