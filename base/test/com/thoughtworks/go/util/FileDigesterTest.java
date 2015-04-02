/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FileDigesterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        temporaryFolder.create();
    }

    @After
    public void tearDown() throws Exception {
        temporaryFolder.delete();
    }

    @Test
    public void shouldReturnSameMd5ForSameData() throws Exception {
        String digest1 = FileDigester.md5DigestOfFile(createFileWithSampleData("test1.txt"));
        assertThat("md5 should not be empty", digest1.length() > 0, is(true));
        String digest2 = FileDigester.md5DigestOfFile(createFileWithSampleData("test2.txt"));
        assertThat(digest1, is(digest2));
    }

    @Test
    public void shouldReturnSameMd5ForSameFolderContents() throws Exception {
        String digest1 = FileDigester.md5DigestOfFolderContent(createFolderWithSampleData("test1", 3));
        assertThat("md5 should not be empty", digest1.length() > 0, is(true));
        temporaryFolder.delete();
        temporaryFolder.create();
        String digest2 = FileDigester.md5DigestOfFolderContent(createFolderWithSampleData("test2", 3));
        assertThat(digest1, is(digest2));
    }

    @Test
    public void shouldReturnConsistentMd5BySortingTheFileList() throws Exception {
        String digest1 = FileDigester.md5DigestOfFolderContent(createFolderWithSampleData("test", 3));
        assertThat("md5 should not be empty", digest1.length() > 0, is(true));
        assertThat(digest1, is("SCRRYY4W1zLuiA++CSuy6A=="));
    }

    @Test
    public void shouldThrowExceptionIfITryToGetMd5WithoutDigestingFile() {
        FileDigester fileDigester = new FileDigester(null, null);
        try {
            fileDigester.md5();
            fail("Should have thrown an invalid state exception");
        } catch (Exception ignored) {
        }
    }

    private File createFileWithSampleData(String fileName) throws IOException {
        File tempFile = temporaryFolder.newFile(fileName);
        FileUtil.writeContentToFile("sample data", tempFile);
        return tempFile;
    }

    private File createFolderWithSampleData(String fileNamePrefix, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            File file = temporaryFolder.newFile(fileNamePrefix + i);
            FileUtil.writeContentToFile("sample plugin for plugin " + count, file);
        }
        return temporaryFolder.getRoot();
    }
}
