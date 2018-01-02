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

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.domain.BuildCommand.cleandir;
import static com.thoughtworks.go.domain.BuildCommand.mkdirs;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class CleandirCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    public void cleanDirWithoutAllows() throws IOException {
        runBuild(mkdirs("foo/baz"), Passed);
        assertTrue(new File(sandbox, "foo/file1").createNewFile());
        assertTrue(new File(sandbox, "file2").createNewFile());

        runBuild(cleandir(""), Passed);
        assertThat(sandbox.exists(), is(true));
        assertThat(sandbox.listFiles().length, is(0));
    }

    @Test
    public void cleanDirWithAllows() throws IOException {
        runBuild(mkdirs("bar/foo/baz"), Passed);
        runBuild(mkdirs("bar/foo2"), Passed);
        assertTrue(new File(sandbox, "bar/foo/file1").createNewFile());
        assertTrue(new File(sandbox, "bar/file2").createNewFile());
        assertTrue(new File(sandbox, "file3").createNewFile());

        runBuild(cleandir("bar", "file2", "foo2"), Passed);
        assertThat(new File(sandbox, "bar").isDirectory(), is(true));
        assertThat(new File(sandbox, "file3").exists(), is(true));
        assertThat(new File(sandbox, "bar").listFiles(), arrayContainingInAnyOrder(new File(sandbox, "bar/file2"), new File(sandbox, "bar/foo2")));
    }

    @Test
    public void cleanDirWithAllowsAndWorkingDir() throws IOException {
        runBuild(mkdirs("bar/foo/baz"), Passed);
        runBuild(mkdirs("bar/foo2"), Passed);
        assertTrue(new File(sandbox, "bar/foo/file1").createNewFile());
        assertTrue(new File(sandbox, "bar/file2").createNewFile());
        assertTrue(new File(sandbox, "file3").createNewFile());

        runBuild(cleandir("", "file2", "foo2").setWorkingDirectory("bar"), Passed);
        assertThat(new File(sandbox, "bar").isDirectory(), is(true));
        assertThat(new File(sandbox, "file3").exists(), is(true));
        assertThat(new File(sandbox, "bar").listFiles(), arrayContainingInAnyOrder(new File(sandbox, "bar/file2"), new File(sandbox, "bar/foo2")));
    }


}
