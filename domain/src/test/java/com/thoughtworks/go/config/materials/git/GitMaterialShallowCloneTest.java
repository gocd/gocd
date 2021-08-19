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
package com.thoughtworks.go.config.materials.git;


import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempDirUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.*;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitMaterialShallowCloneTest {

    private GitTestRepo repo;
    private File workingDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException {
        repo = new GitTestRepo(tempDir);
        workingDir = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
    }

    @Test
    void defaultShallowFlagIsOff() {
        assertThat(new GitMaterial(repo.projectRepositoryUrl()).isShallowClone()).isFalse();
        assertThat(new GitMaterial(repo.projectRepositoryUrl(), null).isShallowClone()).isFalse();
        assertThat(new GitMaterial(repo.projectRepositoryUrl(), true).isShallowClone()).isTrue();
        assertThat(new GitMaterial(git(repo.projectRepositoryUrl())).isShallowClone()).isFalse();
        assertThat(new GitMaterial(git(repo.projectRepositoryUrl(), GitMaterialConfig.DEFAULT_BRANCH, true)).isShallowClone()).isTrue();
        assertThat(new GitMaterial(git(repo.projectRepositoryUrl(), GitMaterialConfig.DEFAULT_BRANCH, false)).isShallowClone()).isFalse();
    }

    @Test
    void shouldGetLatestModificationWithShallowClone() throws IOException {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        List<Modification> mods = material.latestModification(workingDir, context());
        assertThat(mods.size()).isEqualTo(1);
        assertThat(mods.get(0).getComment()).isEqualTo("Added 'run-till-file-exists' ant target");
        assertThat(localRepoFor(material).isShallow()).isTrue();
        assertThat(localRepoFor(material).containsRevisionInBranch(REVISION_0)).isFalse();
        assertThat(localRepoFor(material).currentRevision()).isEqualTo(REVISION_4.getRevision());
    }

    @Test
    void shouldGetModificationSinceANotInitiallyClonedRevision() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);

        List<Modification> modifications = material.modificationsSince(workingDir, REVISION_0, context());
        assertThat(modifications.size()).isEqualTo(4);
        assertThat(modifications.get(0).getRevision()).isEqualTo(REVISION_4.getRevision());
        assertThat(modifications.get(0).getComment()).isEqualTo("Added 'run-till-file-exists' ant target");
        assertThat(modifications.get(1).getRevision()).isEqualTo(REVISION_3.getRevision());
        assertThat(modifications.get(1).getComment()).isEqualTo("adding build.xml");
        assertThat(modifications.get(2).getRevision()).isEqualTo(REVISION_2.getRevision());
        assertThat(modifications.get(2).getComment()).isEqualTo("Created second.txt from first.txt");
        assertThat(modifications.get(3).getRevision()).isEqualTo(REVISION_1.getRevision());
        assertThat(modifications.get(3).getComment()).isEqualTo("Added second line");
    }


    @Test
    void shouldBeAbleToUpdateToRevisionNotFetched() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);

        material.updateTo(inMemoryConsumer(), workingDir, new RevisionContext(REVISION_3, REVISION_2, 2), context());

        assertThat(localRepoFor(material).currentRevision()).isEqualTo(REVISION_3.getRevision());
        assertThat(localRepoFor(material).containsRevisionInBranch(REVISION_2)).isTrue();
        assertThat(localRepoFor(material).containsRevisionInBranch(REVISION_3)).isTrue();
    }

    @Test
    void configShouldIncludesShallowFlag() {
        GitMaterialConfig shallowConfig = (GitMaterialConfig) new GitMaterial(repo.projectRepositoryUrl(), true).config();
        assertThat(shallowConfig.isShallowClone()).isTrue();
        GitMaterialConfig normalConfig = (GitMaterialConfig) new GitMaterial(repo.projectRepositoryUrl(), null).config();
        assertThat(normalConfig.isShallowClone()).isFalse();
    }

    @Test
    void xmlAttributesShouldIncludesShallowFlag() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        assertThat(material.getAttributesForXml().get("shallowClone")).isEqualTo(true);
    }

    @Test
    void attributesShouldIncludeShallowFlag() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        Map gitConfig = (Map) (material.getAttributes(false).get("git-configuration"));
        assertThat(gitConfig.get("shallow-clone")).isEqualTo(true);
    }

    @Test
    void shouldConvertExistingRepoToFullRepoWhenShallowCloneIsOff() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        material.latestModification(workingDir, context());
        assertThat(localRepoFor(material).isShallow()).isTrue();
        material = new GitMaterial(repo.projectRepositoryUrl(), false);
        material.latestModification(workingDir, context());
        assertThat(localRepoFor(material).isShallow()).isFalse();
    }

    @Test
    void withShallowCloneShouldGenerateANewMaterialWithOverriddenShallowConfig() {
        GitMaterial original = new GitMaterial(repo.projectRepositoryUrl(), false);
        assertThat(original.withShallowClone(true).isShallowClone()).isTrue();
        assertThat(original.withShallowClone(false).isShallowClone()).isFalse();
        assertThat(original.isShallowClone()).isFalse();
    }

    @Test
    void updateToANewRevisionShouldNotResultInUnshallowing() throws IOException {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        material.updateTo(inMemoryConsumer(), workingDir, new RevisionContext(REVISION_4, REVISION_4, 1), context());
        assertThat(localRepoFor(material).isShallow()).isTrue();
        List<Modification> modifications = repo.addFileAndPush("newfile", "add new file");
        StringRevision newRevision = new StringRevision(modifications.get(0).getRevision());
        material.updateTo(inMemoryConsumer(), workingDir, new RevisionContext(newRevision, newRevision, 1), context());
        assertThat(new File(workingDir, "newfile").exists()).isTrue();
        assertThat(localRepoFor(material).isShallow()).isTrue();
    }

    @Test
    void shouldUnshallowServerSideRepoCompletelyOnRetrievingModificationsSincePreviousRevision() {
        SystemEnvironment mockSystemEnvironment = mock(SystemEnvironment.class);
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        when(mockSystemEnvironment.get(SystemEnvironment.GO_SERVER_SHALLOW_CLONE)).thenReturn(false);

        material.modificationsSince(workingDir, REVISION_4, new TestSubprocessExecutionContext(mockSystemEnvironment, true));

        assertThat(localRepoFor(material).isShallow()).isFalse();
    }

    @Test
    void shouldNotUnshallowOnServerSideIfShallowClonePropertyIsOnAndRepoIsAlreadyShallow() {
        SystemEnvironment mockSystemEnvironment = mock(SystemEnvironment.class);
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        when(mockSystemEnvironment.get(SystemEnvironment.GO_SERVER_SHALLOW_CLONE)).thenReturn(true);

        material.modificationsSince(workingDir, REVISION_4, new TestSubprocessExecutionContext(mockSystemEnvironment, false));

        assertThat(localRepoFor(material).isShallow()).isTrue();
    }

    private TestSubprocessExecutionContext context() {
        return new TestSubprocessExecutionContext();
    }

    private GitCommand localRepoFor(GitMaterial material) {
        return new GitCommand(material.getFingerprint(), workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);
    }
}
