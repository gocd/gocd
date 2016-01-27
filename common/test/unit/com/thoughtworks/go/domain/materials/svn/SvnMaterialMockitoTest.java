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
import java.util.ArrayList;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.After;
import org.junit.Test;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SvnMaterialMockitoTest {

    SubversionRevision revision = new SubversionRevision("1");
    private final ArrayList<File> tempFiles = new ArrayList<File>();
    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

    @After
    public void tearDown() throws Exception {
        for (File tempFile : tempFiles) {
            tempFile.delete();
        }
    }

    private File createSvnWorkingCopy(boolean withDotSvnFolder) {
        File folder = TestFileUtil.createTempFolder("testSvnWorkingCopy");
        if (withDotSvnFolder) {
            File dotSvnFolder = new File(folder, ".svn");
            dotSvnFolder.mkdir();
            tempFiles.add(dotSvnFolder);
        }
        tempFiles.add(folder);
        return folder;
    }

    @Test
    public void shouldNotDeleteWorkingDirIfSvnRepositoryUsesFileProtocol() throws IOException {
        Subversion subversion = mock(Subversion.class);
        when(subversion.getUserName()).thenReturn("");
        when(subversion.getPassword()).thenReturn("");
        when(subversion.isCheckExternals()).thenReturn(false);

        File workingCopy = createSvnWorkingCopy(true);
        when(subversion.workingRepositoryUrl(workingCopy)).thenReturn(workingCopy.getPath());

        String url = "file://" + workingCopy.getPath();
        when(subversion.getUrl()).thenReturn(new UrlArgument(url));
        SvnMaterial svnMaterial = SvnMaterial.createSvnMaterialWithMock(subversion);
        svnMaterial.setUrl(url);
        svnMaterial.updateTo(outputStreamConsumer, workingCopy, new RevisionContext(revision), new TestSubprocessExecutionContext());

        assertThat(workingCopy.exists(), is(true));
        verify(subversion).updateTo(outputStreamConsumer, workingCopy, revision);
    }
}
