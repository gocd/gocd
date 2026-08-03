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
package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.util.TempDirUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.REVISION_4;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fixture repository contains exactly three files at its root: {@code build.xml},
 * {@code first.txt} and {@code second.txt}.
 */
class GitMaterialSparseCheckoutTest {

    private GitTestRepo repo;
    private File workingDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
        repo = new GitTestRepo(tempDir);
        workingDir = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
    }

    @Test
    void shouldCheckOutTheWholeRepositoryByDefault() {
        updateTo(materialWithSparseCheckout(null));

        assertThat(new File(workingDir, "first.txt")).exists();
        assertThat(new File(workingDir, "second.txt")).exists();
        assertThat(new File(workingDir, "build.xml")).exists();
        assertThat(localRepo().isSparse()).isFalse();
    }

    @Test
    void shouldCheckOutOnlyPathsMatchingTheConfiguredPatterns() {
        updateTo(materialWithSparseCheckout("first.txt"));

        assertThat(new File(workingDir, "first.txt")).exists();
        assertThat(new File(workingDir, "second.txt")).doesNotExist();
        assertThat(new File(workingDir, "build.xml")).doesNotExist();
        assertThat(localRepo().isSparse()).isTrue();
    }

    @Test
    void shouldAcceptOnePatternPerLineIgnoringBlanksAndSurroundingWhitespace() {
        updateTo(materialWithSparseCheckout("\n  first.txt  \n\n\tbuild.xml\n"));

        assertThat(new File(workingDir, "first.txt")).exists();
        assertThat(new File(workingDir, "build.xml")).exists();
        assertThat(new File(workingDir, "second.txt")).doesNotExist();
    }

    @Test
    void shouldNarrowAnExistingWorkingCopyWhenPatternsChange() {
        updateTo(materialWithSparseCheckout("first.txt\nbuild.xml"));
        assertThat(new File(workingDir, "build.xml")).exists();

        updateTo(materialWithSparseCheckout("first.txt"));

        assertThat(new File(workingDir, "first.txt")).exists();
        assertThat(new File(workingDir, "build.xml")).doesNotExist();
    }

    @Test
    void shouldRestoreAFullWorkingCopyOnceSparseCheckoutIsRemoved() {
        updateTo(materialWithSparseCheckout("first.txt"));
        assertThat(localRepo().isSparse()).isTrue();
        assertThat(new File(workingDir, "build.xml")).doesNotExist();

        updateTo(materialWithSparseCheckout(null));

        assertThat(new File(workingDir, "build.xml")).exists();
        assertThat(new File(workingDir, "second.txt")).exists();
        assertThat(localRepo().isSparse()).isFalse();
    }

    @Test
    void shouldLeaveTheWorkingCopyClean() {
        updateTo(materialWithSparseCheckout("first.txt"));

        assertThat(localRepo().getConfigValue("core.sparseCheckout")).isEqualTo("true");
        assertThat(new GitCommand(null, workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, null).currentRevision())
                .isEqualTo(REVISION_4.getRevision());
    }

    @Test
    void configShouldRoundTripSparseCheckout() {
        GitMaterial material = materialWithSparseCheckout("first.txt");

        assertThat(material.getSparseCheckout()).isEqualTo("first.txt");
        assertThat(((GitMaterialConfig) material.config()).getSparseCheckout()).isEqualTo("first.txt");
        assertThat(materialWithSparseCheckout(null).getSparseCheckout()).isNull();
    }

    @Test
    void blankSparseCheckoutShouldBeTreatedAsAbsent() {
        GitMaterialConfig config = new GitMaterialConfig();
        config.setSparseCheckout("   \n  ");

        assertThat(config.getSparseCheckout()).isNull();
        assertThat(config.sparseCheckoutPatterns()).isEmpty();
    }

    @Test
    void attributesShouldIncludeSparseCheckout() {
        GitMaterial material = materialWithSparseCheckout("first.txt");

        @SuppressWarnings("unchecked") Map<String, ?> gitConfig =
                (Map<String, ?>) material.getAttributes(false).get("git-configuration");
        assertThat(gitConfig.get("sparse-checkout")).isEqualTo("first.txt");
    }

    private GitMaterial materialWithSparseCheckout(String patterns) {
        GitMaterialConfig config = new GitMaterialConfig();
        config.setUrl(repo.projectRepositoryUrl());
        config.setSparseCheckout(patterns);
        return new GitMaterial(config);
    }

    private void updateTo(GitMaterial material) {
        material.updateTo(inMemoryConsumer(), workingDir, new RevisionContext(REVISION_4), new TestSubprocessExecutionContext());
    }

    private GitCommand localRepo() {
        return new GitCommand(null, workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);
    }
}
