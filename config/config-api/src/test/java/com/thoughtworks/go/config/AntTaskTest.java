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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AntTaskTest {
    private AntTask antTask;

    @BeforeEach
    public void setup() throws Exception {
        antTask = new AntTask();
    }

    @Test
    public void shouldRetainDoubleQuotesInDescription() {
        antTask.setTarget("\"foo bar\" baz —debug");
        assertThat(antTask.describe()).isEqualTo("ant \"foo bar\" baz —debug");
    }

    @Test
    public void shouldRetainSingleQuotesInDescription() {
        antTask.setTarget("'foo bar' baz —debug");
        assertThat(antTask.describe()).isEqualTo("ant 'foo bar' baz —debug");
    }

    @Test
    public void shouldNotSetTargetOnBuilderWhenNotSet() {
        assertThat(antTask.arguments()).isEqualTo("");
    }

    @Test
    public void shouldSetTargetOnBuilderWhenAvailable() {
        String target = "target";
        antTask.setTarget(target);
        assertThat(antTask.arguments()).isEqualTo(target);
    }

    @Test
    public void shouldSetBuildFileWhenAvailable() {
        String target = "target";
        String buildXml = "build.xml";
        antTask.setBuildFile(buildXml);
        antTask.setTarget(target);
        assertThat(antTask.arguments()).isEqualTo("-f \"" + buildXml + "\" " + target);

        String distBuildXml = "build/dist.xml";
        antTask.setBuildFile(distBuildXml);
        assertThat(antTask.arguments()).isEqualTo("-f \"" + distBuildXml + "\" " + target);
    }

    @Test
    public void describeTest() {
        antTask.setBuildFile("build.xml");
        antTask.setTarget("test");
        antTask.setWorkingDirectory("lib");
        assertThat(antTask.describe()).isEqualTo("ant -f \"build.xml\" test (workingDirectory: lib)");
    }

    @Test
    public void shouldReturnCommandAndWorkingDir() {
        antTask.setWorkingDirectory("lib");
        assertThat(antTask.command()).isEqualTo("ant");
        assertThat(antTask.workingDirectory()).isEqualTo("lib");
    }

    @Test
    public void shouldGiveArgumentsIncludingBuildFileAndTarget() {
        AntTask task = new AntTask();
        task.setBuildFile("build/build.xml");
        task.setTarget("compile");
        assertThat(task.arguments()).isEqualTo("-f \"build/build.xml\" compile");
    }
}
