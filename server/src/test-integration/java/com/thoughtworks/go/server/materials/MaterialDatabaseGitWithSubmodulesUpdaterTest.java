/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.helper.GitRepoContainingSubmodule;
import com.thoughtworks.go.helper.TestRepo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class MaterialDatabaseGitWithSubmodulesUpdaterTest extends TestBaseForDatabaseUpdater {

    @Override
    protected Material material() {
        return new GitMaterial(testRepo.projectRepositoryUrl());
    }

    @Override
    protected TestRepo repo() throws Exception {
        GitRepoContainingSubmodule testRepoWithExternal = new GitRepoContainingSubmodule(temporaryFolder);
        testRepoWithExternal.addSubmodule("submodule-1", "sub1");
        return testRepoWithExternal;
    }

    @Test
    public void shouldUpdateModificationsForExternalsAsWell() throws Exception {
        updater.updateMaterial(material);
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        assertThat(materialRevisions.numberOfRevisions(), is(1));
    }

}
