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

package com.thoughtworks.go.domain.materials.svn;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Ignore("CS - This test is currently failing on a WindowsXP box for an unknown reason.")
public class SvnLongFileNamesTest {
    private File workingDir;
    private File workingDir2;
    private SvnTestRepo repo;

    @Before
    public void createDirectories() throws IOException {
        repo = new SvnTestRepo();
        workingDir = TestFileUtil.createTempFolder("working-" + System.currentTimeMillis());
        workingDir2 = TestFileUtil.createTempFolder("working2-" + System.currentTimeMillis());
    }

    @After
    public void removeDirectories() throws IOException {
        repo.tearDown();
        FileUtil.deleteFolder(workingDir);
        FileUtil.deleteFolder(workingDir2);
    }

    @Test
    public void shouldSupportVeryLongFileNames() throws IOException {
        InMemoryStreamConsumer output = null;
        try {
            SvnCommand svn = new SvnCommand(null, repo.projectRepositoryUrl());

            output = inMemoryConsumer();
            svn.checkoutTo(output, workingDir, SubversionRevision.HEAD);

            File root = new File(workingDir, "root");
            root.mkdir();
            File deeplyNestedFile = createDeeplyNestedFile(root);

            assertThat(deeplyNestedFile.exists(), is(true));
            assertThat(deeplyNestedFile.getAbsolutePath().length(), is(greaterThan(260)));

            svn.add(output, root);
            svn.commit(output, workingDir, "Checking in deeply nested file.");

            svn.checkoutTo(output, workingDir2, SubversionRevision.HEAD);
            String deepPath = deeplyNestedFile.getAbsolutePath().substring(workingDir.getAbsolutePath().length());
            File checkedOut = new File(workingDir2, deepPath);
            assertThat(checkedOut.exists(), is(true));
            assertThat(checkedOut.getAbsolutePath().length(), is(greaterThan(260)));

        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    private File createDeeplyNestedFile(File workingDir) throws IOException {
        File previous = workingDir;
        for (int i = 0; i < 30; i++) {
            File newDir = new File(previous, "0123456789-" + i);
            newDir.mkdir();
            previous = newDir;
        }
        File textFile = new File(previous, "somefile.txt");
        FileUtil.writeContentToFile("This is a test", textFile);
        return textFile;
    }
}
