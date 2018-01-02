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

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class SecretCommandExecutorTest extends BuildSessionBasedTestCase {

    @Test
    public void secretMaskValuesInExecOutput() throws Exception {
        runBuild(compose(
                secret("42"),
                exec("echo", "the answer is 42")), Passed);
        assertThat(console.output(), containsString("the answer is ******"));
    }

    @Test
    public void secretMaskValuesInExportOutput() throws Exception {
        runBuild(compose(
                secret("42"),
                export("oracle", "the answer is 42", false)), Passed);
        assertThat(console.output(), is("[go] setting environment variable 'oracle' to value 'the answer is ******'"));
    }

    @Test
    public void addSecretWithSubstitution() throws Exception {
        runBuild(compose(
                secret("foo:bar@ssss.com", "foo:******@ssss.com"),
                exec("echo", "connecting to foo:bar@ssss.com"),
                exec("echo", "connecting to foo:bar@tttt.com")), Passed);
        assertThat(console.firstLine(), containsString("connecting to foo:******@ssss.com"));
        assertThat(console.asList().get(1), containsString("connecting to foo:bar@tttt.com"));
    }

    @Test
    public void shouldNotLeakSecretWhenExceptionHappened() throws Exception {
        runBuild(compose(
                secret("the-answer-is-42"),
                error("error: the-answer-is-42")), Failed);
        assertThat(console.output(), containsString("error: ******"));
        assertThat(console.output(), not(containsString("the-anwser-is-42")));
    }
}