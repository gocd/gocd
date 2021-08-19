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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.util.TempDirUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MixedMultipleMaterialsTest {
    private SvnTestRepo svnRepo;
    private HgTestRepo hgRepo;
    private GitTestRepo gitRepo;
    private File pipelineDir;

    @BeforeEach
    public void createRepo(@TempDir Path tempDir) throws IOException {
        svnRepo = new SvnTestRepo(tempDir);
        hgRepo = new HgTestRepo(tempDir);
        gitRepo = new GitTestRepo(tempDir);
        pipelineDir = TempDirUtils.createTempDirectoryIn(tempDir, "pipeline").toFile();
    }

    @Test
    public void shouldGetLatestModifications() {
        HgMaterial hgMaterial = hgRepo.material();
        SvnMaterial svnMaterial = svnRepo.createMaterial("multiple-materials/trunk/part1", "part1");

        Materials materials = new Materials(hgMaterial, svnMaterial);
        MaterialRevisions revisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        assertThat(revisions.getMaterialRevision(0).numberOfModifications(), is(1));
        assertThat(revisions.getMaterialRevision(0).getRevision(),
                is(new Modifications(hgRepo.latestModifications()).latestRevision(hgMaterial)));
        assertThat(revisions.getMaterialRevision(1).numberOfModifications(), is(1));
        assertThat(revisions.getMaterialRevision(1).getRevision(), is(latestRevision(svnMaterial, pipelineDir, new TestSubprocessExecutionContext())));
        assertThat(revisions.toString(), revisions.totalNumberOfModifications(), is(2));
    }

    @Test
    public void shouldGetLatestModificationsWithThreeRepositories() {
        HgMaterial hgMaterial = hgRepo.material();
        SvnMaterial svnMaterial = svnRepo.createMaterial("multiple-materials/trunk/part1", "part1");
        GitMaterial gitMaterial = gitRepo.createMaterial();

        Materials materials = new Materials(hgMaterial, svnMaterial, gitMaterial);
        MaterialRevisions revisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        assertThat(revisions.getMaterialRevision(0).numberOfModifications(), is(1));
        assertThat(revisions.getMaterialRevision(0).getRevision(),
                is(new Modifications(hgRepo.latestModifications()).latestRevision(hgMaterial)));

        assertThat(revisions.getMaterialRevision(1).numberOfModifications(), is(1));
        assertThat(revisions.getMaterialRevision(1).getRevision(), is(latestRevision(svnMaterial, pipelineDir, new TestSubprocessExecutionContext())));

        assertThat(revisions.getMaterialRevision(2).numberOfModifications(), is(1));
        assertThat(revisions.getMaterialRevision(2).getRevision(),
                is(new Modifications(gitRepo.latestModifications()).latestRevision(gitMaterial)));

        assertThat(revisions.toString(), revisions.totalNumberOfModifications(), is(3));
    }

    private Revision latestRevision(SvnMaterial material, File workingDir, TestSubprocessExecutionContext execCtx) {
        List<Modification> modifications = material.latestModification(workingDir, execCtx);
        return new Modifications(modifications).latestRevision(material);
    }

}
