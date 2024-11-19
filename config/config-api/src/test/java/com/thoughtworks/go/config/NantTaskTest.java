/*
 * Copyright 2024 Thoughtworks, Inc.
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NantTaskTest {
    @Test
    public void describeTest() {
        NantTask task = new NantTask();
        task.setBuildFile("default.build");
        task.setTarget("test");
        task.setWorkingDirectory("lib");
        task.setNantPath("tmp");

        assertThat(task.describe()).isEqualTo("tmp" + File.separator + "nant -buildfile:\"default.build\" test (workingDirectory: lib)");
    }

    @Test
    public void shouldUpdateAllItsAttributes() {
        NantTask nant = new NantTask();
        nant.setConfigAttributes(Map.of(BuildTask.BUILD_FILE, "foo/build.xml", NantTask.NANT_PATH, "/usr/bin/nant"));
        assertThat(nant.getBuildFile()).isEqualTo("foo/build.xml");
        assertThat(nant.getNantPath()).isEqualTo("/usr/bin/nant");
        nant.setConfigAttributes(Map.of());
        assertThat(nant.getBuildFile()).isEqualTo("foo/build.xml");
        assertThat(nant.getNantPath()).isEqualTo("/usr/bin/nant");
        nant.setConfigAttributes(null);
        assertThat(nant.getBuildFile()).isEqualTo("foo/build.xml");
        assertThat(nant.getNantPath()).isEqualTo("/usr/bin/nant");
    }

    @Test
    public void shouldNotUpdateAllItsAttributesWhenNotPassedInAsAttributes() {
        NantTask nant = new NantTask();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(BuildTask.BUILD_FILE, null);
        attributes.put(NantTask.NANT_PATH, null);
        nant.setConfigAttributes(attributes);
        assertThat(nant.getBuildFile()).isNull();
        assertThat(nant.getNantPath()).isNull();
    }

    @Test
    public void shouldReturnPropertiesPopulatedWithNantpath() {
        NantTask nantTask = new NantTask();
        assertThat(nantTask.getPropertiesForDisplay().isEmpty()).isTrue();
        nantTask.setBuildFile("some-file.xml");
        nantTask.setTarget("bulls_eye");
        nantTask.setWorkingDirectory("some/dir");
        nantTask.setNantPath("foo/bar/baz");
        assertThat(nantTask.getPropertiesForDisplay()).contains(new TaskProperty("Nant Path", "foo/bar/baz", "nant_path"),
                new TaskProperty("Build File", "some-file.xml", "build_file"), new TaskProperty("Target", "bulls_eye", "target"),
                new TaskProperty("Working Directory", "some/dir", "working_directory"));
        assertThat(nantTask.getPropertiesForDisplay().size()).isEqualTo(4);
    }

    @Test
    public void commandShouldShowFullNantPath() {
        NantTask task = new NantTask();
        String path = "c:/nant/bin";
        task.setNantPath(path);
        assertThat(task.command()).isEqualTo(new File(path, "nant").getPath());
    }

    @Test
    public void shouldGiveArgumentsIncludingBuildFileAndTarget() {
        NantTask task = new NantTask();
        task.setBuildFile("build/build.xml");
        task.setTarget("compile");
        assertThat(task.arguments()).isEqualTo("-buildfile:\"build/build.xml\" compile");
    }
}
