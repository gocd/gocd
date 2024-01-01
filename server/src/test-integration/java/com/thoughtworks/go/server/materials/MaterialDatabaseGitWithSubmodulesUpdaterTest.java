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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.helper.GitRepoContainingSubmodule;
import com.thoughtworks.go.helper.TestRepo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(SpringExtension.class)
@ExtendWith(SystemStubsExtension.class)
public class MaterialDatabaseGitWithSubmodulesUpdaterTest extends TestBaseForDatabaseUpdater {

    @SystemStub
    static SystemProperties systemProperties;

    @BeforeAll
    public static void setUpAll() {
        systemProperties.set(GitCommand.GIT_SUBMODULE_ALLOW_FILE_PROTOCOL, "Y");
    }

    @Override
    protected Material material() {
        return new GitMaterial(testRepo.projectRepositoryUrl());
    }

    @Override
    protected TestRepo repo(Path tempDir) throws Exception {
        GitRepoContainingSubmodule testRepoWithExternal = new GitRepoContainingSubmodule(tempDir);
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
