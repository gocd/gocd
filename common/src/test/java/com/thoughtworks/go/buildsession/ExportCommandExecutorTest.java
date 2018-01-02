/*
 * Copyright 2016 ThoughtWorks, Inc.
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
package com.thoughtworks.go.buildsession;

import org.junit.Test;

import static com.thoughtworks.go.domain.BuildCommand.compose;
import static com.thoughtworks.go.domain.BuildCommand.export;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ExportCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    public void exportEnvironmentVariableHasMeaningfulOutput() throws Exception {
        runBuild(compose(
                export("answer", "2", false),
                export("answer", "42", false)), Passed);
        assertThat(console.asList().get(0), is("[go] setting environment variable 'answer' to value '2'"));
        assertThat(console.asList().get(1), is("[go] overriding environment variable 'answer' with value '42'"));
    }

    @Test
    public void exportOutputWhenOverridingSystemEnv() throws Exception {
        String envName = pathSystemEnvName();
        runBuild(export(envName, "/foo/bar", false), Passed);
        assertThat(console.output(), is(String.format("[go] overriding environment variable '%s' with value '/foo/bar'", envName)));
    }

    @Test
    public void exportSecretEnvShouldMaskValue() throws Exception {
        runBuild(export("answer", "42", true), Passed);
        assertThat(console.output(), is("[go] setting environment variable 'answer' to value '********'"));
    }

    @Test
    public void exportWithoutValueDisplayCurrentValue() throws Exception {
        runBuild(export("foo"), Passed);
        assertThat(console.lastLine(), is("[go] setting environment variable 'foo' to value 'null'"));
        runBuild(compose(
                export("foo", "bar", false),
                export("foo")), Passed);
        assertThat(console.lastLine(), is("[go] setting environment variable 'foo' to value 'bar'"));
    }
}