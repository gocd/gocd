/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ExecTaskTest {

    @Test
    public void describeTest() {
        ExecTask task = new ExecTask("ant", "-f build.xml run", "subfolder");
        task.setTimeout(600);
        assertThat(task.describe()).isEqualTo("ant -f build.xml run");
    }

    @Test
    public void describeMultipleArgumentsTest() {
        ExecTask task = new ExecTask("echo", null, new Arguments(new Argument("abc"), new Argument("hello baby!")));
        task.setTimeout(600);
        assertThat(task.describe()).isEqualTo("echo abc \"hello baby!\"");
    }

    @Test
    public void shouldValidateConfig() {
        ExecTask execTask = new ExecTask("arg1 arg2", new Arguments(new Argument("arg1"), new Argument("arg2")));
        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(execTask.errors().isEmpty()).isFalse();
        assertThat(execTask.errors().firstErrorOn(ExecTask.ARGS)).isEqualTo(ExecTask.EXEC_CONFIG_ERROR);
        assertThat(execTask.errors().firstErrorOn(ExecTask.ARG_LIST_STRING)).isEqualTo(ExecTask.EXEC_CONFIG_ERROR);
    }

    @Test
    public void shouldValidateEachArgumentAndAddErrorsToTask() {
        ExecTask execTask = new ExecTask("echo", new Arguments(new Argument(null)), null);

        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));

        assertThat(execTask.errors().firstErrorOn(ExecTask.ARG_LIST_STRING)).isEqualTo("Invalid argument, cannot be null.");
    }


    @Test
    public void shouldBeValid() {
        ExecTask execTask = new ExecTask("", new Arguments(new Argument("arg1"), new Argument("arg2")));
        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(execTask.errors().isEmpty()).isTrue();
        execTask = new ExecTask("command", "", "blah");
        execTask.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(execTask.errors().isEmpty()).isTrue();
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
        assertThat(errors.size()).isEqualTo(1);
        String message = "The path of the working directory for the custom command in job 'job' in stage 'stage' of pipeline 'pipeline' is outside the agent sandbox. It must be relative to the directory where the agent checks out materials.";
        assertThat(errors.get(0).firstError()).isEqualTo(message);
        assertThat(task.errors().firstErrorOn(ExecTask.WORKING_DIR)).isEqualTo(message);
    }

    @Test
    public void shouldAllowSettingOfConfigAttributes() {
        ExecTask exec = new ExecTask();
        exec.setConfigAttributes(Map.of(ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "my_dir"));
        assertThat(exec.command()).isEqualTo("ls");
        assertThat(exec.getArgs()).isEqualTo("-la");
        assertThat(exec.getArgListString()).isEmpty();
        assertThat(exec.workingDirectory()).isEqualTo("my_dir");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(ExecTask.COMMAND, null);
        attributes.put(ExecTask.ARGS, null);
        attributes.put(ExecTask.WORKING_DIR, null);
        exec.setConfigAttributes(attributes);
        assertThat(exec.command()).isNull();
        assertThat(exec.getArgs()).isEmpty();
        assertThat(exec.workingDirectory()).isNull();

        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put(ExecTask.COMMAND, null);
        attributes1.put(ExecTask.ARG_LIST_STRING, "-l\n-a\npavan's\\n working dir?");
        attributes1.put(ExecTask.WORKING_DIR, null);
        exec.setConfigAttributes(attributes1);
        assertThat(exec.command()).isNull();
        assertThat(exec.getArgListString()).isEqualTo("-l\n-a\npavan's\\n working dir?");
        assertThat(exec.getArgList().size()).isEqualTo(3);
        assertThat(exec.getArgList().get(0)).isEqualTo(new Argument("-l"));
        assertThat(exec.getArgList().get(1)).isEqualTo(new Argument("-a"));
        assertThat(exec.getArgList().get(2)).isEqualTo(new Argument("pavan's\\n working dir?"));
        assertThat(exec.workingDirectory()).isNull();
    }

    @Test
    public void shouldNotSetAttributesWhenKeysNotPresentInAttributeMap() {
        ExecTask exec = new ExecTask();
        exec.setConfigAttributes(Map.of(ExecTask.COMMAND, "ls", ExecTask.ARGS, "-la", ExecTask.WORKING_DIR, "my_dir"));
        exec.setConfigAttributes(Map.of());//Key is not present
        assertThat(exec.command()).isEqualTo("ls");
        assertThat(exec.getArgs()).isEqualTo("-la");
        assertThat(exec.workingDirectory()).isEqualTo("my_dir");
    }

    @Test
    public void shouldNotSetArgsIfTheValueIsBlank() {
        ExecTask exec = new ExecTask();
        exec.setConfigAttributes(Map.of(ExecTask.COMMAND, "ls", ExecTask.ARGS, "", ExecTask.WORKING_DIR, "my_dir"));
        exec.setConfigAttributes(Map.of());
        assertThat(exec.command()).isEqualTo("ls");
        assertThat(exec.getArgList().size()).isEqualTo(0);
        assertThat(exec.workingDirectory()).isEqualTo("my_dir");
    }

    @Test
    public void shouldNullOutWorkingDirectoryIfGivenBlank() {
        ExecTask exec = new ExecTask("ls", "-la", "foo");
        exec.setConfigAttributes(Map.of(ExecTask.COMMAND, "", ExecTask.ARGS, "", ExecTask.WORKING_DIR, ""));
        assertThat(exec.command()).isEmpty();
        assertThat(exec.getArgs()).isEmpty();
        assertThat(exec.workingDirectory()).isNull();
    }

    @Test
    public void shouldPopulateAllAttributesOnPropertiesForDisplay() {
        ExecTask execTask = new ExecTask("ls", "-la", "holy/dir");
        execTask.setTimeout(10L);
        assertThat(execTask.getPropertiesForDisplay()).contains(new TaskProperty("Command", "ls", "command"), new TaskProperty("Arguments", "-la", "arguments"), new TaskProperty("Working Directory", "holy/dir", "working_directory"), new TaskProperty("Timeout", "10", "timeout"));
        assertThat(execTask.getPropertiesForDisplay().size()).isEqualTo(4);

        execTask = new ExecTask("ls", new Arguments(new Argument("-la"), new Argument("/proc")), "holy/dir");
        execTask.setTimeout(10L);
        assertThat(execTask.getPropertiesForDisplay()).contains(new TaskProperty("Command", "ls", "command"), new TaskProperty("Arguments", "-la /proc", "arguments"), new TaskProperty("Working Directory", "holy/dir", "working_directory"), new TaskProperty("Timeout", "10", "timeout"));
        assertThat(execTask.getPropertiesForDisplay().size()).isEqualTo(4);

        execTask = new ExecTask("ls", new Arguments(new Argument()), null);
        assertThat(execTask.getPropertiesForDisplay()).contains(new TaskProperty("Command", "ls", "command"));
        assertThat(execTask.getPropertiesForDisplay().size()).isEqualTo(1);

        execTask = new ExecTask("ls", "", (String) null);
        assertThat(execTask.getPropertiesForDisplay()).contains(new TaskProperty("Command", "ls", "command"));
        assertThat(execTask.getPropertiesForDisplay().size()).isEqualTo(1);
    }

    @Test
    public void validateTask_shouldValidateThatCommandIsRequired() {
        ExecTask execTask = new ExecTask();

        execTask.validateTask(null);

        assertThat(execTask.errors().isEmpty()).isFalse();
        assertThat(execTask.errors().firstErrorOn(ExecTask.COMMAND)).isEqualTo("Command cannot be empty");
    }

    @Test
    public void shouldErrorOutForTemplates_WhenItHasATaskWithInvalidWorkingDirectory() {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("some_pipeline");
        StageConfig templateStage = StageConfigMother.stageWithTasks("templateStage");
        ExecTask execTask = new ExecTask("ls", "-la", "/");
        templateStage.getJobs().getFirstOrNull().addTask(execTask);
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("template_name"), templateStage);
        cruiseConfig.addTemplate(template);

        try {
            execTask.validateTask(ConfigSaveValidationContext.forChain(cruiseConfig, template, templateStage, templateStage.getJobs().getFirstOrNull()));
            assertThat(execTask.errors().isEmpty()).isFalse();
            assertThat(execTask.errors().firstErrorOn(ExecTask.WORKING_DIR)).isEqualTo("The path of the working directory for the custom command in job 'job' in stage 'templateStage' of template 'template_name' is outside the agent sandbox. It must be relative to the directory where the agent checks out materials.");
        } catch (Exception e) {
            fail("should not have failed. Exception: " + e.getMessage());
        }
    }

    @Test
    public void shouldUseConfiguredWorkingDirectory() {
        File absoluteFile = new File("test").getAbsoluteFile();
        ExecTask task = new ExecTask("command", "arguments", absoluteFile.getAbsolutePath());

        assertThat(task.workingDirectory()).isEqualTo((absoluteFile.getPath()));
    }

    @Test
    public void shouldReturnCommandTaskAttributes() {
        ExecTask task = new ExecTask("ls", "-laht", "src/build");
        assertThat(task.command()).isEqualTo("ls");
        assertThat(task.arguments()).isEqualTo("-laht");
        assertThat(task.workingDirectory()).isEqualTo("src/build");
    }

    @Test
    public void shouldReturnCommandArgumentList() {
        ExecTask task = new ExecTask("./bn", new Arguments(new Argument("clean"), new Argument("compile"), new Argument("\"buildfile\"")), "src/build");
        assertThat(task.arguments()).isEqualTo("clean compile \"buildfile\"");
    }

    @Test
    public void shouldReturnEmptyCommandArguments() {
        ExecTask task = new ExecTask("./bn", new Arguments(), "src/build");
        assertThat(task.arguments()).isEmpty();
    }

    @Test
    public void shouldBeSameIfCommandMatches() {
        ExecTask task = new ExecTask("ls", new Arguments());

        assertEquals(new ExecTask("ls", new Arguments()), task);
    }

    @Test
    public void shouldUnEqualIfCommandsDontMatch() {
        ExecTask task = new ExecTask("ls", new Arguments());

        assertNotEquals(new ExecTask("rm", new Arguments()), task);
    }

    @Test
    public void shouldUnEqualIfCommandIsNull() {
        ExecTask task = new ExecTask(null, new Arguments());

        assertNotEquals(new ExecTask("rm", new Arguments()), task);
    }

    @Test
    public void shouldUnEqualIfOtherTaskCommandIsNull() {
        ExecTask task = new ExecTask("ls", new Arguments());

        assertNotEquals(new ExecTask(null, new Arguments()), task);
    }

    @Test
    public void shouldSetConfigAttributesWithCarriageReturnCharPresent() {
        ExecTask exec = new ExecTask();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(ExecTask.COMMAND, null);
        attributes.put(ExecTask.ARG_LIST_STRING, "ls\r\n-al\r\n&&\npwd");
        exec.setConfigAttributes(attributes);
        assertThat(exec.command()).isNull();
        assertThat(exec.getArgListString()).isEqualTo("ls\n-al\n&&\npwd");
        assertThat(exec.getArgList().size()).isEqualTo(4);
        assertThat(exec.getArgList().get(0)).isEqualTo(new Argument("ls"));
        assertThat(exec.getArgList().get(1)).isEqualTo(new Argument("-al"));
        assertThat(exec.getArgList().get(2)).isEqualTo(new Argument("&&"));
        assertThat(exec.getArgList().get(3)).isEqualTo(new Argument("pwd"));
    }
}
