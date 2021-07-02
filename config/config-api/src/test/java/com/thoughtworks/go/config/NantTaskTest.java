/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.TaskProperty;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class NantTaskTest {
    @Test
    public void describeTest() throws Exception {
        NantTask task = new NantTask();
        task.setBuildFile("default.build");
        task.setTarget("test");
        task.setWorkingDirectory("lib");
        task.setNantPath("tmp");

        assertThat(task.describe(),
                is("tmp" + File.separator + "nant -buildfile:\"default.build\" test (workingDirectory: lib)"));
    }

    @Test
    public void shouldUpdateAllItsAttributes() throws Exception {
        NantTask nant = new NantTask();
        nant.setConfigAttributes(m(BuildTask.BUILD_FILE, "foo/build.xml", NantTask.NANT_PATH, "/usr/bin/nant"));
        assertThat(nant.getBuildFile(), is("foo/build.xml"));
        assertThat(nant.getNantPath(), is("/usr/bin/nant"));
        nant.setConfigAttributes(m());
        assertThat(nant.getBuildFile(), is("foo/build.xml"));
        assertThat(nant.getNantPath(), is("/usr/bin/nant"));
        nant.setConfigAttributes(null);
        assertThat(nant.getBuildFile(), is("foo/build.xml"));
        assertThat(nant.getNantPath(), is("/usr/bin/nant"));
    }

    @Test
    public void shouldNotUpdateAllItsAttributesWhenNotPassedInAsAttributes() throws Exception {
        NantTask nant = new NantTask();
        nant.setConfigAttributes(m(BuildTask.BUILD_FILE, null, NantTask.NANT_PATH, null));
        assertThat(nant.getBuildFile(), is(nullValue()));
        assertThat(nant.getNantPath(), is(nullValue()));
    }

    @Test
    public void shouldReturnPropertiesPopulatedWithNantpath() {
        NantTask nantTask = new NantTask();
        assertThat(nantTask.getPropertiesForDisplay().isEmpty(), is(true));
        nantTask.setBuildFile("some-file.xml");
        nantTask.setTarget("bulls_eye");
        nantTask.setWorkingDirectory("some/dir");
        nantTask.setNantPath("foo/bar/baz");
        assertThat(nantTask.getPropertiesForDisplay(), hasItems(new TaskProperty("Nant Path", "foo/bar/baz", "nant_path"),
                new TaskProperty("Build File", "some-file.xml", "build_file"), new TaskProperty("Target", "bulls_eye", "target"),
                new TaskProperty("Working Directory", "some/dir", "working_directory")));
        assertThat(nantTask.getPropertiesForDisplay().size(), is(4));
    }

    @Test
    public void commandShouldShowFullNantPath() {
        NantTask task = new NantTask();
        String path = "c:/nant/bin";
        task.setNantPath(path);
        assertThat(task.command(), is(new File(path, "nant").getPath()));
    }

    @Test
    public void shouldGiveArgumentsIncludingBuildfileAndTarget() {
        NantTask task = new NantTask();
        task.setBuildFile("build/build.xml");
        task.setTarget("compile");
        assertThat(task.arguments(), is("-buildfile:\"build/build.xml\" compile"));
    }
}
