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

package com.thoughtworks.go.domain.materials.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.config.MaterialRevisionsMatchers.containsModifiedFile;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GitMultipleMaterialsTest {
    private GitTestRepo repo;
    private File pipelineDir;
    private List<File> toClean = new ArrayList<File>();

    @Before
    public void createRepo() throws IOException {
        repo = new GitTestRepo();
        pipelineDir = TestFileUtil.createTempFolder("working-dir-" + UUID.randomUUID());
        toClean.add(pipelineDir);
    }

    @After
    public void cleanupRepo() {
        repo.tearDown();
        for (File file : toClean) {
            FileUtil.deleteFolder(file);
        }
    }

    @Test
    public void shouldCloneMaterialToItsDestFolder() throws Exception {
        GitMaterial material1 = repo.createMaterial("dest1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));

        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext());

        assertThat(new File(pipelineDir, "dest1").exists(), is(true));
        assertThat(new File(pipelineDir, "dest1/.git").exists(), is(true));
    }

    @Test
    public void shouldIgnoreDestinationFolderWhenCloningMaterialWhenServerSide() throws Exception {
        GitMaterial material1 = repo.createMaterial("dest1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));

        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext(true));

        assertThat(new File(pipelineDir, "dest1").exists(), is(false));
        assertThat(new File(pipelineDir, ".git").exists(), is(true));
    }

    @Test
    public void shouldFindModificationsForBothMaterials() throws Exception {
        Materials materials = new Materials(repo.createMaterial("dest1"), repo.createMaterial("dest2"));

        String fileName = "newFile.txt";
        repo.addFileAndPush(fileName, "add a new file " + fileName);

        MaterialRevisions materialRevisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        assertThat(materialRevisions.getRevisions().size(), is(2));
        assertThat(materialRevisions, containsModifiedFile(fileName));
    }
}