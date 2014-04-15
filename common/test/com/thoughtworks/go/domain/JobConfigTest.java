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

package com.thoughtworks.go.domain;

import java.util.HashMap;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.config.Tab;
import com.thoughtworks.go.config.Tasks;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.DataStructureUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.TestUtils.sizeIs;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobConfigTest {
    JobConfig config;

    @Before
    public void setup(){
        config = new JobConfig();
        Tasks tasks = mock(Tasks.class);
        config.injectTasksForTest(tasks);
        doNothing().when(tasks).setConfigAttributes(Matchers.<Object>anyObject(), Matchers.<TaskFactory>any());
    }
    
    @Test
    public void shouldCopyAttributeValuesFromAttributeMap() throws Exception {
        config = new JobConfig();//override the setup mock
        TaskFactory taskFactory = mock(TaskFactory.class);
        ExecTask emptyExecTask = new ExecTask();
        when(taskFactory.taskInstanceFor(emptyExecTask.getTaskType())).thenReturn(emptyExecTask);

        config.setConfigAttributes(DataStructureUtils.m(JobConfig.NAME, "foo-job", JobConfig.TASKS, DataStructureUtils.m(Tasks.TASK_OPTIONS, "exec", "exec",
                DataStructureUtils.m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))), taskFactory);
        assertThat(config.name(), is(new CaseInsensitiveString("foo-job")));
        assertThat(config.getTasks().get(0), is((Task) new ExecTask("ls", "-la", "/tmp")));
        assertThat(config.getTasks().size(), is(1));
    }

    @Test
    public void shouldSetTimeoutIfSpecified() throws Exception {
        config.setConfigAttributes(
                m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.OVERRIDE_TIMEOUT, JobConfig.TIMEOUT, "100", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec", "exec",
                        m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))));
        assertThat(config.getTimeout(), is("100"));
    }

    @Test
    public void shouldClearTimeoutIfSubmittedWithEmptyValue() throws Exception {
        config.setConfigAttributes(
                m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.OVERRIDE_TIMEOUT, JobConfig.TIMEOUT, "", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec",
                        "exec", m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))));
        assertThat(config.getTimeout(), is(nullValue()));
    }

    @Test
    public void shouldSetTimeoutToZeroIfSubmittedWithNever() throws Exception {
        config.setConfigAttributes(m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.NEVER_TIMEOUT, JobConfig.TIMEOUT, "100", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec", "exec",
                m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))));
        assertThat(config.getTimeout(), is("0"));
    }

    @Test
    public void shouldSetTimeoutToNullIfSubmittedWithDefault() throws Exception {
        config.setConfigAttributes(m(JobConfig.NAME, "foo-job", "timeoutType", JobConfig.DEFAULT_TIMEOUT, JobConfig.TIMEOUT, "", JobConfig.TASKS, m(Tasks.TASK_OPTIONS, "exec", "exec",
                m(Task.TASK_TYPE, "exec", ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "/tmp"))));
        assertThat(config.getTimeout(), is(nullValue()));
    }

    @Test
    public void shouldNotSetJobNameIfNotGiven() throws Exception {
        JobConfig config = new JobConfig("some-job-name");
        config.setConfigAttributes(m());
        assertThat(config.name(), is(new CaseInsensitiveString("some-job-name")));
        config.setConfigAttributes(m(JobConfig.NAME, null));
        assertThat(config.name(), is(nullValue()));
    }

    @Test
    public void shouldReturnAntTaskAsDefaultIfNoTasksSpecified() {
        JobConfig jobConfig = new JobConfig();
        assertThat(jobConfig.tasks(), sizeIs(1));
        Task task = jobConfig.tasks().first();
        assertThat(task, instanceOf(NullTask.class));
    }

    @Test
    public void shouldNotSetTasksIfNoTasksGiven() throws Exception {
        config = new JobConfig();
        AntTask task = new AntTask();
        task.setTarget("hello");
        config.addTask(task);
        config.setConfigAttributes(m());
        AntTask taskAfterUpdate = (AntTask) config.getTasks().get(0);
        assertThat(taskAfterUpdate.getTarget(), is("hello"));
        assertThat(config.getTasks().size(), is(1));
        config.setConfigAttributes(m(JobConfig.TASKS, null));
        assertThat(config.getTasks().size(), is(0));
    }

    @Test
    public void shouldValidateTheJobName() {
        assertThat(createJobAndValidate(".name").errors().isEmpty(), is(true));
        ConfigErrors configErrors = createJobAndValidate("name pavan").errors();
        assertThat(configErrors.isEmpty(), is(false));
        assertThat(configErrors.on(JobConfig.NAME), is("Invalid job name 'name pavan'. This must be alphanumeric and can contain underscores and periods. The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldValidateEmptyAndNullResources()
    {
        PipelineConfig pipelineConfig=PipelineConfigMother.CreatePipelineConfigWithJobConfigs("pipeline1");
        JobConfig jobConfig = JobConfigMother.createJobConfigWithJobNameAndEmptyResources();
        ValidationContext validationContext=mock(ValidationContext.class);
        when(validationContext.getPipeline()).thenReturn(pipelineConfig);
        when(validationContext.getStage()).thenReturn(pipelineConfig.getFirstStageConfig());
        jobConfig.validate(validationContext);
        assertThat(jobConfig.errors().isEmpty(), is(false));
        assertThat(jobConfig.errors().getAll().get(0),is("Empty resource name in job \"defaultJob\" of stage \"mingle\" of pipeline \"pipeline1\". If a template is used, please ensure that the resource parameters are defined for this pipeline."));
    }

    @Test
    public void shouldErrorOutIfTwoJobsHaveSameName() {
        HashMap<String, JobConfig> visitedConfigs = new HashMap<String, JobConfig>();
        visitedConfigs.put("defaultJob".toLowerCase(), new JobConfig("defaultJob"));
        JobConfig defaultJob = new JobConfig("defaultJob");
        defaultJob.validateNameUniqueness(visitedConfigs);

        assertThat(defaultJob.errors().isEmpty(), is(false));
        assertThat(defaultJob.errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'defaultJob'. Job names are case-insensitive and must be unique."));

        JobConfig defaultJobAllLowerCase = new JobConfig("defaultjob");
        defaultJobAllLowerCase.validateNameUniqueness(visitedConfigs);

        assertThat(defaultJobAllLowerCase.errors().isEmpty(), is(false));
        assertThat(defaultJobAllLowerCase.errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'defaultjob'. Job names are case-insensitive and must be unique."));
    }

    @Test
    public void shouldPopulateEnvironmentVariablesFromAttributeMap() {
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
    public void shouldPopulateResourcesFromAttributeMap() {
        HashMap map = new HashMap();
        String value = "a,  b,c   ,d,e";
        map.put(JobConfig.RESOURCES, value);
        Resources resources = new Resources();
        resources.add(new Resource("z"));
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job-name"), resources, null);

        jobConfig.setConfigAttributes(map);

        assertThat(jobConfig.resources().size(), is(5));
    }

    @Test
    public void shouldPopulateTabsFromAttributeMap() {
        JobConfig jobConfig = new JobConfig("job-name");

        jobConfig.setConfigAttributes(m(JobConfig.TABS, a(m(Tab.NAME, "tab1", Tab.PATH, "path1"), m(Tab.NAME, "tab2", Tab.PATH, "path2"))));

        assertThat(jobConfig.getTabs().size(), is(2));
        assertThat(jobConfig.getTabs().get(0).getName(), is("tab1"));
        assertThat(jobConfig.getTabs().get(1).getName(), is("tab2"));
        assertThat(jobConfig.getTabs().get(0).getPath(), is("path1"));
        assertThat(jobConfig.getTabs().get(1).getPath(), is("path2"));
    }

    @Test
    public void shouldSetPrimitiveAttributes() {
        HashMap map = new HashMap();
        map.put(JobConfig.RUN_ON_ALL_AGENTS, "1");
        JobConfig jobConfig = new JobConfig();

        jobConfig.setConfigAttributes(map);

        assertThat(jobConfig.isRunOnAllAgents(), is(true));
    }

    @Test
    public void shouldPopulateArtifactPlansFromAttributeMap() {
        HashMap map = new HashMap();
        HashMap valueHashMap = new HashMap();
        valueHashMap.put("src", "dest");
        valueHashMap.put("src1", "dest1");
        map.put(JobConfig.ARTIFACT_PLANS, valueHashMap);
        ArtifactPlans mockArtifactPlans = mock(ArtifactPlans.class);
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job-name"), new Resources(), mockArtifactPlans);

        jobConfig.setConfigAttributes(map);

        verify(mockArtifactPlans).setConfigAttributes(valueHashMap);
    }

    @Test
    public void shouldValidateThatTheTimeoutIsAValidNumber() {
        JobConfig job = new JobConfig("job");
        job.setTimeout("5.5");
        job.validate(ValidationContext.forChain(new CruiseConfig()));
        assertThat(job.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldMarkJobInvalidIfTimeoutIsNotAValidNumber() {
        JobConfig job = new JobConfig("job");
        job.setTimeout("5.5MN");
        job.validate(ValidationContext.forChain(new CruiseConfig()));
        assertThat(job.errors().isEmpty(), is(false));
        assertThat(job.errors().on(JobConfig.TIMEOUT), is("Timeout should be a valid number as it represents number of minutes"));
    }

    @Test
    public void shouldReturnTimeoutType() {
        JobConfig job = new JobConfig("job");
        assertThat(job.getTimeoutType(), is(JobConfig.DEFAULT_TIMEOUT));
        job.setTimeout("0");
        assertThat(job.getTimeoutType(), is(JobConfig.NEVER_TIMEOUT));
        job.setTimeout("10");
        assertThat(job.getTimeoutType(), is(JobConfig.OVERRIDE_TIMEOUT));
    }

    @Test
    public void shouldErrorOutWhenTimeoutIsANegativeNumber() {
        JobConfig jobConfig = new JobConfig("job");
        jobConfig.setTimeout("-1");
        jobConfig.validate(ValidationContext.forChain(new CruiseConfig()));

        assertThat(jobConfig.errors().isEmpty(), is(false));
        assertThat(jobConfig.errors().on(JobConfig.TIMEOUT), is("Timeout cannot be a negative number as it represents number of minutes"));
    }

    private JobConfig createJobAndValidate(final String name) {
        JobConfig jobConfig = new JobConfig(name);
        jobConfig.validate(ValidationContext.forChain(new CruiseConfig()));
        return jobConfig;
    }
}
