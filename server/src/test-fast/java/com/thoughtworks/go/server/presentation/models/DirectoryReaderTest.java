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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.domain.DirectoryEntry;
import com.thoughtworks.go.domain.FolderDirectoryEntry;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryReaderTest {
    @TempDir
    File testFolder;

    private JobIdentifier jobIdentifier;
    private String folderRoot;

    @BeforeEach
    public void setUp() {
        jobIdentifier = new JobIdentifier("pipelineName", -1, "LATEST", "stageName", "LATEST", "buildName", 123L);
        folderRoot = "/" + testFolder.getName();
    }

    @Test
    public void shouldNotDieIfGivenBogusPath() {
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(new File("totally bogus path!!!"), "");
        assertThat(entries.size()).isEqualTo(0);
    }

    @Test
    public void shouldNotDieIfGivenBogusFile() {
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(null, "");
        assertThat(entries.size()).isEqualTo(0);
    }

    @Test
    public void shouldGetFileList() throws Exception {
        String filename = "text.html$%";
        TestFileUtil.createTestFile(testFolder, filename);
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        assertThat(entries.size()).isEqualTo(1);
        assertThat(entries.get(0).getFileName()).isEqualTo(filename);
        assertThat(entries.get(0).getUrl()).isEqualTo("/files/pipelineName/LATEST/stageName/LATEST/buildName" + folderRoot + "/"
                + URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }

    @Test
    public void shouldGetSubSubFolder() throws Exception {
        TestFileUtil.createTestFile(TestFileUtil.createTestFolder(TestFileUtil.createTestFolder(testFolder, "primate"), "monkey"), "baboon.html");
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        FolderDirectoryEntry folder = (FolderDirectoryEntry) entries.get(0);
        assertThat(folder.getFileName()).isEqualTo("primate");
        FolderDirectoryEntry subFolder = (FolderDirectoryEntry) folder.getSubDirectory().get(0);
        assertThat(subFolder.getFileName()).isEqualTo("monkey");
        assertThat(subFolder.getSubDirectory().get(0).getFileName()).isEqualTo("baboon.html");
        assertThat(subFolder.getSubDirectory().get(0).getUrl()).isEqualTo("/files/pipelineName/LATEST/stageName/LATEST/buildName"
                + folderRoot + "/primate/monkey/baboon.html");
    }

    @Test
    public void shouldGetListOfFilesAndFolders() throws Exception {
        TestFileUtil.createTestFile(testFolder, "text.html");
        File subFolder = TestFileUtil.createTestFolder(testFolder, "primate");
        TestFileUtil.createTestFile(subFolder, "baboon.html");
        DirectoryReader reader = new DirectoryReader(jobIdentifier);
        List<DirectoryEntry> entries = reader.listEntries(testFolder, folderRoot);
        assertThat(entries.size()).isEqualTo(2);
        FolderDirectoryEntry folder = (FolderDirectoryEntry) entries.get(0);
        assertThat(folder.getFileName()).isEqualTo("primate");
        assertThat(folder.getUrl()).isEqualTo("/files/pipelineName/LATEST/stageName/LATEST/buildName"
            + folderRoot + "/primate");
        assertThat(entries.get(1).getFileName()).isEqualTo("text.html");
        assertThat(folder.getSubDirectory().get(0).getFileName()).isEqualTo("baboon.html");
        assertThat(folder.getSubDirectory().get(0).getUrl()).isEqualTo("/files/pipelineName/LATEST/stageName/LATEST/buildName" + folderRoot + "/primate/baboon.html");
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
        assertThat(entries.size()).isEqualTo(2);
        FolderDirectoryEntry folder = (FolderDirectoryEntry) entries.get(0);
        assertThat(folder.getFileName()).isEqualTo("testoutput");
        assertThat(entries.get(1).getFileName()).isEqualTo("build.html");
        assertThat(folder.getSubDirectory().get(0).getFileName()).isEqualTo("apple.html");
        assertThat(folder.getSubDirectory().get(1).getFileName()).isEqualTo("baboon.html");
        assertThat(folder.getSubDirectory().get(2).getFileName()).isEqualTo("pear.html");
    }

    @Test
    public void shouldKeepRootsInUrl() throws Exception {
        File b = TestFileUtil.createTestFolder(testFolder, "b");
        TestFileUtil.createTestFile(b, "c.xml");
        List<DirectoryEntry> entries = new DirectoryReader(jobIdentifier).listEntries(b, folderRoot + "/b");
        assertThat(entries.size()).isEqualTo(1);
        String expectedUrl = "/files/pipelineName/LATEST/stageName/LATEST/buildName/"
            + testFolder.getName() + "/b/c.xml";
        assertThat(entries.get(0).getUrl()).isEqualTo(expectedUrl);
    }

    @Nested
    public class DirectoriesFirstFileNameOrderTest {

        private final File file1 = new FileStub("a", false);
        private final File file2 = new FileStub("b", false);
        private final File folder1 = new FileStub("c", true);
        private final File folder2 = new FileStub("d", true);

        private final DirectoryReader.DirectoriesFirstFileNameOrder comparator = new DirectoryReader.DirectoriesFirstFileNameOrder();

        @Test
        public void shouldBeAlphabeticForSameType() {
            assertThat(comparator.compare(file1, file2) < 0).isTrue();
            assertThat(comparator.compare(folder1, folder2) < 0).isTrue();
        }

        @Test
        public void folderShouldBeLessThanFile() {
            assertThat(comparator.compare(file1, folder1) > 0).isTrue();
        }
    }

    private static class FileStub extends File {
        private final boolean directory;

        public FileStub(String name, boolean isDirectory) {
            super(name);
            directory = isDirectory;
        }

        @Override
        public boolean isDirectory() {
            return directory;
        }

        @Override
        public boolean isFile() {
            return !directory;
        }
    }

}
