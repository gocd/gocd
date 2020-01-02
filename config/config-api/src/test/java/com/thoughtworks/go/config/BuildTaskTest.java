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
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.List;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class BuildTaskTest {

    @Test
    public void antTaskShouldNormalizeBuildFile() throws Exception {
        AntTask task = new AntTask();
        task.setBuildFile("pavan\\build.xml");
        assertThat(task.arguments(), containsString("\"pavan/build.xml\""));
    }

    @Test
    public void rakeTaskShouldNormalizeBuildFile() throws Exception {
        RakeTask task = new RakeTask();
        task.setBuildFile("pavan\\build.xml");
        assertThat(task.arguments(), containsString("\"pavan/build.xml\""));
    }

    @Test
    public void shouldUpdateAllItsAttributes() throws Exception {
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
        task.setConfigAttributes(m(BuildTask.BUILD_FILE, "foo/build.xml", BuildTask.TARGET, "foo.target", BuildTask.WORKING_DIRECTORY, "work_dir"));
        assertThat(task.getBuildFile(), Matchers.is("foo/build.xml"));
        assertThat(task.getTarget(), Matchers.is("foo.target"));
        assertThat(task.workingDirectory(), Matchers.is("work_dir"));
        task.setConfigAttributes(m(BuildTask.BUILD_FILE, "", BuildTask.TARGET, "", BuildTask.WORKING_DIRECTORY, ""));
        assertThat(task.getBuildFile(), Matchers.is(nullValue()));
        assertThat(task.getTarget(), Matchers.is(nullValue()));
        assertThat(task.workingDirectory(), Matchers.is(nullValue()));
    }

    @Test
    public void shouldSetWorkingDirectoryToNullIfValueIsAnEmptyString() throws Exception {
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
        task.setConfigAttributes(m(BuildTask.BUILD_FILE, "", BuildTask.TARGET, "", BuildTask.WORKING_DIRECTORY, ""));
        assertThat(task.getBuildFile(), is(nullValue()));
        assertThat(task.getTarget(), is(nullValue()));
        assertThat(task.workingDirectory(), is(nullValue()));
    }

    @Test
    public void shouldNotUpdateItsAttributesWhenMapDoesNotHaveKeys() throws Exception {
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
        task.setConfigAttributes(m(BuildTask.BUILD_FILE, "foo/build.xml", BuildTask.TARGET, "foo.target", BuildTask.WORKING_DIRECTORY, "work_dir"));
        task.setConfigAttributes(m());
        assertThat(task.getBuildFile(), Matchers.is("foo/build.xml"));
        assertThat(task.getTarget(), Matchers.is("foo.target"));
        assertThat(task.workingDirectory(), Matchers.is("work_dir"));
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

        assertThat(task.getPropertiesForDisplay().isEmpty(), is(true));

        task.setBuildFile("some-file.xml");
        task.setTarget("do-something");
        task.setWorkingDirectory("some/dir");

        assertThat(task.getPropertiesForDisplay(), hasItems(new TaskProperty("Build File", "some-file.xml", "build_file"), new TaskProperty("Target", "do-something", "target"),
                new TaskProperty("Working Directory", "some/dir", "working_directory")));
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
        StageConfig stage = pipeline.get(0);
        JobConfig job = stage.getJobs().get(0);
        job.addTask(task);

        List<ConfigErrors> errors = config.validateAfterPreprocess();
        assertThat(errors.size(), is(1));
        String message = "Task of job 'job' in stage 'stage' of pipeline 'pipeline' has path '/blah' which is outside the working directory.";
        assertThat(task.errors().on(BuildTask.WORKING_DIRECTORY), is(message));
    }

    @Test
    public void shouldErrorOutIfWorkingDirectoryIsOutsideTheCurrentWorkingDirectoryForTemplates() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline-blah");

        BuildTask task = new AntTask();
        task.setWorkingDirectory("/blah");
        StageConfig stageConfig = StageConfigMother.manualStage("manualStage");
        stageConfig.getJobs().get(0).addTask(task);
        PipelineTemplateConfig template = new PipelineTemplateConfig(new CaseInsensitiveString("some-template"), stageConfig);
        config.addTemplate(template);

        List<ConfigErrors> errors = config.validateAfterPreprocess();
        assertThat(errors.size(), is(1));
        String message = "Task of job 'default' in stage 'manualStage' of template 'some-template' has path '/blah' which is outside the working directory.";
        assertThat(task.errors().on(BuildTask.WORKING_DIRECTORY), is(message));
    }
}
