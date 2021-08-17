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
package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class SvnMaterialMockitoTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    SubversionRevision revision = new SubversionRevision("1");
    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

    private File createSvnWorkingCopy(boolean withDotSvnFolder) throws IOException {
        File folder = temporaryFolder.newFolder("testSvnWorkingCopy");
        if (withDotSvnFolder) {
            File dotSvnFolder = new File(folder, ".svn");
            dotSvnFolder.mkdir();
        }
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
