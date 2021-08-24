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

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MaterialDatabaseGitUpdaterTest extends TestBaseForDatabaseUpdater {
    @Override
    protected GitMaterial material() {
        return new GitMaterial(testRepo.projectRepositoryUrl());
    }

    @Override
    protected GitTestRepo repo(Path tempDir) throws IOException {
        return new GitTestRepo(tempDir);
    }

    @Test
    public void shouldRemoveFlyweightWhenConfiguredBranchDoesNotExist() throws Exception {
        material = new GitMaterial(testRepo.projectRepositoryUrl(), "bad-bad-branch");

        // Ensure we start clean
        File flyweightDir = materialRepository.folderFor(new GitMaterial("dummy")).getParentFile();
        FileUtils.deleteQuietly(flyweightDir);

        assertThatThrownBy(() -> updater.updateMaterial(material))
                .hasMessageContaining("bad-bad-branch not found");

        assertThat(materialRepository.findMaterialInstance(material)).isNull();
        // Check there are no flyweight folders created by looking at the parent directory of a new folder generation
        assertThat(flyweightDir).isEmptyDirectory();
    }

}
