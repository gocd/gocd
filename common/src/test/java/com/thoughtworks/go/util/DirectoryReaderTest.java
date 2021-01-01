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

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import com.thoughtworks.go.domain.DirectoryEntry;
import com.thoughtworks.go.domain.FolderDirectoryEntry;
import com.thoughtworks.go.domain.JobIdentifier;
import static org.hamcrest.Matchers.is;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;


public class DirectoryReaderTest {
    private File testFolder;
    private JobIdentifier jobIdentifier;
    private String folderRoot;

    @Before
    public void setUp() throws IOException {
        testFolder = TestFileUtil.createTempFolder("testFiles");
        jobIdentifier = new JobIdentifier("pipelineName", -1, "LATEST", "stageName", "LATEST", "buildName", 123L);
        folderRoot = "/" + testFolder.getName();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(testFolder);
    }

    @Test
    public void shouldNotDieIfGivenBogusPath() throws Exception {
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(new File("totally bogus path!!!"), "");
        assertThat(entries.size(), is(0));
    }

    @Test
    public void shouldNotDieIfGivenBogusFile() {
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(null, "");
        assertThat(entries.size(), is(0));
    }

    @Test
    public void shouldGetFileList() throws Exception {
        String filename = "text.html$%";
        TestFileUtil.createTestFile(testFolder, filename);
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        assertThat(entries.size(), is(1));
        assertThat(entries.get(0).getFileName(), is(filename));
        assertThat(entries.get(0).getUrl(),
                is("/files/pipelineName/LATEST/stageName/LATEST/buildName" + folderRoot + "/"
                        + URLEncoder.encode(filename)));
    }

    @Test
    public void shouldGetSubSubFolder() throws Exception {
        TestFileUtil.createTestFile(TestFileUtil.createTestFolder(TestFileUtil.createTestFolder(testFolder, "primate"), "monkey"), "baboon.html");
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        FolderDirectoryEntry folder = (FolderDirectoryEntry) entries.get(0);
        assertThat(folder.getFileName(), is("primate"));
        FolderDirectoryEntry subFolder = (FolderDirectoryEntry) folder.getSubDirectory().get(0);
        assertThat(subFolder.getFileName(), is("monkey"));
        assertThat(subFolder.getSubDirectory().get(0).getFileName(), is("baboon.html"));
        assertThat(subFolder.getSubDirectory().get(0).getUrl(),
                is("/files/pipelineName/LATEST/stageName/LATEST/buildName"
                        + folderRoot + "/primate/monkey/baboon.html"));
    }

    @Test
    public void shouldGetListOfFilesAndFolders() throws Exception {
        TestFileUtil.createTestFile(testFolder, "text.html");
        File subFolder = TestFileUtil.createTestFolder(testFolder, "primate");
        TestFileUtil.createTestFile(subFolder, "baboon.html");
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        assertThat(entries.size(), is(2));
        FolderDirectoryEntry folder = (FolderDirectoryEntry) entries.get(0);
        assertThat(folder.getFileName(), is("primate"));
        assertThat(folder.getUrl(), is("/files/pipelineName/LATEST/stageName/LATEST/buildName"
                + folderRoot + "/primate"));
        assertThat(entries.get(1).getFileName(), is("text.html"));
        assertThat(folder.getSubDirectory().get(0).getFileName(), is("baboon.html"));
        assertThat(folder.getSubDirectory().get(0).getUrl(),
                is("/files/pipelineName/LATEST/stageName/LATEST/buildName" + folderRoot + "/primate/baboon.html"));
    }

    @Test
    public void shouldGetListOfFilesWithDirectoriesFirstAndFilesInAlphabeticOrder() throws Exception {
        TestFileUtil.createTestFile(testFolder, "build.html");
        File subFolder = TestFileUtil.createTestFolder(testFolder, "testoutput");
        TestFileUtil.createTestFile(subFolder, "baboon.html");
        TestFileUtil.createTestFile(subFolder, "apple.html");
        TestFileUtil.createTestFile(subFolder, "pear.html");
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        assertThat(entries.size(), is(2));
        FolderDirectoryEntry folder = (FolderDirectoryEntry) entries.get(0);
        assertThat(folder.getFileName(), is("testoutput"));
        assertThat(entries.get(1).getFileName(), is("build.html"));
        assertThat(folder.getSubDirectory().get(0).getFileName(), is("apple.html"));
        assertThat(folder.getSubDirectory().get(1).getFileName(), is("baboon.html"));
        assertThat(folder.getSubDirectory().get(2).getFileName(), is("pear.html"));
    }

    @Test
    public void shouldNotContainSerializedObjectFile() throws Exception {
        String filename = ".log200806041535.xml.ser";
        TestFileUtil.createTestFile(testFolder, filename);
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        assertThat(entries.size(), is(0));
    }

    @Test public void shouldKeepRootsInUrl() throws Exception {
        File b = TestFileUtil.createTestFolder(testFolder, "b");
        TestFileUtil.createTestFile(b, "c.xml");
        List<DirectoryEntry> entries = new DirectoryReader(jobIdentifier).listEntries(b, folderRoot + "/b");
        assertThat(entries.size(), is(1));
        String expectedUrl = "/files/pipelineName/LATEST/stageName/LATEST/buildName/"
                + testFolder.getName() + "/b/c.xml";
        assertThat(entries.get(0).getUrl(), is(expectedUrl));
    }

}
