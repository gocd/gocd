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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.helper.SvnTestRepoWithExternal;
import com.thoughtworks.go.helper.TestRepo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MaterialDatabaseSvnWithExternalsUpdaterTest extends TestBaseForDatabaseUpdater {
    private SvnTestRepoWithExternal testRepoWithExternal;

    @Override
    protected Material material() {
        return new SvnMaterial(testRepo.projectRepositoryUrl(), null, null, true);
    }

    @Override
    protected TestRepo repo() throws IOException {
        testRepoWithExternal = new SvnTestRepoWithExternal(temporaryFolder);
        return testRepoWithExternal;
    }

    @Test
    public void shouldUpdateModificationsForExternalsAsWell() throws Exception {
        updater.updateMaterial(material);
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        assertThat(materialRevisions.numberOfRevisions(), is(2));
    }

    @Test
    public void shouldUpdateModificationsForNewlyAddedExternalToExistingMaterial() throws Exception {
        updater.updateMaterial(material);
        testRepoWithExternal.checkInOneFile("another_dir/some_file");
        testRepoWithExternal.setupExternals("another_repo", new File(testRepoWithExternal.workingFolder(), "another_dir"));
        updater.updateMaterial(material);
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        assertThat(materialRevisions.numberOfRevisions(), is(3));
    }

    @Test
    public void shouldNotTryToSaveModificationForAnExternalThathasAlreadyBeenSaved() throws Exception {
        updater.updateMaterial(material);

        SvnTestRepoWithExternal otherRepo = new SvnTestRepoWithExternal(testRepoWithExternal.externalRepositoryUrl(), temporaryFolder);
        SvnMaterial otherMaterial = new SvnMaterial(otherRepo.projectRepositoryUrl(), null, null, true);
        updater.updateMaterial(otherMaterial);

        MaterialRevisions materialRevisions = materialRepository.findLatestModification(otherMaterial);
        assertThat(materialRevisions.numberOfRevisions(), is(2));
    }

    @Test
    public void shouldUpdateModificationsForExternals() throws Exception {
        updater.updateMaterial(material);
        testRepoWithExternal.checkInExternalFile("foo_bar", "foo bar quux");
        updater.updateMaterial(material);
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        assertThat(materialRevisions.numberOfRevisions(), is(2));
        SvnMaterial externalMaterial = testRepoWithExternal.externalMaterial();

        MaterialRevision revisionForExternal = materialRevisions.findRevisionFor(externalMaterial);
        assertThat(revisionForExternal.getModification(0).getComment(), is("foo bar quux"));
    }

    @Test
    public void shouldDetectChangesToExternals() throws Exception {
        ((SvnTestRepoWithExternal)testRepo).checkInExternalFile("external.txt", "EXTERNAL");
        updater.updateMaterial(material);
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        assertThat(materialRevisions.numberOfRevisions(), is(2));

        assertThat(materialRevisions.getMaterialRevision(1).getModification(0).getComment(), is("EXTERNAL"));
    }


}
