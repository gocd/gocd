/*
 * Copyright Thoughtworks, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

public class FilenameUtilTest {

    @Test
    public void shouldReturnFalseIfGivenFolderIsAbsolute() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("c:\\foo")).isFalse();
    }

    @Test
    public void shouldReturnFalseIfGivenFolderIsAbsoluteUnderLinux() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("/tmp")).isFalse();
    }

    @Test
    public void shouldReturnFalseIfGivenFolderWithRelativeTakesYouOutOfSandbox() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("../tmp")).isFalse();
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("tmp/../../../pavan")).isFalse();
    }

    @Test
    public void shouldReturnTrueIfGivenFolderWithRelativeKeepsYouInsideSandbox() {
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir("tmp/../home/cruise")).isTrue();
    }


    @Test
    public void shouldReturnFalseEvenIfAnAbsolutePathKeepsYouInsideSandbox() {
        File file = new File("somethingInsideCurrentFolder");
        assertThat(FilenameUtil.isNormalizedPathOutsideWorkingDir(file.getAbsolutePath())).isFalse();
    }

    @Test
    public void shouldReturnFalseIfDirectoryNameIsSameAsParentDirectoryButNotASubdirectory() {
        assertThat(FilenameUtil.isNormalizedDirectoryPathInsideNormalizedParentDirectory("config", "artifacts/config")).isFalse();
    }

    @Test
    public void shouldReturnTrueIfDirectoryIsSubdirectoryOfParent() {
        assertThat(FilenameUtil.isNormalizedDirectoryPathInsideNormalizedParentDirectory("artifacts", "artifacts/config")).isTrue();
    }
}
