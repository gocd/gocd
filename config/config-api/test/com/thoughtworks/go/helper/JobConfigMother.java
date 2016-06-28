/*
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
 */

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.*;

import java.util.Arrays;

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
        return new JobConfig(new CaseInsensitiveString("defaultJob"), new Resources(new Resource("Linux"), new Resource()), new ArtifactPlans());
    }

    public static JobConfig jobConfig() {
        JobConfig job = createJobConfigWithResourceAndArtifactPlans();
        addTask(job);
        job.setTimeout("100");
        job.setRunInstanceCount(3);
        job.artifactPlans().clear();
        job.artifactPlans().add(new ArtifactPlan("target/dist.jar", "pkg"));
        job.artifactPlans().add(new TestArtifactPlan("target/reports/**/*Test.xml", "reports"));
        job.addTab("coverage", "Jcoverage/index.html");
        job.addTab("something", "something/path.html");
        job.getProperties().add(new ArtifactPropertiesGenerator("coverage.class", "target/emma/coverage.xml", "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"));
        return job;
    }

    public static JobConfig createJobConfigWithResourceAndArtifactPlans() {
        return new JobConfig(new CaseInsensitiveString("defaultJob"), new Resources(new Resource("Linux"), new Resource("Java")), new ArtifactPlans(Arrays.asList(new ArtifactPlan("src", "dest"))));
    }
}
