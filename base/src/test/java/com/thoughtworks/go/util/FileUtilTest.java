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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static com.thoughtworks.go.util.FileUtil.isSubdirectoryOf;
import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilTest {

    @TempDir
    File folder;

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
    void folderIsEmptyWhenItHasNoContents() {
        assertThat(FileUtil.isFolderEmpty(folder)).isTrue();
    }

    @Test
    void folderIsNotEmptyWhenItHasContents() throws Exception {
        new File(folder, "subfolder").createNewFile();
        assertThat(FileUtil.isFolderEmpty(folder)).isFalse();
    }

    @Test
    void deleteQuietlyShouldDeleteAFile() throws Exception {
        File file = new File(folder, "file.txt");
        assertThat(file.createNewFile()).isTrue();

        assertThat(FileUtil.deleteQuietly(file)).isTrue();
        assertThat(file).doesNotExist();
    }

    @Test
    void deleteQuietlyShouldDeleteADirectoryRecursively() throws Exception {
        File dir = new File(folder, "dir");
        FileUtil.createFilesByPath(dir, "a.txt", "sub/b.txt", "sub/subsub/c.txt", "empty/");

        assertThat(FileUtil.deleteQuietly(dir)).isTrue();
        assertThat(dir).doesNotExist();
    }

    @Test
    void deleteQuietlyShouldNotThrowForMissingOrNullFiles() {
        assertThat(FileUtil.deleteQuietly(null)).isFalse();
        assertThat(FileUtil.deleteQuietly(new File(folder, "does-not-exist"))).isFalse();
    }

    @Test
    void touchShouldCreateFileAndMissingParentDirs() throws Exception {
        File file = new File(folder, "parent/child/file.txt");

        FileUtil.touch(file);
        assertThat(file).exists().isFile();
    }

    @Test
    void touchShouldUpdateLastModifiedTimeOfExistingFile() throws Exception {
        File file = new File(folder, "file.txt");
        Files.writeString(file.toPath(), "contents");
        assertThat(file.setLastModified(System.currentTimeMillis() - 60_000)).isTrue();
        long originalLastModified = file.lastModified();

        FileUtil.touch(file);
        assertThat(file.lastModified()).isGreaterThan(originalLastModified);
        assertThat(Files.readString(file.toPath())).isEqualTo("contents");
    }
}
