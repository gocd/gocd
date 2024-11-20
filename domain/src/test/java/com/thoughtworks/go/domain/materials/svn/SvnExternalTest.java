/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.helper.SvnTestRepoWithExternal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.thoughtworks.go.helper.MaterialsMother.svnMaterial;
import static org.assertj.core.api.Assertions.assertThat;

public class SvnExternalTest {

    private static SvnTestRepoWithExternal svnRepo;

    @BeforeAll
    public static void copyRepository(@TempDir Path tempDir) throws IOException {
        svnRepo = new SvnTestRepoWithExternal(tempDir);
    }

    @Test
    public void shouldGetAllExternalURLSByPropGetOnMainURL() {
        String url = svnRepo.projectRepositoryUrl();
        SvnCommand svn = new SvnCommand(null, url, "user", "pass", false);
        List<SvnExternal> urls = svn.getAllExternalURLs();
        assertThat(urls.size()).isEqualTo(1);
    }

    @Test
    public void shouldGetLatestRevisionFromExpandedSvnExternalRepository() {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        Material svnExt = svnMaterial(svnRepo.externalRepositoryUrl(), "end2end");
        List<Modification> modifications = ((SvnMaterial) svnExt).latestModification(svnRepo.workingFolder(), new TestSubprocessExecutionContext());
        materialRevisions.addRevision(svnExt, modifications);

        assertThat(materialRevisions.numberOfRevisions()).isEqualTo(1);
        MaterialRevision materialRevision = materialRevisions.getRevisions().get(0);
        assertThat(materialRevision.getMaterial()).isEqualTo(svnExt);
        assertThat(materialRevision.getRevision().getRevision()).isEqualTo("4");
    }

    @Test
    public void shouldGetLatestRevision() {
        SvnMaterial svn = svnMaterial(svnRepo.projectRepositoryUrl(), null);
        SvnMaterial svnExt = svnMaterial(svnRepo.externalRepositoryUrl(), "end2end");
        final Materials materials = new Materials(svn, svnExt);

        final MaterialRevisions materialRevisions = materials.latestModification(svnRepo.workingFolder(), new TestSubprocessExecutionContext());
        assertThat(materialRevisions.numberOfRevisions()).isEqualTo(2);

        MaterialRevision main = materialRevisions.getRevisions().get(0);
        assertThat(main.getMaterial()).isEqualTo(svn);
        assertThat(main.getModifications().size()).isEqualTo(1);
        assertThat(main.getRevision().getRevision()).isEqualTo("5");

        MaterialRevision external = materialRevisions.getRevisions().get(1);
        assertThat(external.getMaterial()).isEqualTo(svnExt);
        assertThat(external.getRevision().getRevision()).isEqualTo("4");
        assertThat(external.getModifications().size()).isEqualTo(1);
    }
}
