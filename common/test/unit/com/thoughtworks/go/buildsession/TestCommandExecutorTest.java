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

import com.thoughtworks.go.domain.JobResult;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class TestCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    public void testDirExists() throws IOException {
        runBuild(test("-d", ""), Passed);
        runBuild(test("-d", "dir"), Failed);
        runBuild(test("-d", "file"), Failed);
        assertTrue(new File(sandbox, "file").createNewFile());
        assertTrue(new File(sandbox, "dir").mkdir());
        runBuild(test("-d", "file"), Failed);
        runBuild(test("-d", "dir"), Passed);
    }

    @Test
    public void testDirNotExists() throws IOException {
        runBuild(test("-nd", ""), Failed);
        runBuild(test("-nd", "dir"), Passed);
        runBuild(test("-nd", "file"), Passed);
        assertTrue(new File(sandbox, "file").createNewFile());
        assertTrue(new File(sandbox, "dir").mkdir());
        runBuild(test("-nd", "file"), Passed);
        runBuild(test("-nd", "dir"), Failed);
    }

    @Test
    public void testFileExists() throws IOException {
        runBuild(test("-f", ""), Failed);
        runBuild(test("-f", "file"), Failed);
        File file = new File(sandbox, "file");
        assertTrue(file.createNewFile());
        runBuild(test("-f", "file"), Passed);
    }

    @Test
    public void testFileNotExists() throws IOException {
        runBuild(test("-nf", ""), Passed);
        runBuild(test("-nf", "file"), Passed);
        File file = new File(sandbox, "file");
        assertTrue(file.createNewFile());
        runBuild(test("-nf", "file"), Failed);
    }

    @Test
    public void testEqWithCommandOutput() throws IOException {
        runBuild(test("-eq", "foo", echo("foo")), Passed);
        runBuild(test("-eq", "bar", echo("foo")), Failed);
        assertThat(console.lineCount(), is(0));
    }

    @Test
    public void testNotEqWithCommandOutput() throws IOException {
        runBuild(test("-neq", "foo", echo("foo")), Failed);
        runBuild(test("-neq", "bar", echo("foo")), Passed);
        assertThat(console.lineCount(), is(0));
    }

    @Test
    public void mkdirWithWorkingDir() {
        runBuild(mkdirs("foo").setWorkingDirectory("bar"), Passed);
        assertThat(new File(sandbox, "bar/foo").isDirectory(), is(true));
        assertThat(new File(sandbox, "foo").isDirectory(), is(false));
    }

    @Test
    public void shouldNotFailBuildWhenTestCommandFail() {
        runBuild(echo("foo").setTest(fail("")), Passed);
        assertThat(statusReporter.singleResult(), is(Passed));
    }

    @Test
    public void shouldNotFailBuildWhenComposedTestCommandFail() {
        runBuild(echo("foo").setTest(compose(echo(""), fail(""))), Passed);
        assertThat(statusReporter.singleResult(), is(JobResult.Passed));
    }

    @Test
    public void shouldNotFailBuildWhenTestEqWithComposedCommandOutputFail() {
        runBuild(echo("foo").setTest(test("-eq", "42", compose(fail("42")))), Passed);
        assertThat(statusReporter.singleResult(), is(Passed));
        assertThat(console.output(), containsString("foo"));
    }
}
