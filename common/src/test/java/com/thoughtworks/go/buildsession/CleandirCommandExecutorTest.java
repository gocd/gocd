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


import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.domain.BuildCommand.cleandir;
import static com.thoughtworks.go.domain.BuildCommand.mkdirs;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static org.assertj.core.api.Assertions.assertThat;

class CleandirCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    void cleanDirWithoutAllows() throws IOException {
        runBuild(mkdirs("foo/baz"), Passed);
        assertThat(new File(sandbox, "foo/file1").createNewFile()).isTrue();
        assertThat(new File(sandbox, "file2").createNewFile()).isTrue();

        runBuild(cleandir(""), Passed);
        assertThat(sandbox.exists()).isTrue();
        assertThat(sandbox.listFiles().length).isEqualTo(0);
    }

    @Test
    void cleanDirWithAllows() throws IOException {
        runBuild(mkdirs("bar/foo/baz"), Passed);
        runBuild(mkdirs("bar/foo2"), Passed);
        assertThat(new File(sandbox, "bar/foo/file1").createNewFile()).isTrue();
        assertThat(new File(sandbox, "bar/file2").createNewFile()).isTrue();
        assertThat(new File(sandbox, "file3").createNewFile()).isTrue();

        runBuild(cleandir("bar", "file2", "foo2"), Passed);
        assertThat(new File(sandbox, "bar").isDirectory()).isTrue();
        assertThat(new File(sandbox, "file3").exists()).isTrue();
        assertThat(new File(sandbox, "bar").listFiles()).contains(new File(sandbox, "bar/file2"), new File(sandbox, "bar/foo2"));
    }

    @Test
    void cleanDirWithAllowsAndWorkingDir() throws IOException {
        runBuild(mkdirs("bar/foo/baz"), Passed);
        runBuild(mkdirs("bar/foo2"), Passed);
        assertThat(new File(sandbox, "bar/foo/file1").createNewFile()).isTrue();
        assertThat(new File(sandbox, "bar/file2").createNewFile()).isTrue();
        assertThat(new File(sandbox, "file3").createNewFile()).isTrue();

        runBuild(cleandir("", "file2", "foo2").setWorkingDirectory("bar"), Passed);
        assertThat(new File(sandbox, "bar").isDirectory()).isTrue();
        assertThat(new File(sandbox, "file3").exists()).isTrue();
        assertThat(new File(sandbox, "bar").listFiles()).contains(new File(sandbox, "bar/file2"), new File(sandbox, "bar/foo2"));
    }


}
