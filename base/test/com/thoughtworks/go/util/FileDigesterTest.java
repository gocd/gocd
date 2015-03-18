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
        File fileWithSampleData = createFileWithSampleData();
        String digest1 = FileDigester.md5DigestOfFile(fileWithSampleData);
        assertThat("md5 should not be empty", digest1.length() > 0, is(true));
        String digest2 = FileDigester.md5DigestOfFile(fileWithSampleData);
        assertThat(digest1, is(digest2));
    }

    @Test
    public void shouldReturnSameMd5ForFolderContents() throws Exception {
        File folderWithSampleData = createFolderWithSampleData();
        String digest1 = FileDigester.md5DigestOfFolderContent(folderWithSampleData);
        assertThat("md5 should not be empty", digest1.length() > 0, is(true));
        String digest2 = FileDigester.md5DigestOfFolderContent(folderWithSampleData);
        assertThat(digest1, is(digest2));
    }

    @Test
    public void shouldReturnConsistentMd5BySortingTheFileList() throws Exception {
        File folderWithSampleData = createFolderWithSampleData();
        String digest1 = FileDigester.md5DigestOfFolderContent(folderWithSampleData);
        assertThat("md5 should not be empty", digest1.length() > 0, is(true));
        assertThat(digest1, is("FJ9Q0KO4KE5ukH6Y7r1FIQ=="));
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

    private File createFileWithSampleData() throws IOException {
        File tempFile = temporaryFolder.newFile("test.txt");
        FileUtil.writeContentToFile("sample data", tempFile);
        return tempFile;
    }

    private File createFolderWithSampleData() throws IOException {
        File firstPlugin = temporaryFolder.newFile("first-plugin");
        File secondPlugin = temporaryFolder.newFile("second-plugin");
        File thirdPlugin = temporaryFolder.newFile("third-plugin");
        FileUtil.writeContentToFile("sample plugin for first plugin", firstPlugin);
        FileUtil.writeContentToFile("sample plugin for third plugin", secondPlugin);
        FileUtil.writeContentToFile("sample plugin for second plugin", thirdPlugin);
        return temporaryFolder.getRoot();
    }
}
