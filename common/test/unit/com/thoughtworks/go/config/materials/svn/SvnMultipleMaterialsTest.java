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

package com.thoughtworks.go.config.materials.svn;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.config.MaterialRevisionsMatchers.containsModifiedFile;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SvnMultipleMaterialsTest {
    private SvnTestRepo repo;
    private File pipelineDir;

    @Before
    public void createRepo() throws IOException {
        repo = new SvnTestRepo();
        pipelineDir = TestFileUtil.createTempFolder("working-dir-" + UUID.randomUUID());
    }

    @After
    public void cleanupRepo() {
        repo.tearDown();
        FileUtil.deleteFolder(pipelineDir);
    }

    @Test
    public void shouldNotThrowNPEIfTheWorkingDirectoryIsEmpty() throws Exception {
        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        Revision revision = latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext());
        updateMaterials(materials, revision);

        FileUtil.deleteFolder(pipelineDir);

        updateMaterials(materials, revision);
    }

    @Test
    public void shouldClearOutAllDirectoriesIfMaterialsChange() throws Exception {

        SvnMaterial part1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial part2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(part1, part2);

        Revision revision = latestRevision(part1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists(), is(true));
        assertThat(new File(pipelineDir, "part2").exists(), is(true));

        SvnMaterial newFolder = repo.createMaterial("multiple-materials/trunk/part2", "newFolder");

        Materials changedMaterials = new Materials(part1, newFolder);

        updateMaterials(changedMaterials, revision);

        assertThat(new File(pipelineDir, "part1").exists(), is(true));
        assertThat(new File(pipelineDir, "newFolder").exists(), is(true));
    }

    @Test
    public void shouldClearOutAllDirectoriesIfMaterialIsDeleted() throws Exception {

        SvnMaterial part1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial part2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(part1, part2);

        Revision revision = latestRevision(part1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists(), is(true));
        assertThat(new File(pipelineDir, "part2").exists(), is(true));

        Materials changedMaterials = new Materials(part1);

        updateMaterials(changedMaterials, revision);

        assertThat(new File(pipelineDir, "part1").exists(), is(true));
        assertThat(new File(pipelineDir, "part2").exists(), is(false));
    }


    @Test
    public void shouldNotDeleteDirectoriesWhenThereIsACruiseOutputDirectory() throws Exception {
        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        Revision revision = latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists(), is(true));
        assertThat(new File(pipelineDir, "part2").exists(), is(true));

        File testFile = new File(pipelineDir, "part1/test-file");
        testFile.createNewFile();
        assertThat(testFile.exists(), is(true));
        //simulates what a build will do
        TestFileUtil.createTestFolder(pipelineDir, ArtifactLogUtil.CRUISE_OUTPUT_FOLDER);
        assertThat(pipelineDir.listFiles().length, is(3));

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists(), is(true));
        assertThat(new File(pipelineDir, "part2").exists(), is(true));

        assertThat("Should not delete the part1 directory", testFile.exists(), is(true));
    }

    @Test
    public void shouldNotDeleteWorkingDirIfThereAreNoRequiredFolders() throws Exception {
        SvnMaterial part = repo.createMaterial("multiple-materials/trunk/part1", null);

        Materials materials = new Materials(part);

        Revision revision = latestRevision(part, pipelineDir, new TestSubprocessExecutionContext());
        updateMaterials(materials, revision);

        File shouldNotCleanUp = new File(pipelineDir, "shouldNotDelete");
        shouldNotCleanUp.createNewFile();
        assertThat(shouldNotCleanUp.exists(), is(true));

        updateMaterials(materials, revision);
        assertThat("should not clean up working dir for this pipeline if none of the materials specified a sub folder",
                shouldNotCleanUp.exists(), is(true));
    }

    //This is bug #2320 - Cruise doing full checkouts most times
    @Test
    public void shouldNotDeleteWorkingDirIfMaterialsAreCheckedOutToSubfoldersWithTheSameRootBug2320() throws Exception {
        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "root/part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "root/part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        Revision revision = latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "root/part1").exists(), is(true));
        assertThat(new File(pipelineDir, "root/part2").exists(), is(true));

        File testFile = new File(pipelineDir, "root/part1/test-file");
        testFile.createNewFile();
        assertThat(testFile.exists(), is(true));
        //simulates what a build will do
        TestFileUtil.createTestFolder(pipelineDir, ArtifactLogUtil.CRUISE_OUTPUT_FOLDER);
        assertThat(pipelineDir.listFiles().length, is(2));

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "root/part1").exists(), is(true));
        assertThat(new File(pipelineDir, "root/part2").exists(), is(true));

        assertThat("Should not delete the part1 directory", testFile.exists(), is(true));
    }

    @Test
    public void shouldDetectLatestModifications() throws Exception {

        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        MaterialRevisions materialRevisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        MaterialRevision revision1 = materialRevisions.getMaterialRevision(0);
        assertThat(revision1.getRevision(), is(latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext())));

        MaterialRevision revision2 = materialRevisions.getMaterialRevision(1);
        assertThat(revision2.getRevision(), is(latestRevision(svnMaterial2, pipelineDir, new TestSubprocessExecutionContext())));
    }

    @Test
    public void shouldUpdateMaterialToItsDestFolder() throws Exception {
        SvnMaterial material1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));
        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext());

        assertThat(new File(pipelineDir, "part1").exists(), is(true));
    }

    @Test
    public void shouldIgnoreDestinationFolderWhenServerSide() throws Exception {
        SvnMaterial material1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));
        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext(true));

        assertThat(new File(pipelineDir, "part1").exists(), is(false));
    }

    @Test
    public void shouldFindModificationForEachMaterial() throws Exception {
        SvnMaterial material1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial material2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");

        Materials materials = new Materials(material1, material2);

        repo.checkInOneFile("filename.txt", material1);
        repo.checkInOneFile("filename2.txt", material2);

        MaterialRevisions materialRevisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        assertThat(materialRevisions.getRevisions().size(), is(2));
        assertThat(materialRevisions, containsModifiedFile("/trunk/part1/filename.txt"));
        assertThat(materialRevisions, containsModifiedFile("/trunk/part2/filename2.txt"));
    }

    private Revision latestRevision(SvnMaterial material, File workingDir, TestSubprocessExecutionContext execCtx) {
        List<Modification> modifications = material.latestModification(workingDir, execCtx);
        return new Modifications(modifications).latestRevision(material);
    }

    private void updateMaterials(Materials materials, Revision revision) {
        ProcessOutputStreamConsumer outputStreamConsumer = inMemoryConsumer();
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        materials.cleanUp(pipelineDir, outputStreamConsumer);
        for (Material material : materials) {
            material.updateTo(outputStreamConsumer, pipelineDir, new RevisionContext(revision), execCtx);
        }
    }
}
