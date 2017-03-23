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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ExecTaskTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void describeTest() {
        ExecTask task = new ExecTask("ant", "-f build.xml run", "subfolder");
        task.setTimeout(600);
        assertThat(task.describe(),
                Is.is("<exec command=\"ant\" args=\"-f build.xml run\" workingdir=\"subfolder\" timeout=\"600\" />"));
    }

    @Test
    public void describeMutipleArgumentsTest() {
        ExecTask task = new ExecTask("echo", null, new Arguments(new Argument("abc"), new Argument("hello baby!")));
        task.setTimeout(600);
        assertThat(task.describe(),
                Is.is("<exec command=\"echo\" timeout=\"600\" >\n<arg>abc</arg>\n<arg>hello baby!</arg>\n</exec>"));
    }

    @Test
    public void shouldValidateConfig() throws Exception {
        ExecTask execTask = new ExecTask("arg1 arg2", new Arguments(new Argument("arg1"), new Argument("arg2")));
        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(execTask.errors().isEmpty(), is(false));
        assertThat(execTask.errors().on(ExecTask.ARGS), is(ExecTask.EXEC_CONFIG_ERROR));
        assertThat(execTask.errors().on(ExecTask.ARG_LIST_STRING), is(ExecTask.EXEC_CONFIG_ERROR));
    }

    @Test
    public void shouldAddErrorsOfEachArgumentToTheParent() {
        Argument argument = new Argument("invalid-argument");
        argument.addError(ExecTask.ARG_LIST_STRING, "Invalid argument");
        ExecTask execTask = new ExecTask("echo", new Arguments(argument), null);
        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(execTask.errors().on(ExecTask.ARG_LIST_STRING), is("Invalid argument"));
    }

    @Test
    public void shouldBeValid() throws Exception {
        ExecTask execTask = new ExecTask("", new Arguments(new Argument("arg1"), new Argument("arg2")));
        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(execTask.errors().isEmpty(), is(true));
        execTask = new ExecTask("command", "", "blah");
        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(execTask.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldValidateWorkingDirectory() {
        ExecTask task = new ExecTask("ls", "-l", "../../../assertTaskInvalid");
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline");
        PipelineConfig pipeline = config.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        StageConfig stage = pipeline.get(0);
        JobConfig job = stage.getJobs().get(0);
        job.addTask(task);

        List<ConfigErrors> errors = config.validateAfterPreprocess();
        assertThat(errors.size(), is(1));
        String message = "The path of the working directory for the custom command in job 'job' in stage 'stage' of pipeline 'pipeline' is outside the agent sandbox.";
        assertThat(errors.get(0).firstError(), is(message));
        assertThat(task.errors().on(ExecTask.WORKING_DIR), is(message));
    }

    @Test
    public void shouldAllowSettingOfConfigAttributes() throws Exception {
        ExecTask exec = new ExecTask();
        exec.setConfigAttributes(m(ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "my_dir"));
        assertThat(exec.command(), is("ls"));
        assertThat(exec.getArgs(), is("-la"));
        assertThat(exec.getArgListString(), is(""));
        assertThat(exec.workingDirectory(), is("my_dir"));

        exec.setConfigAttributes(m(ExecTask.COMMAND, null, ExecTask.ARGS, null, ExecTask.WORKING_DIR, null));
        assertThat(exec.command(), is(nullValue()));
        assertThat(exec.getArgs(), is(""));
        assertThat(exec.workingDirectory(), is(nullValue()));

        exec.setConfigAttributes(m(ExecTask.COMMAND, null, ExecTask.ARG_LIST_STRING, "-l\n-a\npavan's\\n working dir?", ExecTask.WORKING_DIR, null));
        assertThat(exec.command(), is(nullValue()));
        assertThat(exec.getArgListString(), is("-l\n-a\npavan's\\n working dir?"));
        assertThat(exec.getArgList().size(), is(3));
        assertThat(exec.getArgList().get(0), is(new Argument("-l")));
        assertThat(exec.getArgList().get(1), is(new Argument("-a")));
        assertThat(exec.getArgList().get(2), is(new Argument("pavan's\\n working dir?")));
        assertThat(exec.workingDirectory(), is(nullValue()));
    }

    @Test
    public void shouldNotSetAttributesWhenKeysNotPresentInAttributeMap() throws Exception {
        ExecTask exec = new ExecTask();
        exec.setConfigAttributes(m(ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "my_dir"));
        exec.setConfigAttributes(m());//Key is not present
        assertThat(exec.command(), is("ls"));
        assertThat(exec.getArgs(), is("-la"));
        assertThat(exec.workingDirectory(), is("my_dir"));
    }

    @Test
    public void shouldNotSetArgsIfTheValueIsBlank() throws Exception {
        ExecTask exec = new ExecTask();
        exec.setConfigAttributes(m(ExecTask.COMMAND, "ls", ExecTask.ARGS, "", ExecTask.WORKING_DIR, "my_dir"));
        exec.setConfigAttributes(m());
        assertThat(exec.command(), is("ls"));
        assertThat(exec.getArgList().size(), is(0));
        assertThat(exec.workingDirectory(), is("my_dir"));
    }

    @Test
    public void shouldNullOutWorkingDirectoryIfGivenBlank() {
        ExecTask exec = new ExecTask("ls", "-la", "foo");
        exec.setConfigAttributes(m(ExecTask.COMMAND, "", ExecTask.ARGS, "", ExecTask.WORKING_DIR, ""));
        assertThat(exec.command(), is(""));
        assertThat(exec.getArgs(), is(""));
        assertThat(exec.workingDirectory(), is(nullValue()));
    }

    @Test
    public void shouldPopulateAllAttributesOnPropertiesForDisplay() {
        ExecTask execTask = new ExecTask("ls", "-la", "holy/dir");
        execTask.setTimeout(10l);
        assertThat(execTask.getPropertiesForDisplay(), hasItems(new TaskProperty("COMMAND", "ls", "command"), new TaskProperty("ARGUMENTS", "-la", "arguments"), new TaskProperty("WORKING_DIR", "holy/dir", "working_dir"), new TaskProperty("TIMEOUT", "10", "timeout")));
        assertThat(execTask.getPropertiesForDisplay().size(), is(4));

        execTask = new ExecTask("ls", new Arguments(new Argument("-la"), new Argument("/proc")), "holy/dir");
        execTask.setTimeout(10l);
        assertThat(execTask.getPropertiesForDisplay(), hasItems(new TaskProperty("COMMAND", "ls", "command"), new TaskProperty("ARGUMENTS", "-la /proc", "arguments"), new TaskProperty("WORKING_DIR", "holy/dir", "working_dir"), new TaskProperty("TIMEOUT", "10", "timeout")));
        assertThat(execTask.getPropertiesForDisplay().size(), is(4));

        execTask = new ExecTask("ls", new Arguments(new Argument()), null);
        assertThat(execTask.getPropertiesForDisplay(), hasItems(new TaskProperty("COMMAND", "ls", "command")));
        assertThat(execTask.getPropertiesForDisplay().size(), is(1));

        execTask = new ExecTask("ls", "", (String) null);
        assertThat(execTask.getPropertiesForDisplay(), hasItems(new TaskProperty("COMMAND", "ls", "command")));
        assertThat(execTask.getPropertiesForDisplay().size(), is(1));
    }

    @Test
    public void shouldErrorOutForTemplates_WhenItHasATaskWithInvalidWorkingDirectory() {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("some_pipeline");
        StageConfig templateStage = StageConfigMother.stageWithTasks("templateStage");
        ExecTask execTask = new ExecTask("ls", "-la", "/");
        templateStage.getJobs().first().addTask(execTask);
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template_name"), templateStage);
        cruiseConfig.addTemplate(template);

        try {
            execTask.validateTask(ConfigSaveValidationContext.forChain(cruiseConfig, template, templateStage, templateStage.getJobs().first()));
            assertThat(execTask.errors().isEmpty(), is(false));
            assertThat(execTask.errors().on(ExecTask.WORKING_DIR), is("The path of the working directory for the custom command in job 'job' in stage 'templateStage' of template 'template_name' is outside the agent sandbox."));
        } catch (Exception e) {
            fail("should not have failed. Exception: " + e.getMessage());
        }
    }

    @Test
    public void shouldReturnCommandTaskAttributes(){
        ExecTask task = new ExecTask("ls", "-laht", "src/build");
        assertThat(task.command(),is("ls"));
        assertThat(task.arguments(), is("-laht"));
        assertThat(task.workingDirectory(), is("src/build"));
    }

    @Test
    public void shouldReturnCommandArgumentList(){
        ExecTask task = new ExecTask("./bn", new Arguments(new Argument("clean"), new Argument("compile"), new Argument("\"buildfile\"")), "src/build" );
        assertThat(task.arguments(),is("clean compile \"buildfile\""));
    }

    @Test
    public void shouldReturnEmptyCommandArguments(){
        ExecTask task = new ExecTask("./bn", new Arguments(), "src/build" );
        assertThat(task.arguments(),is(""));
    }

    @Test
    public void shouldBeSameIfCommandMatches() {
        ExecTask task = new ExecTask("ls", new Arguments());

        assertTrue(task.equals(new ExecTask("ls", new Arguments())));
    }

    @Test
    public void shouldUnEqualIfCommandsDontMatch() {
        ExecTask task = new ExecTask("ls", new Arguments());

        assertFalse(task.equals(new ExecTask("rm", new Arguments())));
    }

    @Test
    public void shouldUnEqualIfCommandIsNull() {
        ExecTask task = new ExecTask(null, new Arguments());

        assertFalse(task.equals(new ExecTask("rm", new Arguments())));
    }

    @Test
    public void shouldUnEqualIfOtherTaskCommandIsNull() {
        ExecTask task = new ExecTask("ls", new Arguments());

        assertFalse(task.equals(new ExecTask(null, new Arguments())));
    }
}
