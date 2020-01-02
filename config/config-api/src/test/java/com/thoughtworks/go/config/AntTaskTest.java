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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AntTaskTest {
    private AntTask antTask;

    @Before
    public void setup() throws Exception {
        antTask = new AntTask();
    }

    @Test
    public void shouldRetainDoubleQuotesInDescription() {
        antTask.setTarget("\"foo bar\" baz —debug");
        assertThat(antTask.describe(), is("ant \"foo bar\" baz —debug"));
    }

    @Test
    public void shouldRetainSingleQuotesInDescription() {
        antTask.setTarget("'foo bar' baz —debug");
        assertThat(antTask.describe(), is("ant 'foo bar' baz —debug"));
    }

    @Test public void shouldNotSetTargetOnBuilderWhenNotSet() throws Exception {
        assertThat(antTask.arguments(), is(""));
    }

    @Test public void shouldSetTargetOnBuilderWhenAvailable() throws Exception {
        String target = "target";
        antTask.setTarget(target);
        assertThat(antTask.arguments(), is(target));
    }

    @Test public void shouldSetBuildFileWhenAvailable() throws Exception {
        String target = "target";
        String buildXml = "build.xml";
        antTask.setBuildFile(buildXml);
        antTask.setTarget(target);
        assertThat(antTask.arguments(), is("-f \"" + buildXml + "\" " + target));

        String distBuildXml = "build/dist.xml";
        antTask.setBuildFile(distBuildXml);
        assertThat(antTask.arguments(), is("-f \"" + distBuildXml + "\" " + target));
    }

    @Test
    public void describeTest() throws Exception {
        antTask.setBuildFile("build.xml");
        antTask.setTarget("test");
        antTask.setWorkingDirectory("lib");
        assertThat(antTask.describe(), is("ant -f \"build.xml\" test (workingDirectory: lib)"));
    }

    @Test
    public void shouldReturnCommandAndWorkingDir(){
        antTask.setWorkingDirectory("lib");
        assertThat(antTask.command(),is("ant"));
        assertThat(antTask.workingDirectory(), is("lib"));
    }

    @Test
    public void shouldGiveArgumentsIncludingBuildfileAndTarget(){
        AntTask task = new AntTask();
        task.setBuildFile("build/build.xml");
        task.setTarget("compile");
        assertThat(task.arguments(), is("-f \"build/build.xml\" compile"));
    }
}
