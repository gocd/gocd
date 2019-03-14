/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static org.assertj.core.api.Assertions.assertThat;

class TestCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    void testDirExists() throws IOException {
        runBuild(test("-d", ""), Passed);
        runBuild(test("-d", "dir"), Failed);
        runBuild(test("-d", "file"), Failed);
        assertThat(new File(sandbox, "file").createNewFile()).isTrue();
        assertThat(new File(sandbox, "dir").mkdir()).isTrue();
        runBuild(test("-d", "file"), Failed);
        runBuild(test("-d", "dir"), Passed);
    }

    @Test
    void testDirNotExists() throws IOException {
        runBuild(test("-nd", ""), Failed);
        runBuild(test("-nd", "dir"), Passed);
        runBuild(test("-nd", "file"), Passed);
        assertThat(new File(sandbox, "file").createNewFile()).isTrue();
        assertThat(new File(sandbox, "dir").mkdir()).isTrue();
        runBuild(test("-nd", "file"), Passed);
        runBuild(test("-nd", "dir"), Failed);
    }

    @Test
    void testFileExists() throws IOException {
        runBuild(test("-f", ""), Failed);
        runBuild(test("-f", "file"), Failed);
        File file = new File(sandbox, "file");
        assertThat(file.createNewFile()).isTrue();
        runBuild(test("-f", "file"), Passed);
    }

    @Test
    void testFileNotExists() throws IOException {
        runBuild(test("-nf", ""), Passed);
        runBuild(test("-nf", "file"), Passed);
        File file = new File(sandbox, "file");
        assertThat(file.createNewFile()).isTrue();
        runBuild(test("-nf", "file"), Failed);
    }

    @Test
    void testEqWithCommandOutput() {
        runBuild(test("-eq", "foo", echo("foo")), Passed);
        runBuild(test("-eq", "bar", echo("foo")), Failed);
        assertThat(console.lineCount()).isEqualTo(0);
    }

    @Test
    void testNotEqWithCommandOutput() {
        runBuild(test("-neq", "foo", echo("foo")), Failed);
        runBuild(test("-neq", "bar", echo("foo")), Passed);
        assertThat(console.lineCount()).isEqualTo(0);
    }

    @Test
    void testCommandOutputContainsString() {
        runBuild(test("-in", "foo", echo("foo bar")), Passed);
        runBuild(test("-in", "foo", echo("bar")), Failed);
        assertThat(console.lineCount()).isEqualTo(0);
    }

    @Test
    void testCommandOutputDoesNotContainsString() {
        runBuild(test("-nin", "foo", echo("foo bar")), Failed);
        runBuild(test("-nin", "foo", echo("bar")), Passed);
        assertThat(console.lineCount()).isEqualTo(0);
    }

    @Test
    void mkdirWithWorkingDir() {
        runBuild(mkdirs("foo").setWorkingDirectory("bar"), Passed);
        assertThat(new File(sandbox, "bar/foo").isDirectory()).isTrue();
        assertThat(new File(sandbox, "foo").isDirectory()).isFalse();
    }

    @Test
    void shouldNotFailBuildWhenTestCommandFail() {
        runBuild(echo("foo").setTest(fail("")), Passed);
        assertThat(statusReporter.singleResult()).isEqualTo(Passed);
    }

    @Test
    void shouldNotFailBuildWhenComposedTestCommandFail() {
        runBuild(echo("foo").setTest(compose(echo(""), fail(""))), Passed);
        assertThat(statusReporter.singleResult()).isEqualTo(JobResult.Passed);
    }

    @Test
    void shouldNotFailBuildWhenTestEqWithComposedCommandOutputFail() {
        runBuild(echo("foo").setTest(test("-eq", "42", compose(fail("42")))), Passed);
        assertThat(statusReporter.singleResult()).isEqualTo(Passed);
        assertThat(console.output()).contains("foo");
    }
}
