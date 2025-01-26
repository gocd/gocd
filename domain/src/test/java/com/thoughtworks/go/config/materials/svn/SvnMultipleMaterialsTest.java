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
package com.thoughtworks.go.config.materials.svn;

import com.thoughtworks.go.config.MaterialRevisionsMatchers;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.InMemoryConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SvnMultipleMaterialsTest {
    private SvnTestRepo repo;
    private File pipelineDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void createRepo() throws IOException {
        repo = new SvnTestRepo(tempDir);
        pipelineDir = TempDirUtils.createTempDirectoryIn(tempDir, "pipeline").toFile();
    }

    @Test
    public void shouldNotThrowNPEIfTheWorkingDirectoryIsEmpty() {
        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        Revision revision = latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext());
        updateMaterials(materials, revision);

        FileUtils.deleteQuietly(pipelineDir);

        updateMaterials(materials, revision);
    }

    @Test
    public void shouldClearOutAllDirectoriesIfMaterialsChange() {

        SvnMaterial part1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial part2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(part1, part2);

        Revision revision = latestRevision(part1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "part2").exists()).isTrue();

        SvnMaterial newFolder = repo.createMaterial("multiple-materials/trunk/part2", "newFolder");

        Materials changedMaterials = new Materials(part1, newFolder);

        updateMaterials(changedMaterials, revision);

        assertThat(new File(pipelineDir, "part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "newFolder").exists()).isTrue();
    }

    @Test
    public void shouldClearOutAllDirectoriesIfMaterialIsDeleted() {

        SvnMaterial part1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial part2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(part1, part2);

        Revision revision = latestRevision(part1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "part2").exists()).isTrue();

        Materials changedMaterials = new Materials(part1);

        updateMaterials(changedMaterials, revision);

        assertThat(new File(pipelineDir, "part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "part2").exists()).isFalse();
    }


    @Test
    public void shouldNotDeleteDirectoriesWhenThereIsACruiseOutputDirectory() throws Exception {
        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        Revision revision = latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "part2").exists()).isTrue();

        File testFile = new File(pipelineDir, "part1/test-file");
        testFile.createNewFile();
        assertThat(testFile.exists()).isTrue();
        //simulates what a build will do
        new File(pipelineDir, ArtifactLogUtil.CRUISE_OUTPUT_FOLDER).mkdir();
        assertThat(pipelineDir.listFiles().length).isEqualTo(3);

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "part2").exists()).isTrue();

        assertThat(testFile.exists()).isTrue();
    }

    @Test
    public void shouldNotDeleteWorkingDirIfThereAreNoRequiredFolders() throws Exception {
        SvnMaterial part = repo.createMaterial("multiple-materials/trunk/part1", null);

        Materials materials = new Materials(part);

        Revision revision = latestRevision(part, pipelineDir, new TestSubprocessExecutionContext());
        updateMaterials(materials, revision);

        File shouldNotCleanUp = new File(pipelineDir, "shouldNotDelete");
        shouldNotCleanUp.createNewFile();
        assertThat(shouldNotCleanUp.exists()).isTrue();

        updateMaterials(materials, revision);
        assertThat(shouldNotCleanUp.exists()).isTrue();
    }

    //This is bug #2320 - Cruise doing full checkouts most times
    @Test
    public void shouldNotDeleteWorkingDirIfMaterialsAreCheckedOutToSubfoldersWithTheSameRootBug2320() throws Exception {
        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "root/part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "root/part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        Revision revision = latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext());

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "root/part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "root/part2").exists()).isTrue();

        File testFile = new File(pipelineDir, "root/part1/test-file");
        testFile.createNewFile();
        assertThat(testFile.exists()).isTrue();
        //simulates what a build will do
        new File(pipelineDir, ArtifactLogUtil.CRUISE_OUTPUT_FOLDER).mkdir();
        assertThat(pipelineDir.listFiles().length).isEqualTo(2);

        updateMaterials(materials, revision);

        assertThat(new File(pipelineDir, "root/part1").exists()).isTrue();
        assertThat(new File(pipelineDir, "root/part2").exists()).isTrue();

        assertThat(testFile.exists()).isTrue();
    }

    @Test
    public void shouldDetectLatestModifications() {

        SvnMaterial svnMaterial1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial svnMaterial2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");
        Materials materials = new Materials(svnMaterial1, svnMaterial2);

        MaterialRevisions materialRevisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        MaterialRevision revision1 = materialRevisions.getMaterialRevision(0);
        assertThat(revision1.getRevision()).isEqualTo(latestRevision(svnMaterial1, pipelineDir, new TestSubprocessExecutionContext()));

        MaterialRevision revision2 = materialRevisions.getMaterialRevision(1);
        assertThat(revision2.getRevision()).isEqualTo(latestRevision(svnMaterial2, pipelineDir, new TestSubprocessExecutionContext()));
    }

    @Test
    public void shouldUpdateMaterialToItsDestFolder() {
        SvnMaterial material1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));
        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext());

        assertThat(new File(pipelineDir, "part1").exists()).isTrue();
    }

    @Test
    public void shouldIgnoreDestinationFolderWhenServerSide() {
        SvnMaterial material1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));
        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext(true));

        assertThat(new File(pipelineDir, "part1").exists()).isFalse();
    }

    @Test
    public void shouldFindModificationForEachMaterial() throws Exception {
        SvnMaterial material1 = repo.createMaterial("multiple-materials/trunk/part1", "part1");
        SvnMaterial material2 = repo.createMaterial("multiple-materials/trunk/part2", "part2");

        Materials materials = new Materials(material1, material2);

        repo.checkInOneFile("filename.txt", material1);
        repo.checkInOneFile("filename2.txt", material2);

        MaterialRevisions materialRevisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        assertThat(materialRevisions.getRevisions().size()).isEqualTo(2);
        assertThat(materialRevisions.getRevisions()).satisfiesExactlyInAnyOrder(
            MaterialRevisionsMatchers.containsModifiedFile("/trunk/part1/filename.txt"),
            MaterialRevisionsMatchers.containsModifiedFile("/trunk/part2/filename2.txt")
        );
    }

    private Revision latestRevision(SvnMaterial material, File workingDir, TestSubprocessExecutionContext execCtx) {
        List<Modification> modifications = material.latestModification(workingDir, execCtx);
        return new Modifications(modifications).latestRevision(material);
    }

    private void updateMaterials(Materials materials, Revision revision) {
        ProcessOutputStreamConsumer<InMemoryConsumer, InMemoryConsumer> outputStreamConsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        materials.cleanUp(pipelineDir, outputStreamConsumer);
        for (Material material : materials) {
            material.updateTo(outputStreamConsumer, pipelineDir, new RevisionContext(revision), execCtx);
        }
    }
}
