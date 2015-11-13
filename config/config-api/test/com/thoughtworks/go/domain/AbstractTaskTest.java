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

package com.thoughtworks.go.domain;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.service.TaskFactory;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractTaskTest {
    private TaskFactory taskFactory = mock(TaskFactory.class);

    @Test
    public void shouldKnowTheTypeOfExecTask() {
        assertThat(new ExecTask().getTaskType(), is("exec"));
        assertThat(new FetchTask().getTaskType(), is("fetch"));
    }

    @Test
    public void shouldSetConfigAttributes() {
        AbstractTask task = new ExecTask();
        Map attributes = new HashMap();
        attributes.put(AbstractTask.RUN_IF_CONFIGS_ANY, "1");
        attributes.put(AbstractTask.RUN_IF_CONFIGS_FAILED, "1");
        attributes.put(AbstractTask.RUN_IF_CONFIGS_PASSED, "1");
        task.setConfigAttributes(attributes);
        assertThat(task.getConditions().match(RunIfConfig.ANY), is(true));
        assertThat(task.getConditions().match(RunIfConfig.FAILED), is(true));
        assertThat(task.getConditions().match(RunIfConfig.PASSED), is(true));
        assertThat(task.hasCancelTask(), is(false));
    }

    @Test
    public void shouldSetOnCancelExecTask() {
        AbstractTask task = new ExecTask();

        Map onCancelMapAttrib = new HashMap();
        onCancelMapAttrib.put(ExecTask.COMMAND, "sudo");
        onCancelMapAttrib.put(ExecTask.ARGS, "ls -la");
        onCancelMapAttrib.put(ExecTask.WORKING_DIR, "working_dir");
        onCancelMapAttrib.put(AbstractTask.RUN_IF_CONFIGS_ANY, "1");
        onCancelMapAttrib.put(AbstractTask.RUN_IF_CONFIGS_FAILED, "1");
        onCancelMapAttrib.put(AbstractTask.RUN_IF_CONFIGS_PASSED, "1");

        Map onCancelConfigAttributes = new HashMap();
        onCancelConfigAttributes.put(OnCancelConfig.EXEC_ON_CANCEL, onCancelMapAttrib);
        onCancelConfigAttributes.put(OnCancelConfig.ON_CANCEL_OPTIONS, "exec");

        Map actualTaskAttributes = new HashMap();
        actualTaskAttributes.put(AbstractTask.ON_CANCEL_CONFIG, onCancelConfigAttributes);
        actualTaskAttributes.put(AbstractTask.HAS_CANCEL_TASK, "1");


        ExecTask execTask = new ExecTask();
        when(taskFactory.taskInstanceFor(execTask.getTaskType())).thenReturn(execTask);
        task.setConfigAttributes(actualTaskAttributes, taskFactory);

        assertThat(task.hasCancelTask(), is(true));

        ExecTask expected = new ExecTask("sudo", "ls -la", "working_dir");
        expected.getConditions().add(RunIfConfig.ANY);
        expected.getConditions().add(RunIfConfig.FAILED);
        expected.getConditions().add(RunIfConfig.PASSED);
        assertThat((ExecTask) task.cancelTask(), is(expected));
    }

    @Test
    public void shouldBeAbleToRemoveOnCancelConfig() {
        AbstractTask task = new ExecTask();
        task.setCancelTask(new ExecTask());

        Map cancelTaskAttributes = new HashMap();
        cancelTaskAttributes.put(ExecTask.COMMAND, "ls");
        cancelTaskAttributes.put(ExecTask.ARG_LIST_STRING, "-la");

        Map onCancelConfigAttributes = new HashMap();
        onCancelConfigAttributes.put(OnCancelConfig.EXEC_ON_CANCEL, cancelTaskAttributes);

        Map cancelConfigAttributes = new HashMap();
        cancelConfigAttributes.put(OnCancelConfig.ON_CANCEL_OPTIONS, "exec");

        Map actualTaskAttributes = new HashMap();
        actualTaskAttributes.put(AbstractTask.HAS_CANCEL_TASK, "0");
        actualTaskAttributes.put(AbstractTask.ON_CANCEL_CONFIG, cancelConfigAttributes);

        task.setConfigAttributes(actualTaskAttributes);

        assertThat(task.hasCancelTask(), is(false));
    }

    @Test
    public void shouldResetRunifConfigsWhenTheConfigIsNotPresent() {
        AbstractTask task = new ExecTask();
        task.getConditions().add(RunIfConfig.ANY);
        task.getConditions().add(RunIfConfig.PASSED);
        Map attributes = new HashMap();
        attributes.put(AbstractTask.RUN_IF_CONFIGS_ANY, "0");
        attributes.put(AbstractTask.RUN_IF_CONFIGS_FAILED, "1");
        attributes.put(AbstractTask.RUN_IF_CONFIGS_PASSED, "0");
        task.setConfigAttributes(attributes);
        assertThat(task.getConditions().match(RunIfConfig.ANY), is(false));
        assertThat(task.getConditions().match(RunIfConfig.FAILED), is(true));
        assertThat(task.getConditions().match(RunIfConfig.PASSED), is(false));
    }

    @Test
    public void validate_shouldErrorOutWhenAnOncancelTaskHasAnOncancelTask() {
        AbstractTask task = new ExecTask();
        ExecTask onCancelTask = new ExecTask();
        onCancelTask.setCancelTask(new ExecTask());
        task.setCancelTask(onCancelTask);

        task.validate(null);

        assertThat(task.errors().isEmpty(), is(false));
        assertThat(task.errors().on(AbstractTask.ON_CANCEL_CONFIG), is("Cannot nest 'oncancel' within a cancel task"));
    }

    @Test
    public void validate_shouldBeValidNoOncancelTaskIsDefined() {
        AbstractTask task = new ExecTask("ls", "-la", "foo");

        task.validate(null);

        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void validate_shouldBeValidWhenOncancelTaskIsNotNested() {
        AbstractTask task = new ExecTask("ls", "-la", "foo");
        task.setCancelTask(new ExecTask());

        task.validate(null);

        assertThat(task.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldReturnCommaSeparatedRunIfConfigsConditionsForDisplay() {
        AbstractTask execTask = new ExecTask("ls", "-la", "42");
        execTask.getConditions().add(RunIfConfig.PASSED);
        execTask.getConditions().add(RunIfConfig.FAILED);
        execTask.getConditions().add(RunIfConfig.ANY);

        String actual = execTask.getConditionsForDisplay();

        assertThat(actual, is("Passed, Failed, Any"));
    }

    @Test
    public void shouldReturnPassedByDefaultWhenNoRunIfConfigIsSpecified() {
        AbstractTask execTask = new ExecTask("ls", "-la", "42");
        assertThat(execTask.getConditionsForDisplay(), is("Passed"));
    }

    @Test
    public void shouldValidateTree(){
        String pipelineName = "p1";
        PipelineConfig pipelineConfig = GoConfigMother.configWithPipelines(pipelineName).pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        StageConfig stageConfig = pipelineConfig.getStages().get(0);
        JobConfig jobConfig = stageConfig.getJobs().get(0);

        AbstractTask execTask = new ExecTask("ls", "-la", "42");
        AntTask antTask = new AntTask();
        antTask.setWorkingDirectory("/abc");
        execTask.setCancelTask(antTask);
        PipelineConfigSaveValidationContext context = PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, stageConfig, jobConfig);
        assertThat(execTask.validateTree(context), is(false));
        assertThat(antTask.errors().isEmpty(), is(false));
        assertThat(antTask.errors().get(AntTask.WORKING_DIRECTORY).size(), is(1));
        assertThat(antTask.errors().get(AntTask.WORKING_DIRECTORY).contains("Task of job 'job' in stage 'stage' of pipeline 'p1' has path '/abc' which is outside the working directory."), is(true));
    }

}
