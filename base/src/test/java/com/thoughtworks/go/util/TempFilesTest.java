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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TempFilesTest {
    TempFiles files;
    private Properties original;

    @BeforeEach
    public void setUp() {
        original = new Properties();
        original.putAll(System.getProperties());
        files = new TempFiles();
    }

    @AfterEach
    public void cleanUp() {
        System.setProperties(original);
        files.cleanUp();
    }

    @Test
    public void shouldRecordFilesThatAreCreated() throws IOException {
        File created = files.createFile("foo");

        assertThat(created.exists()).isEqualTo(true);
        files.cleanUp();

        assertThat(created.exists()).isEqualTo(false);
    }

    @Test
    public void shouldRecordFoldersThatAreCreated() {
        File dir = files.mkdir("foo");
        assertThat(dir.exists()).isEqualTo(true);

        files.cleanUp();
        assertThat(dir.exists()).isEqualTo(false);
    }

    @Test
    public void shouldDeleteNonEmptyFolders() throws IOException {
        File dir = files.mkdir("foo");
        assertThat(dir.exists()).isEqualTo(true);

        File file = new File(dir, "foo");
        file.createNewFile();

        files.cleanUp();
        assertThat(dir.exists()).isEqualTo(false);
    }

    @Test
    public void shouldForgetFolders() {
        files.mkdir("foo");

        files.cleanUp();
        files.cleanUp();
    }

    @Test
    public void shouldCreateFilesInTempDirectory() throws IOException {
        File file = files.createFile("foo");
        File parentFile = file.getParentFile();
        assertThat(parentFile.getName()).isEqualTo("cruise");
        assertThat(parentFile.getParentFile()).isEqualTo(tmpDir());
    }

    private File tmpDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void shouldCreateDirsInTempDirectory() {
        File dir = files.mkdir("foo");
        File parentFile = dir.getParentFile();
        assertThat(parentFile.getName()).isEqualTo("cruise");
        assertThat(parentFile.getParentFile()).isEqualTo(tmpDir());
    }

    @Test
    public void shouldCreateUniqueFilesEveryTime() {
        TestingClock clock = new TestingClock();
        files.setClock(clock);
        File file1 = files.createUniqueFile("foo");
        File file2 = files.createUniqueFile("foo");
        assertThat(file1).isNotEqualTo(file2);
    }

    @Test
    public void shouldCreateUniqueFilesParentDirectoryIfDoesNotExist() {
        String newTmpDir = original.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID();
        System.setProperty("java.io.tmpdir", newTmpDir);
        File file = files.createUniqueFile("foo");
        assertThat(file.getParentFile().exists()).isEqualTo(true);
    }

    @Test
    public void shouldCreateUniqueFolders() {
        TestingClock clock = new TestingClock();
        files.setClock(clock);
        File file1 = files.createUniqueFolder("foo");
        clock.addSeconds(1);
        File file2 = files.createUniqueFolder("foo");
        assertThat(file2).isNotEqualTo(file1);
    }

    @Test
    public void willNotDeleteParentDirectoriesIfPathologicalFilesGetCreated() throws IOException {
        File file1 = files.createFile("foo/bar/baz.zip");
        assertThat(file1.exists()).isEqualTo(true);
        files.cleanUp();
        assertThat(file1.exists()).isEqualTo(false);
        assertThat(new File(new File(tmpDir(), "cruise"), "foo").exists()).isEqualTo(true);
    }
}
