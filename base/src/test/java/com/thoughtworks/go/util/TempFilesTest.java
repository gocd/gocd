/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TempFilesTest {
    TempFiles files;
    private Properties original;

    @Before
    public void setUp() {
        original = new Properties();
        original.putAll(System.getProperties());
        files = new TempFiles();
    }

    @After
    public void cleanUp() {
        System.setProperties(original);
        files.cleanUp();
    }

    @Test
    public void shouldRecordFilesThatAreCreated() throws IOException {
        File created = files.createFile("foo");

        assertThat(created.exists(), is(true));
        files.cleanUp();

        assertThat(created.exists(), is(false));
    }

    @Test
    public void shouldRecordFoldersThatAreCreated() {
        File dir = files.mkdir("foo");
        assertThat(dir.exists(), is(true));

        files.cleanUp();
        assertThat(dir.exists(), is(false));
    }

    @Test
    public void shouldDeleteNonEmptyFolders() throws IOException {
        File dir = files.mkdir("foo");
        assertThat(dir.exists(), is(true));

        File file = new File(dir, "foo");
        file.createNewFile();

        files.cleanUp();
        assertThat(dir.exists(), is(false));
    }

    @Test
    public void shouldForgetFolders() throws IOException {
        File file = files.mkdir("foo");

        files.cleanUp();
        files.cleanUp();
    }

    @Test
    public void shouldCreateFilesInTempDirectory() throws IOException {
        File file = files.createFile("foo");
        File parentFile = file.getParentFile();
        assertThat(parentFile.getName(), is("cruise"));
        assertThat(parentFile.getParentFile(), is(tmpDir()));
    }

    private File tmpDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void shouldCreateDirsInTempDirectory() throws IOException {
        File dir = files.mkdir("foo");
        File parentFile = dir.getParentFile();
        assertThat(parentFile.getName(), is("cruise"));
        assertThat(parentFile.getParentFile(), is(tmpDir()));
    }

    @Test
    public void shouldCreateUniqueFilesEveryTime() throws IOException {
        TestingClock clock = new TestingClock();
        files.setClock(clock);
        File file1 = files.createUniqueFile("foo");
        File file2 = files.createUniqueFile("foo");
        assertThat(file1, not(file2));
    }

    @Test
    public void shouldCreateUniqueFilesParentDirectoryIfDoesNotExist() throws IOException {
        String newTmpDir = original.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID();
        System.setProperty("java.io.tmpdir", newTmpDir);
        File file = files.createUniqueFile("foo");
        assertThat(file.getParentFile().exists(), is(true));
    }

    @Test
    public void shouldCreateUniqueFolders() throws IOException {
        TestingClock clock = new TestingClock();
        files.setClock(clock);
        File file1 = files.createUniqueFolder("foo");
        clock.addSeconds(1);
        File file2 = files.createUniqueFolder("foo");
        assertThat(file2, not(file1));
    }

    @Test
    public void willNotDeleteParentDirectoriesIfPathologicalFilesGetCreated() throws IOException {
        File file1 = files.createFile("foo/bar/baz.zip");
        assertThat(file1.exists(), is(true));
        files.cleanUp();
        assertThat(file1.exists(), is(false));
        assertThat(new File(new File(tmpDir(), "cruise"), "foo").exists(), is(true));
    }
}
