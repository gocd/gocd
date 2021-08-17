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

import java.io.File;
import java.io.IOException;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import org.apache.commons.io.FileUtils;
import org.aspectj.util.FileUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class MaterialDatabaseGitUpdaterTest extends TestBaseForDatabaseUpdater {
    @Override
    protected GitMaterial material() {
        return new GitMaterial(testRepo.projectRepositoryUrl());
    }

    @Override
    protected GitTestRepo repo() throws IOException {
        return new GitTestRepo(temporaryFolder);
    }

    @Test
    public void shouldRemoveFlyweightWhenConfiguredBranchDoesNotExist() throws Exception {
        File flyweightDir = new File("pipelines", "flyweight");
        FileUtils.deleteQuietly(flyweightDir);

        material = new GitMaterial(testRepo.projectRepositoryUrl(), "bad-bad-branch");

        try {
            updater.updateMaterial(material);
            fail("material update should have failed as given branch does not exist in repository");
        } catch (Exception e) {
            //ignore
        }

        MaterialInstance materialInstance = materialRepository.findMaterialInstance(material);

        assertThat(materialInstance, is(nullValue()));
        assertThat(FileUtil.listFiles(flyweightDir).length, is(0));//no flyweight dir left behind
    }

}
