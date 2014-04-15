/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FileDigesterTest {
    private List<File> tempFiles = new ArrayList<File>();

    @After
    public void tearDown() throws Exception {
        for (File tempFile : tempFiles) {
            tempFile.delete();
        }
    }

    private File createFileWithSampleData() throws IOException {
        File tempFile = TestFileUtil.createTempFile("test.txt");
        FileUtil.writeContentToFile("sample data", tempFile);
        return tempFile;
    }

    @Test
    public void shouldReturnSameMd5ForSameData() throws Exception {
        String digest1 = FileDigester.md5DigestOfFile(createFileWithSampleData());
        assertThat("md5 should not be empty", digest1.length() > 0, is(true));
        String digest2 = FileDigester.md5DigestOfFile(createFileWithSampleData());
        assertThat(digest1, is(digest2));
    }

    @Test
    public void shouldThrowExceptionIfITryToGetMd5WithoutDigestingFile() {
        FileDigester fileDigester = new FileDigester(null, null);
        try {
            fileDigester.md5();
            fail("Should have thrown an invalid state exception");
        } catch (Exception e) {

        }
    }
}
