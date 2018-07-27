/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.*;

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
        jobConfig.addTask(antTask());
    }

    public static JobConfig createJobConfigWithJobNameAndEmptyResources() {
        return new JobConfig(new CaseInsensitiveString("defaultJob"), new ResourceConfigs(new ResourceConfig("Linux"), new ResourceConfig()), new ArtifactConfigs());
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
        job.getProperties().add(new ArtifactPropertyConfig("coverage.class", "target/emma/coverage.xml", "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"));
        return job;
    }

    public static JobConfig createJobConfigWithResourceAndArtifactPlans() {
        return new JobConfig(new CaseInsensitiveString("defaultJob"), new ResourceConfigs(new ResourceConfig("Linux"), new ResourceConfig("Java")), new ArtifactConfigs(Arrays.asList(new BuildArtifactConfig("src", "dest"))));
    }

    public static JobConfig elasticJob(String elasticProfileId) {
        return elasticJob(UUID.randomUUID().toString(), elasticProfileId);
    }

    public static JobConfig elasticJob(String name, String elasticProfileId) {
        JobConfig jobConfig = jobWithNoResourceRequirement();
        jobConfig.setElasticProfileId(elasticProfileId);
        jobConfig.setName(name);
        return jobConfig;
    }

    public static JobConfig jobWithNoResourceRequirement() {
        JobConfig jobConfig = jobConfig();
        jobConfig.setName(UUID.randomUUID().toString());
        jobConfig.setRunInstanceCount((String) null);
        jobConfig.resourceConfigs().clear();
        return jobConfig;
    }

    public static JobConfig withAllKindsOfTasks(String name) {
        JobConfig jobConfig = jobWithNoResourceRequirement();
        jobConfig.setName(name);
        jobConfig.addTask(nantTask());
        jobConfig.addTask(antTask());
        jobConfig.addTask(execTask());
        jobConfig.addTask(rakeTask());
        jobConfig.addTask(pluggableTask());
        jobConfig.addTask(fetchTask());
        return jobConfig;
    }

    private static Task fetchTask() {
        return new FetchTask(new CaseInsensitiveString("up"), new CaseInsensitiveString("stage"),
                new CaseInsensitiveString("job"), "src", "dest");
    }

    private static Task pluggableTask() {
        return new PluggableTask(new PluginConfiguration("id", "version"),
                new Configuration(
                        new ConfigurationProperty(new ConfigurationKey("name"), new ConfigurationValue("value")),
                        new ConfigurationProperty(new ConfigurationKey("name"), new EncryptedConfigurationValue("value"))));
    }

    private static Task rakeTask() {
        RakeTask task = new RakeTask();
        task.setBuildFile("rakefile");
        task.setTarget("target");
        return task;
    }

    private static Task execTask() {
        ExecTask execTask = new ExecTask();
        execTask.setCommand("command");
        execTask.setArgs("args");
        execTask.setArgsList(new String[]{"arg1", "arg2"});
        execTask.setWorkingDirectory("dir");
        return execTask;
    }

    private static AntTask antTask() {
        AntTask task = new AntTask();
        task.setBuildFile("build-file");
        task.setTarget("target");
        task.setWorkingDirectory("working-directory");
        return task;
    }

    private static NantTask nantTask() {
        NantTask nantTask = new NantTask();
        nantTask.setBuildFile("default.build");
        nantTask.setTarget("test");
        nantTask.setWorkingDirectory("lib");
        nantTask.setNantPath("tmp");
        return nantTask;
    }
}
