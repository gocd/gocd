/*
 * Copyright 2016 ThoughtWorks, Inc.
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


import com.googlecode.junit.ext.JunitExtRunner;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.*;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JunitExtRunner.class)

public class GitMaterialShallowCloneTest {
    private GitTestRepo repo;
    private File workingDir;

    @Before
    public void setup() throws Exception {
        repo = new GitTestRepo();
        workingDir = TestFileUtil.createUniqueTempFolder("working");
    }


    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
    }

    @Test
    public void defaultShallowFlagIsOff() throws Exception {
        assertThat(new GitMaterial(repo.projectRepositoryUrl()).isShallowClone(), is(false));
        assertThat(new GitMaterial(repo.projectRepositoryUrl(), null).isShallowClone(), is(false));
        assertThat(new GitMaterial(repo.projectRepositoryUrl(), true).isShallowClone(), is(true));
        assertThat(new GitMaterial(new GitMaterialConfig(repo.projectRepositoryUrl())).isShallowClone(), is(false));
        assertThat(new GitMaterial(new GitMaterialConfig(repo.projectRepositoryUrl(), GitMaterialConfig.DEFAULT_BRANCH, true)).isShallowClone(), is(true));
        assertThat(new GitMaterial(new GitMaterialConfig(repo.projectRepositoryUrl(), GitMaterialConfig.DEFAULT_BRANCH, false)).isShallowClone(), is(false));
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldGetLatestModificationWithShallowClone() throws IOException {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        List<Modification> mods = material.latestModification(workingDir, context());
        assertThat(mods.size(), is(1));
        assertThat(mods.get(0).getComment(), Matchers.is("Added 'run-till-file-exists' ant target"));
        assertThat(localRepoFor(material).isShallow(), is(true));
        assertThat(localRepoFor(material).containsRevisionInBranch(REVISION_0), is(false));
        assertThat(localRepoFor(material).currentRevision(), is(REVISION_4.getRevision()));
    }

    @Test
    public void shouldGetModificationSinceANotInitiallyClonedRevision() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);

        List<Modification> modifications = material.modificationsSince(workingDir, REVISION_0, context());
        assertThat(modifications.size(), is(4));
        assertThat(modifications.get(0).getRevision(), is(REVISION_4.getRevision()));
        assertThat(modifications.get(0).getComment(), is("Added 'run-till-file-exists' ant target"));
        assertThat(modifications.get(1).getRevision(), is(REVISION_3.getRevision()));
        assertThat(modifications.get(1).getComment(), is("adding build.xml"));
        assertThat(modifications.get(2).getRevision(), is(REVISION_2.getRevision()));
        assertThat(modifications.get(2).getComment(), is("Created second.txt from first.txt"));
        assertThat(modifications.get(3).getRevision(), is(REVISION_1.getRevision()));
        assertThat(modifications.get(3).getComment(), is("Added second line"));
    }


    @Test
    public void shouldBeAbleToUpdateToRevisionNotFetched() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);

        material.updateTo(inMemoryConsumer(), workingDir, new RevisionContext(REVISION_3, REVISION_2, 2), context());

        assertThat(localRepoFor(material).currentRevision(), is(REVISION_3.getRevision()));
        assertThat(localRepoFor(material).containsRevisionInBranch(REVISION_2), is(true));
        assertThat(localRepoFor(material).containsRevisionInBranch(REVISION_3), is(true));
    }

    @Test
    public void configShouldIncludesShallowFlag() {
        GitMaterialConfig shallowConfig = (GitMaterialConfig) new GitMaterial(repo.projectRepositoryUrl(), true).config();
        assertThat(shallowConfig.isShallowClone(), is(true));
        GitMaterialConfig normalConfig = (GitMaterialConfig) new GitMaterial(repo.projectRepositoryUrl(), null).config();
        assertThat(normalConfig.isShallowClone(), is(false));
    }

    @Test
    public void xmlAttributesShouldIncludesShallowFlag() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        assertThat(material.getAttributesForXml().get("shallowClone"), Is.<Object>is(true));
    }
    @Test
    public void attributesShouldIncludeShallowFlag() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        Map gitConfig = (Map) (material.getAttributes(false).get("git-configuration"));
        assertThat(gitConfig.get("shallow-clone"), Is.<Object>is(true));
    }

    @Test
    public void shouldConvertExistingRepoToFullRepoWhenShallowCloneIsOff() {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        material.latestModification(workingDir, context());
        assertThat(localRepoFor(material).isShallow(), is(true));
        material = new GitMaterial(repo.projectRepositoryUrl(), false);
        material.latestModification(workingDir, context());
        assertThat(localRepoFor(material).isShallow(), is(false));
    }

    @Test
    public void withShallowCloneShouldGenerateANewMaterialWithOverriddenShallowConfig() {
        GitMaterial original = new GitMaterial(repo.projectRepositoryUrl(), false);
        assertThat(original.withShallowClone(true).isShallowClone(), is(true));
        assertThat(original.withShallowClone(false).isShallowClone(), is(false));
        assertThat(original.isShallowClone(), is(false));
    }

    @Test
    public void updateToANewRevisionShouldNotResultInUnshallowing() throws IOException {
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        material.updateTo(inMemoryConsumer(), workingDir, new RevisionContext(REVISION_4, REVISION_4, 1), context());
        assertThat(localRepoFor(material).isShallow(), is(true));
        List<Modification> modifications = repo.addFileAndPush("newfile", "add new file");
        StringRevision newRevision = new StringRevision(modifications.get(0).getRevision());
        material.updateTo(inMemoryConsumer(), workingDir, new RevisionContext(newRevision, newRevision, 1), context());
        assertThat(new File(workingDir, "newfile").exists(), is(true));
        assertThat(localRepoFor(material).isShallow(), is(true));
    }

    @Test
    public void shouldUnshallowServerSideRepoCompletelyOnRetrievingModificationsSincePreviousRevision() {
        SystemEnvironment mockSystemEnvironment = mock(SystemEnvironment.class);
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        when(mockSystemEnvironment.get(SystemEnvironment.GO_SERVER_SHALLOW_CLONE)).thenReturn(false);

        material.modificationsSince(workingDir, REVISION_4, new TestSubprocessExecutionContext(mockSystemEnvironment, true));

        assertThat(localRepoFor(material).isShallow(), is(false));
    }

    @Test
    public void shouldNotUnshallowOnServerSideIfShallowClonePropertyIsOnAndRepoIsAlreadyShallow() {
        SystemEnvironment mockSystemEnvironment = mock(SystemEnvironment.class);
        GitMaterial material = new GitMaterial(repo.projectRepositoryUrl(), true);
        when(mockSystemEnvironment.get(SystemEnvironment.GO_SERVER_SHALLOW_CLONE)).thenReturn(true);

        material.modificationsSince(workingDir, REVISION_4, new TestSubprocessExecutionContext(mockSystemEnvironment, false));

        assertThat(localRepoFor(material).isShallow(), is(true));
    }

    private TestSubprocessExecutionContext context() {
        return new TestSubprocessExecutionContext();
    }

    private GitCommand localRepoFor(GitMaterial material) {
        return new GitCommand(material.getFingerprint(), workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, new HashMap<String, String>());
    }
}
