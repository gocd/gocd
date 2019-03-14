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

import static com.thoughtworks.go.domain.BuildCommand.mkdirs;
import static com.thoughtworks.go.domain.BuildCommand.test;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static org.assertj.core.api.Assertions.assertThat;

class MkdirsCommandExecutorTest extends BuildSessionBasedTestCase {
    @Test
    void mkdirsShouldCreateDirectoryIfNotExists() {
        runBuild(mkdirs("foo"), Passed);
        assertThat(new File(sandbox, "foo").isDirectory()).isTrue();
    }

    @Test
    void mkdirsShouldFailIfDirExists() {
        runBuild(mkdirs("foo"), Passed);
        runBuild(mkdirs("foo"), Failed);
        assertThat(new File(sandbox, "foo").isDirectory()).isTrue();
    }

    @Test
    void testDirectoryExistsBeforeMkdir() {
        File dir = new File(sandbox, "foo");
        runBuild(mkdirs("foo"), Passed);
        runBuild(mkdirs("foo").setTest(test("-nd", dir.getPath())), Passed);
        assertThat(new File(sandbox, "foo").isDirectory()).isTrue();
    }
}
