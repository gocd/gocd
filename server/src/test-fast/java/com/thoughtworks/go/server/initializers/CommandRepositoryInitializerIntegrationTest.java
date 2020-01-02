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
package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CommandRepositoryInitializerIntegrationTest {
    private CommandRepositoryInitializer initializer;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        initializer = new CommandRepositoryInitializer(new SystemEnvironment(), new ZipUtil(), null);
    }

    @Test
    public void shouldReplaceWithPackagedCommandRepositoryWhenOlderRepoExists() throws Exception {
        File versionFile = TestFileUtil.writeStringToTempFileInFolder("default", "version.txt", "10.1=10");
        File randomFile = TestFileUtil.createTestFile(versionFile.getParentFile(), "random");
        File defaultCommandRepoDir = versionFile.getParentFile();

        initializer.usePackagedCommandRepository(new ZipInputStream(new FileInputStream(getZippedCommandRepo("12.4=12"))), defaultCommandRepoDir);

        assertThat(defaultCommandRepoDir.exists(), is(true));
        assertThat(FileUtils.readFileToString(new File(defaultCommandRepoDir, "version.txt"), UTF_8), is("12.4=12"));
        assertThat(new File(defaultCommandRepoDir, "snippet.xml").exists(), is(true));
        assertThat(new File(defaultCommandRepoDir, randomFile.getName()).exists(), is(false));
    }

    @Test
    public void shouldNotDeleteCustomCommandRepositoryWhenUpdatingDefaultCommandRepository() throws Exception {
        File versionFile = TestFileUtil.writeStringToTempFileInFolder("default", "version.txt", "10.1=10");
        File randomFile = TestFileUtil.createTestFile(versionFile.getParentFile(), "random");
        File defaultCommandRepoDir = versionFile.getParentFile();
        File customCommandRepoDir = TestFileUtil.createTestFolder(new File(defaultCommandRepoDir.getParent()),"customDir");
        File userFile = TestFileUtil.createTestFile(customCommandRepoDir, "userFile");

        initializer.usePackagedCommandRepository(new ZipInputStream(new FileInputStream(getZippedCommandRepo("12.4=12"))), defaultCommandRepoDir);

        assertThat(defaultCommandRepoDir.exists(), is(true));
        assertThat(FileUtils.readFileToString(new File(defaultCommandRepoDir, "version.txt"), UTF_8), is("12.4=12"));
        assertThat(new File(defaultCommandRepoDir, "snippet.xml").exists(), is(true));
        assertThat(new File(defaultCommandRepoDir, randomFile.getName()).exists(), is(false));
        assertThat(customCommandRepoDir.exists(), is(true));
        assertThat(new File(customCommandRepoDir, userFile.getName()).exists(), is(true));
    }

    @Test
    public void shouldCreateCommandRepositoryWhenNoPreviousRepoExists() throws Exception {
        File defaultCommandRepoDir = TestFileUtil.createTempFolder("default");

        initializer.usePackagedCommandRepository(new ZipInputStream(new FileInputStream(getZippedCommandRepo("12.4=12"))), defaultCommandRepoDir);

        assertThat(defaultCommandRepoDir.exists(), is(true));

        assertThat(FileUtils.readFileToString(new File(defaultCommandRepoDir, "version.txt"), UTF_8), is("12.4=12"));
        assertThat(new File(defaultCommandRepoDir, "snippet.xml").exists(), is(true));
    }

    private File getZippedCommandRepo(final String versionText) throws Exception {
        File dir = TestFileUtil.createTempFolder("default");
        File versionFile = TestFileUtil.createTestFile(dir, "version.txt");
        FileUtils.writeStringToFile(versionFile, versionText, UTF_8);
        TestFileUtil.createTestFile(dir, "snippet.xml");

        File zipFile = TestFileUtil.createTempFile("defaultCommandSnippets.zip");
        new ZipUtil().zipFolderContents(dir, zipFile);
        return zipFile;
    }
}
