/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.FileUtil.isSubdirectoryOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class FileUtilTest {

    @TempDir
    File folder;

    @Test
    void shouldUseSpecifiedFolderIfAbsolute() {
        final File absolutePath = new File("zx").getAbsoluteFile();
        assertThat(FileUtil.applyBaseDirIfRelative(new File("xyz"), absolutePath)).isEqualTo(absolutePath);
    }

    @Test
    void shouldUseSpecifiedFolderIfBaseDirIsEmpty() {
        assertThat(FileUtil.applyBaseDirIfRelative(new File(""), new File("zx"))).isEqualTo(new File("zx"));
    }

    @Test
    void shouldAppendToDefaultIfRelative() {
        final File relativepath = new File("zx");
        assertThat(FileUtil.applyBaseDirIfRelative(new File("xyz"), relativepath)).isEqualTo(new File("xyz", relativepath.getPath()));
    }

    @Test
    void shouldUseDefaultIfActualIsNull() {
        final File baseFile = new File("xyz");
        assertThat(FileUtil.applyBaseDirIfRelative(baseFile, null)).isEqualTo(baseFile);
    }

    @Test
    void shouldDetectSubfolders() throws Exception {
        assertThat(isSubdirectoryOf(new File("a"), new File("a"))).isTrue();
        assertThat(isSubdirectoryOf(new File("a"), new File("a/b"))).isTrue();
        assertThat(isSubdirectoryOf(new File("a"), new File("aaaa"))).isFalse();
        assertThat(isSubdirectoryOf(new File("a/b/c/d"), new File("a/b/c/d/e"))).isTrue();
        assertThat(isSubdirectoryOf(new File("a/b/c/d/e"), new File("a/b/c/d"))).isFalse();
        assertThat(isSubdirectoryOf(new File("/a/b"), new File("c/d"))).isFalse();
    }

    @Test
    void shouldDetectSubfoldersWhenUsingRelativePaths() throws Exception {
        File parent = new File("/a/b");
        assertThat(isSubdirectoryOf(parent, new File(parent, "../../.."))).isFalse();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldCreateFileURIForFile() {
        assertThat(FileUtil.toFileURI(new File("/var/lib/foo/"))).isEqualTo("file:///var/lib/foo");
        assertThat(FileUtil.toFileURI(new File("/var/a dir with spaces/foo"))).isEqualTo("file:///var/a%20dir%20with%20spaces/foo");
        assertThat(FileUtil.toFileURI(new File("/var/司徒空在此/foo"))).isEqualTo("file:///var/%E5%8F%B8%E5%BE%92%E7%A9%BA%E5%9C%A8%E6%AD%A4/foo");
    }


    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldCreateFileURIForFileOnWindows() {
        assertThat(FileUtil.toFileURI(new File("c:\\foo")).startsWith("file:///c:/foo")).isTrue();
        assertThat(FileUtil.toFileURI(new File("c:\\a dir with spaces\\foo")).startsWith("file:///c:/a%20dir%20with%20spaces/foo")).isTrue();
    }

    @Test
    void folderIsEmptyWhenItHasNoContents() {
        assertThat(FileUtil.isFolderEmpty(folder)).isTrue();
    }

    @Test
    void FolderIsNotEmptyWhenItHasContents() throws Exception {
        new File(folder, "subfolder").createNewFile();
        assertThat(FileUtil.isFolderEmpty(folder)).isFalse();
    }

    @Test
    void shouldReturnCanonicalPath() throws IOException {
        assertThat(FileUtil.getCanonicalPath(folder)).isEqualTo(folder.getCanonicalPath());
        File spyFile = spy(new File("/xyz/non-existent-file"));
        IOException canonicalPathException = new IOException("Failed to build the canonical path");
        doThrow(canonicalPathException).when(spyFile).getCanonicalPath();
        assertThatThrownBy(() -> FileUtil.getCanonicalPath(spyFile))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(canonicalPathException);
    }
}
