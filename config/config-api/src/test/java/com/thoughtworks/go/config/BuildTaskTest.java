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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildTaskTest {

    @Test
    public void antTaskShouldNormalizeBuildFile() {
        AntTask task = new AntTask();
        task.setBuildFile("pavan\\build.xml");
        assertThat(task.arguments()).contains("\"pavan/build.xml\"");
    }

    @Test
    public void rakeTaskShouldNormalizeBuildFile() {
        RakeTask task = new RakeTask();
        task.setBuildFile("pavan\\build.xml");
        assertThat(task.arguments()).contains("\"pavan/build.xml\"");
    }

    @Test
    public void shouldUpdateAllItsAttributes() {
        BuildTask task = new BuildTask() {

            @Override
            public String getTaskType() {
                return "build";
            }

            @Override
            public String getTypeForDisplay() {
                return "test-task";
            }

            @Override
            public String command() {
                return null;
            }

            @Override
            public String arguments() {
                return null;
            }
        };
        task.setConfigAttributes(Map.of(BuildTask.BUILD_FILE, "foo/build.xml", BuildTask.TARGET, "foo.target", BuildTask.WORKING_DIRECTORY, "work_dir"));
        assertThat(task.getBuildFile()).isEqualTo("foo/build.xml");
        assertThat(task.getTarget()).isEqualTo("foo.target");
        assertThat(task.workingDirectory()).isEqualTo("work_dir");
        task.setConfigAttributes(Map.of(BuildTask.BUILD_FILE, "", BuildTask.TARGET, "", BuildTask.WORKING_DIRECTORY, ""));
        assertThat(task.getBuildFile()).isNull();
        assertThat(task.getTarget()).isNull();
        assertThat(task.workingDirectory()).isNull();
    }

    @Test
    public void shouldSetWorkingDirectoryToNullIfValueIsAnEmptyString() {
        BuildTask task = new BuildTask() {

            @Override
            public String getTaskType() {
                return "build";
            }

            @Override
            public String getTypeForDisplay() {
                return "test-task";
            }

            @Override
            public String command() {
                return null;
            }

            @Override
            public String arguments() {
                return null;
            }
        };
        task.setConfigAttributes(Map.of(BuildTask.BUILD_FILE, "", BuildTask.TARGET, "", BuildTask.WORKING_DIRECTORY, ""));
        assertThat(task.getBuildFile()).isNull();
        assertThat(task.getTarget()).isNull();
        assertThat(task.workingDirectory()).isNull();
    }

    @Test
    public void shouldNotUpdateItsAttributesWhenMapDoesNotHaveKeys() {
        BuildTask task = new BuildTask() {

            @Override
            public String getTaskType() {
                return "build";
            }

            @Override
            public String getTypeForDisplay() {
                return "test-task";
            }

            @Override
            public String command() {
                return null;
            }

            @Override
            public String arguments() {
                return null;
            }
        };
        task.setConfigAttributes(Map.of(BuildTask.BUILD_FILE, "foo/build.xml", BuildTask.TARGET, "foo.target", BuildTask.WORKING_DIRECTORY, "work_dir"));
        task.setConfigAttributes(Map.of());
        assertThat(task.getBuildFile()).isEqualTo("foo/build.xml");
        assertThat(task.getTarget()).isEqualTo("foo.target");
        assertThat(task.workingDirectory()).isEqualTo("work_dir");
    }

    @Test
    public void shouldReturnAllFieldsAsProperties() {
        BuildTask task = new BuildTask() {

            @Override
            public String getTaskType() {
                return "build";
            }

            @Override
            public String getTypeForDisplay() {
                return null;
            }

            @Override
            public String command() {
                return null;
            }

            @Override
            public String arguments() {
                return null;
            }
        };

        assertThat(task.getPropertiesForDisplay().isEmpty()).isTrue();

        task.setBuildFile("some-file.xml");
        task.setTarget("do-something");
        task.setWorkingDirectory("some/dir");

        assertThat(task.getPropertiesForDisplay()).contains(new TaskProperty("Build File", "some-file.xml", "build_file"), new TaskProperty("Target", "do-something", "target"),
                new TaskProperty("Working Directory", "some/dir", "working_directory"));
    }

    @Test
    public void shouldErrorOutIfWorkingDirectoryIsOutsideTheCurrentWorkingDirectory() {
        BuildTask task = new BuildTask() {

            @Override
            public String getTaskType() {
                return "build";
            }

            @Override
            public String getTypeForDisplay() {
                return null;
            }

            @Override
            public String command() {
                return null;
            }

            @Override
            public String arguments() {
                return null;
            }
        };
        task.setWorkingDirectory("/blah");
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline");
        PipelineConfig pipeline = config.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        StageConfig stage = pipeline.getFirst();
        JobConfig job = stage.getJobs().getFirst();
        job.addTask(task);

        List<ConfigErrors> errors = config.validateAfterPreprocess();
        assertThat(errors.size()).isEqualTo(1);
        String message = "Task of job 'job' in stage 'stage' of pipeline 'pipeline' has path '/blah' which is outside the working directory.";
        assertThat(task.errors().firstErrorOn(BuildTask.WORKING_DIRECTORY)).isEqualTo(message);
    }

    @Test
    public void shouldErrorOutIfWorkingDirectoryIsOutsideTheCurrentWorkingDirectoryForTemplates() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-blah");

        BuildTask task = new AntTask();
        task.setWorkingDirectory("/blah");
        StageConfig stageConfig = StageConfigMother.manualStage("manualStage");
        stageConfig.getJobs().getFirst().addTask(task);
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("some-template"), stageConfig);
        config.addTemplate(template);

        List<ConfigErrors> errors = config.validateAfterPreprocess();
        assertThat(errors.size()).isEqualTo(1);
        String message = "Task of job 'default' in stage 'manualStage' of template 'some-template' has path '/blah' which is outside the working directory.";
        assertThat(task.errors().firstErrorOn(BuildTask.WORKING_DIRECTORY)).isEqualTo(message);
    }
}
