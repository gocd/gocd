/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilenameUtilTest {

    @Test
    public void shouldReturnFalseIfGivenFolderIsAbsolute() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("c:\\foo"), is(false));
    }

    @Test
    public void shouldReturnFalseIfGivenFolderIsAbsoluteUnderLinux() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("/tmp"), is(false));
    }

    @Test
    public void shouldReturnFalseIfGivenFolderWithRelativeTakesYouOutOfSandbox() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("../tmp"), is(false));
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("tmp/../../../pavan"), is(false));
    }

    @Test
    public void shouldReturnTrueIfGivenFolderWithRelativeKeepsYouInsideSandbox() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("tmp/../home/cruise"), is(true));
    }


    @Test
    public void shouldReturnFalseEvenIfAnAbsolutePathKeepsYouInsideSandbox() {
        File file = new File("somethingInsideCurrentFolder");
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir(file.getAbsolutePath()), is(false));
    }

    @Test
    public void shouldReturnFalseIfDirectoryNameIsSameAsParentDirectoryButNotASubdirectory() throws Exception {
        assertFalse(FilenameUtil.isNormalizedDirectoryPathInsideNormalizedParentDirectory("config", "artifacts/config"));
    }

    @Test
    public void shouldReturnTrueIfDirectoryIsSubdirectoryOfParent() throws Exception {
        assertTrue(FilenameUtil.isNormalizedDirectoryPathInsideNormalizedParentDirectory("artifacts", "artifacts/config"));
    }
}
