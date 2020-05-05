/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.DataStructureUtils;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class JobConfigTest {
    private JobConfig config;

    @BeforeEach
    void setup() {
        config = new JobConfig();
        Tasks tasks = mock(Tasks.class);
        config.injectTasksForTest(tasks);
        doNothing().when(tasks).setConfigAttributes(any(), any(TaskFactory.class));
    }

    @Test
    void shouldCopyAttributeValuesFromAttributeMap() throws Exception {
        config = new JobConfig();//override the setup mock
        TaskFactory taskFactory = mock(TaskFactory.class);
        ExecTask emptyExecTask = new ExecTask();
        when(taskFactory.taskInstanceFor(emptyExecTask.getTaskType())).thenReturn(emptyExecTask);

        config.setConfigAttributes(DataStructureUtils.m(JobConfig.NAME, "foo-job", JobConfig.TASKS, DataStructureUtils.m(Tasks.TASK_OPTIONS, "exec", "exec",
                DataStructureUtils.m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))), taskFactory);
        assertThat(config.name()).isEqualTo(new CaseInsensitiveString("foo-job"));
        assertThat(config.getTasks().get(0)).isEqualTo(new ExecTask("ls", "-la", "/tmp"));
        assertThat(config.getTasks().size()).isEqualTo(1);
    }

    @Test
    void shouldSetTimeoutIfSpecified() throws Exception {
        config.setConfigAttributes(
                m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.OVERRIDE_TIMEOUT, JobConfig.TIMEOUT, "100", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec", "exec",
                        m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))));
        assertThat(config.getTimeout()).isEqualTo("100");
    }

    @Test
    void shouldClearTimeoutIfSubmittedWithEmptyValue() throws Exception {
        config.setConfigAttributes(
                m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.OVERRIDE_TIMEOUT, JobConfig.TIMEOUT, "", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec",
                        "exec", m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp")))
        );
        assertThat(config.getTimeout()).isNull();
    }

    @Test
    void shouldSetTimeoutToZeroIfSubmittedWithNever() throws Exception {
        config.setConfigAttributes(m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.NEVER_TIMEOUT, JobConfig.TIMEOUT, "100", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec", "exec",
                m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))));
        assertThat(config.getTimeout()).isEqualTo("0");
    }

    @Test
    void shouldSetTimeoutToNullIfSubmittedWithDefault() throws Exception {
        config.setConfigAttributes(m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.DEFAULT_TIMEOUT, JobConfig.TIMEOUT, "", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec", "exec",
                m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))));
        assertThat(config.getTimeout()).isNull();
    }

    @Test
    void shouldNotSetJobNameIfNotGiven() throws Exception {
        JobConfig config = new JobConfig("some-job-name");
        config.setConfigAttributes(m());
        assertThat(config.name()).isEqualTo(new CaseInsensitiveString("some-job-name"));
        config.setConfigAttributes(m(JobConfig.NAME, null));
        assertThat(config.name()).isNull();
    }

    @Test
    void shouldReturnAntTaskAsDefaultIfNoTasksSpecified() {
        JobConfig jobConfig = new JobConfig();
        assertThat(jobConfig.tasks()).hasSize(1);
        Task task = jobConfig.tasks().first();
        assertThat(task).isInstanceOf(NullTask.class);
    }

    @Test
    void shouldNotSetTasksIfNoTasksGiven() throws Exception {
        config = new JobConfig();
        AntTask task = new AntTask();
        task.setTarget("hello");
        config.addTask(task);
        config.setConfigAttributes(m());
        AntTask taskAfterUpdate = (AntTask) config.getTasks().get(0);
        assertThat(taskAfterUpdate.getTarget()).isEqualTo("hello");
        assertThat(config.getTasks().size()).isEqualTo(1);
        config.setConfigAttributes(m(JobConfig.TASKS, null));
        assertThat(config.getTasks().size()).isEqualTo(0);
    }

    @Test
    void shouldValidateTheJobName() {
        assertThat(createJobAndValidate(".name").errors().isEmpty()).isTrue();
        ConfigErrors configErrors = createJobAndValidate("name pavan").errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(JobConfig.NAME)).isEqualTo("Invalid job name 'name pavan'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is 255 characters.");
    }

    @Test
    void shouldFailValidationWhenJobNameIsEmpty() {
        ConfigErrors configErrors = createJobAndValidate(null).errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(JobConfig.NAME)).isEqualTo("Name is a required field");
    }

    @Test
    void shouldValidateTheJobNameAgainstHaving_runOnAll() {
        String jobName = "a-runOnAll-1";
        ConfigErrors configErrors = createJobAndValidate(jobName).errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(JobConfig.NAME)).isEqualTo(String.format("A job cannot have 'runOnAll' in it's name: %s because it is a reserved keyword", jobName));
    }

    @Test
    void shouldValidateTheJobNameAgainstHaving_runInstance() {
        String jobName = "a-runInstance-1";
        ConfigErrors configErrors = createJobAndValidate(jobName).errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(JobConfig.NAME)).isEqualTo(String.format("A job cannot have 'runInstance' in it's name: %s because it is a reserved keyword", jobName));
    }

    @Test
    void shouldValidateAgainstSettingRunInstanceCountToIncorrectValue() {
        JobConfig jobConfig1 = new JobConfig(new CaseInsensitiveString("test"));
        jobConfig1.setRunInstanceCount(-1);

        jobConfig1.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));

        ConfigErrors configErrors1 = jobConfig1.errors();
        assertThat(configErrors1.isEmpty()).isFalse();
        assertThat(configErrors1.on(JobConfig.RUN_TYPE)).isEqualTo("'Run Instance Count' cannot be a negative number as it represents number of instances GoCD needs to spawn during runtime.");

        JobConfig jobConfig2 = new JobConfig(new CaseInsensitiveString("test"));
        ReflectionUtil.setField(jobConfig2, "runInstanceCount", "abcd");

        jobConfig2.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));

        ConfigErrors configErrors2 = jobConfig2.errors();
        assertThat(configErrors2.isEmpty()).isFalse();
        assertThat(configErrors2.on(JobConfig.RUN_TYPE)).isEqualTo("'Run Instance Count' should be a valid positive integer as it represents number of instances GoCD needs to spawn during runtime.");

        JobConfig jobConfig3 = new JobConfig(new CaseInsensitiveString("test"));
        ReflectionUtil.setField(jobConfig3, "runInstanceCount", "0");

        jobConfig3.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));

        ConfigErrors configErrors3 = jobConfig3.errors();
        assertThat(configErrors3.isEmpty()).isFalse();
        assertThat(configErrors3.on(JobConfig.RUN_TYPE)).isEqualTo("'Run Instance Count' cannot be 0 as it represents number of instances GoCD needs to spawn during runtime.");
    }

    @Test
    void shouldValidateAgainstSettingRunOnAllAgentsAndRunInstanceCountSetTogether() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test"));
        jobConfig.setRunOnAllAgents(true);
        jobConfig.setRunInstanceCount(10);

        jobConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));

        ConfigErrors configErrors = jobConfig.errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(JobConfig.RUN_TYPE)).isEqualTo("Job cannot be 'run on all agents' type and 'run multiple instance' type together.");
    }

    @Test
    void shouldValidateEmptyAndNullResources() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithJobConfigs("pipeline1");
        JobConfig jobConfig = JobConfigMother.createJobConfigWithJobNameAndEmptyResources();
        ValidationContext validationContext = mock(ValidationContext.class);
        when(validationContext.getPipeline()).thenReturn(pipelineConfig);
        when(validationContext.getStage()).thenReturn(pipelineConfig.getFirstStageConfig());
        jobConfig.validate(validationContext);
        assertThat(jobConfig.errors().isEmpty()).isFalse();
        assertThat(jobConfig.errors().getAll().get(0)).isEqualTo("Empty resource name in job \"defaultJob\" of stage \"mingle\" of pipeline \"pipeline1\". If a template is used, please ensure that the resource parameters are defined for this pipeline.");
    }

    @Test
    void shouldValidateAgainstPresenceOfBothResourcesAndElasticProfileId() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithJobConfigs("pipeline1");
        JobConfig jobConfig = JobConfigMother.createJobConfigWithJobNameAndEmptyResources();
        ValidationContext validationContext = mock(ValidationContext.class);
        jobConfig.setElasticProfileId("docker.unit-test");

        when(validationContext.getPipeline()).thenReturn(pipelineConfig);
        when(validationContext.getStage()).thenReturn(pipelineConfig.getFirstStageConfig());

        jobConfig.validate(validationContext);

        assertThat(jobConfig.errors().isEmpty()).isFalse();
        assertThat(jobConfig.errors().on(JobConfig.ELASTIC_PROFILE_ID)).isEqualTo("Job cannot have both `resource` and `elasticProfileId`");
        assertThat(jobConfig.errors().on(JobConfig.RESOURCES)).isEqualTo("Job cannot have both `resource` and `elasticProfileId`");
    }

    @Test
    void shouldValidateElasticProfileId() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithJobConfigs("pipeline1");
        JobConfig jobConfig = JobConfigMother.createJobConfigWithJobNameAndEmptyResources();
        ValidationContext validationContext = mock(ValidationContext.class);
        jobConfig.setResourceConfigs(new ResourceConfigs());
        jobConfig.setElasticProfileId("non-existent-profile-id");

        when(validationContext.getPipeline()).thenReturn(pipelineConfig);
        when(validationContext.getStage()).thenReturn(pipelineConfig.getFirstStageConfig());
        when(validationContext.isValidProfileId("non-existent-profile-id")).thenReturn(false);

        jobConfig.validate(validationContext);

        assertThat(jobConfig.errors().isEmpty()).isFalse();
        assertThat(jobConfig.errors().on(JobConfig.ELASTIC_PROFILE_ID)).isEqualTo("No profile defined corresponding to profile_id 'non-existent-profile-id'");
    }

    @Test
    void shouldErrorOutIfTwoJobsHaveSameName() {
        HashMap<String, JobConfig> visitedConfigs = new HashMap<>();
        visitedConfigs.put("defaultJob".toLowerCase(), new JobConfig("defaultJob"));
        JobConfig defaultJob = new JobConfig("defaultJob");
        defaultJob.validateNameUniqueness(visitedConfigs);

        assertThat(defaultJob.errors().isEmpty()).isFalse();
        assertThat(defaultJob.errors().on(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'defaultJob'. Job names are case-insensitive and must be unique.");

        JobConfig defaultJobAllLowerCase = new JobConfig("defaultjob");
        defaultJobAllLowerCase.validateNameUniqueness(visitedConfigs);

        assertThat(defaultJobAllLowerCase.errors().isEmpty()).isFalse();
        assertThat(defaultJobAllLowerCase.errors().on(JobConfig.NAME)).isEqualTo("You have defined multiple jobs called 'defaultjob'. Job names are case-insensitive and must be unique.");
    }

    @Test
    void shouldNotValidateJobNameUniquenessInAbsenceOfName() {
        JobConfig job = new JobConfig();

        job.validateNameUniqueness(new HashMap<>());

        assertThat(job.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldNotValidateJobNameUniquenessIfNameIsEmptyString() {
        JobConfig job = new JobConfig(" ");

        job.validateNameUniqueness(new HashMap<>());

        assertThat(job.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldPopulateEnvironmentVariablesFromAttributeMap() {
        JobConfig jobConfig = new JobConfig();
        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("name", "FOO");
        valueHashMap.put("value", "BAR");
        map.put(JobConfig.ENVIRONMENT_VARIABLES, valueHashMap);
        EnvironmentVariablesConfig mockEnvironmentVariablesConfig = mock(EnvironmentVariablesConfig.class);
        jobConfig.setVariables(mockEnvironmentVariablesConfig);

        jobConfig.setConfigAttributes(map);

        verify(mockEnvironmentVariablesConfig).setConfigAttributes(valueHashMap);
    }

    @Test
    void shouldPopulateResourcesFromAttributeMap() {
        HashMap map = new HashMap();
        String value = "a,  b,c   ,d,e";
        map.put(JobConfig.RESOURCES, value);
        ResourceConfigs resourceConfigs = new ResourceConfigs();
        resourceConfigs.add(new ResourceConfig("z"));
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job-name"), resourceConfigs, null);

        jobConfig.setConfigAttributes(map);

        assertThat(jobConfig.resourceConfigs().size()).isEqualTo(5);
    }

    @Test
    void shouldPopulateTabsFromAttributeMap() {
        JobConfig jobConfig = new JobConfig("job-name");

        jobConfig.setConfigAttributes(m(JobConfig.TABS, a(m(Tab.NAME, "tab1", Tab.PATH, "path1"), m(Tab.NAME, "tab2", Tab.PATH, "path2"))));

        assertThat(jobConfig.getTabs().size()).isEqualTo(2);
        assertThat(jobConfig.getTabs().get(0).getName()).isEqualTo("tab1");
        assertThat(jobConfig.getTabs().get(1).getName()).isEqualTo("tab2");
        assertThat(jobConfig.getTabs().get(0).getPath()).isEqualTo("path1");
        assertThat(jobConfig.getTabs().get(1).getPath()).isEqualTo("path2");
    }

    @Test
    void shouldSetJobRunTypeCorrectly_forRails4() {
        // single instance
        HashMap map1 = new HashMap();
        map1.put(JobConfig.RUN_TYPE, JobConfig.RUN_SINGLE_INSTANCE);
        map1.put(JobConfig.RUN_INSTANCE_COUNT, "10"); // should be ignored
        JobConfig jobConfig1 = new JobConfig();

        jobConfig1.setConfigAttributes(map1);

        assertThat(jobConfig1.isRunOnAllAgents()).isFalse();
        assertThat(jobConfig1.isRunMultipleInstanceType()).isFalse();
        assertThat(jobConfig1.getRunInstanceCount()).isNull();

        // run on all agents
        HashMap map2 = new HashMap();
        map2.put(JobConfig.RUN_TYPE, JobConfig.RUN_ON_ALL_AGENTS);
        JobConfig jobConfig2 = new JobConfig();

        jobConfig2.setConfigAttributes(map2);

        assertThat(jobConfig2.isRunOnAllAgents()).isTrue();
        assertThat(jobConfig2.isRunMultipleInstanceType()).isFalse();
        assertThat(jobConfig2.getRunInstanceCount()).isNull();

        // run multiple instance
        HashMap map3 = new HashMap();
        map3.put(JobConfig.RUN_TYPE, JobConfig.RUN_MULTIPLE_INSTANCE);
        map3.put(JobConfig.RUN_INSTANCE_COUNT, "10");
        JobConfig jobConfig3 = new JobConfig();

        jobConfig3.setConfigAttributes(map3);

        assertThat(jobConfig3.isRunMultipleInstanceType()).isTrue();
        assertThat(jobConfig3.getRunInstanceCountValue()).isEqualTo(10);
        assertThat(jobConfig3.isRunOnAllAgents()).isFalse();

        HashMap map4 = new HashMap();
        map4.put(JobConfig.RUN_TYPE, JobConfig.RUN_MULTIPLE_INSTANCE);
        map4.put(JobConfig.RUN_INSTANCE_COUNT, "");
        JobConfig jobConfig4 = new JobConfig();

        jobConfig4.setConfigAttributes(map4);

        assertThat(jobConfig4.isRunMultipleInstanceType()).isFalse();
        assertThat(jobConfig4.getRunInstanceCount()).isNull();
        assertThat(jobConfig4.isRunOnAllAgents()).isFalse();
    }

    @Test
    void shouldResetJobRunTypeCorrectly() {
        HashMap map1 = new HashMap();
        map1.put(JobConfig.RUN_TYPE, JobConfig.RUN_MULTIPLE_INSTANCE);
        map1.put(JobConfig.RUN_INSTANCE_COUNT, "10");
        JobConfig jobConfig = new JobConfig();

        jobConfig.setConfigAttributes(map1);

        assertThat(jobConfig.getRunInstanceCountValue()).isEqualTo(10);
        assertThat(jobConfig.isRunMultipleInstanceType()).isTrue();
        assertThat(jobConfig.isRunOnAllAgents()).isFalse();

        // should not reset value when correct key not present
        HashMap map2 = new HashMap();

        jobConfig.setConfigAttributes(map2);

        assertThat(jobConfig.getRunInstanceCountValue()).isEqualTo(10);
        assertThat(jobConfig.isRunMultipleInstanceType()).isTrue();
        assertThat(jobConfig.isRunOnAllAgents()).isFalse();

        // reset value for same job config
        HashMap map3 = new HashMap();
        map3.put(JobConfig.RUN_TYPE, JobConfig.RUN_SINGLE_INSTANCE);

        jobConfig.setConfigAttributes(map3);

        assertThat(jobConfig.isRunMultipleInstanceType()).isFalse();
        assertThat(jobConfig.getRunInstanceCount()).isNull();
        assertThat(jobConfig.isRunOnAllAgents()).isFalse();
    }

    @Test
    void shouldPopulateArtifactPlansFromAttributeMap() {
        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("src", "dest");
        valueHashMap.put("src1", "dest1");
        map.put(JobConfig.ARTIFACT_CONFIGS, valueHashMap);
        ArtifactTypeConfigs mockArtifactTypeConfigs = mock(ArtifactTypeConfigs.class);
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job-name"), new ResourceConfigs(), mockArtifactTypeConfigs);

        jobConfig.setConfigAttributes(map);

        verify(mockArtifactTypeConfigs).setConfigAttributes(valueHashMap);
    }

    @Test
    void shouldValidateThatTheTimeoutIsAValidNumber() {
        JobConfig job = new JobConfig("job");
        job.setTimeout("5.5");
        job.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(job.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldMarkJobInvalidIfTimeoutIsNotAValidNumber() {
        JobConfig job = new JobConfig("job");
        job.setTimeout("5.5MN");
        job.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(job.errors().isEmpty()).isFalse();
        assertThat(job.errors().on(JobConfig.TIMEOUT)).isEqualTo("Timeout should be a valid number as it represents number of minutes");
    }

    @Test
    void shouldReturnTimeoutType() {
        JobConfig job = new JobConfig("job");
        assertThat(job.getTimeoutType()).isEqualTo(JobConfig.DEFAULT_TIMEOUT);
        job.setTimeout("0");
        assertThat(job.getTimeoutType()).isEqualTo(JobConfig.NEVER_TIMEOUT);
        job.setTimeout("10");
        assertThat(job.getTimeoutType()).isEqualTo(JobConfig.OVERRIDE_TIMEOUT);
    }

    @Test
    void shouldReturnRunTypeCorrectly() {
        JobConfig job = new JobConfig("job");
        assertThat(job.getRunType()).isEqualTo(JobConfig.RUN_SINGLE_INSTANCE);
        job.setRunOnAllAgents(true);
        assertThat(job.getRunType()).isEqualTo(JobConfig.RUN_ON_ALL_AGENTS);
        job.setRunOnAllAgents(false);
        job.setRunInstanceCount(10);
        assertThat(job.getRunType()).isEqualTo(JobConfig.RUN_MULTIPLE_INSTANCE);
    }

    @Test
    void shouldErrorOutWhenTimeoutIsANegativeNumber() {
        JobConfig jobConfig = new JobConfig("job");
        jobConfig.setTimeout("-1");
        jobConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));

        assertThat(jobConfig.errors().isEmpty()).isFalse();
        assertThat(jobConfig.errors().on(JobConfig.TIMEOUT)).isEqualTo("Timeout cannot be a negative number as it represents number of minutes");
    }

    @Test
    void shouldValidateTree() {
        ResourceConfigs resourceConfigs = mock(ResourceConfigs.class);
        when(resourceConfigs.iterator()).thenReturn(new ResourceConfigs().iterator());
        ArtifactTypeConfigs artifactTypeConfigs = mock(ArtifactTypeConfigs.class);
        Tasks tasks = mock(Tasks.class);
        Tabs tabs = mock(Tabs.class);
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        when(tasks.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(resourceConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(artifactTypeConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(tabs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        when(variables.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);

        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"), resourceConfigs, artifactTypeConfigs, tasks);
        jobConfig.setTabs(tabs);
        jobConfig.setVariables(variables);

        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), new StageConfig(), jobConfig);
        assertThat(jobConfig.validateTree(context)).isTrue();

        ArgumentCaptor<PipelineConfigSaveValidationContext> captor = ArgumentCaptor.forClass(PipelineConfigSaveValidationContext.class);
        verify(tasks).validateTree(captor.capture());
        PipelineConfigSaveValidationContext childContext = captor.getValue();
        assertThat(childContext.getParent()).isEqualTo(jobConfig);
        verify(resourceConfigs).validateTree(childContext);
        verify(artifactTypeConfigs).validateTree(childContext);
        verify(tabs).validateTree(childContext);
        verify(variables).validateTree(childContext);
    }

    @Test
    void shouldFailValidationIfAnyDescendentIsInvalid() {
        ResourceConfigs resourceConfigs = mock(ResourceConfigs.class);
        when(resourceConfigs.iterator()).thenReturn(new ResourceConfigs().iterator());
        ArtifactTypeConfigs artifactTypeConfigs = mock(ArtifactTypeConfigs.class);
        Tasks tasks = mock(Tasks.class);
        Tabs tabs = mock(Tabs.class);
        EnvironmentVariablesConfig variables = mock(EnvironmentVariablesConfig.class);
        when(tasks.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(resourceConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(artifactTypeConfigs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(tabs.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        when(variables.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);

        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"), resourceConfigs, artifactTypeConfigs, tasks);
        jobConfig.setTabs(tabs);
        jobConfig.setVariables(variables);

        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(), new StageConfig(), jobConfig);
        assertThat(jobConfig.validateTree(context)).isFalse();

        ArgumentCaptor<PipelineConfigSaveValidationContext> captor = ArgumentCaptor.forClass(PipelineConfigSaveValidationContext.class);
        verify(tasks).validateTree(captor.capture());
        PipelineConfigSaveValidationContext childContext = captor.getValue();
        assertThat(childContext.getParent()).isEqualTo(jobConfig);
        verify(resourceConfigs).validateTree(childContext);
        verify(artifactTypeConfigs).validateTree(childContext);
        verify(tabs).validateTree(childContext);
        verify(variables).validateTree(childContext);
    }

    @Test
    void shouldValidateAgainstSettingRunOnAllAgentsForAJobAssignedToElasticAgent() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test"));
        jobConfig.setRunOnAllAgents(true);
        jobConfig.setElasticProfileId("ubuntu-dev");

        jobConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));

        ConfigErrors configErrors = jobConfig.errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(JobConfig.RUN_TYPE)).isEqualTo("Job cannot be set to 'run on all agents' when assigned to an elastic agent");
    }

    @Test
    void shouldEncryptSecurePropertiesForOnlyFetchExternalArtifactTask() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
        FetchPluggableArtifactTask mockFetchExternalArtifactTask = mock(FetchPluggableArtifactTask.class);
        jobConfig.addTask(mockFetchExternalArtifactTask);

        jobConfig.encryptSecureProperties(new BasicCruiseConfig(), new PipelineConfig(), jobConfig);

        verify(mockFetchExternalArtifactTask).encryptSecureProperties(any(CruiseConfig.class), any(PipelineConfig.class), any(FetchPluggableArtifactTask.class));
    }

    @Nested
    class RunInstanceCount {
        @Test
        void shouldThrowExceptionIsSingleInstanceJobType() {
            JobConfig jobConfig = new JobConfig("job");

            assertThatCode(jobConfig::getRunInstanceCountValue)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("This job config is a single instance type job.");
            assertThat(jobConfig.isRunMultipleInstanceType()).isFalse();
        }

        @Test
        void shouldNotThrowExceptionIfMultipleInstanceJobType() {
            JobConfig jobConfig = new JobConfig("job");
            jobConfig.setRunInstanceCount(10);

            assertThat(jobConfig.getRunInstanceCountValue()).isEqualTo(10);
            assertThat(jobConfig.isRunMultipleInstanceType()).isTrue();
        }
    }

    private JobConfig createJobAndValidate(final String name) {
        JobConfig jobConfig = new JobConfig(name);
        jobConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        return jobConfig;
    }
}
