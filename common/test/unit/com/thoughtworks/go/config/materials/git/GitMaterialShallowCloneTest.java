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
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.TestFileUtil;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.*;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
        assertThat(localRepoFor(material).hasRevision(REVISION_0), is(false));
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

        material.updateTo(inMemoryConsumer(), REVISION_2, workingDir, context());
        assertThat(localRepoFor(material).currentRevision(), is(REVISION_2.getRevision()));
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
    public void withShallowCloneShouldGenerateANewMaterial() {
        GitMaterial original = new GitMaterial(repo.projectRepositoryUrl(), false);
        GitMaterial shallow = original.withShallowClone();
        assertThat(original.isShallowClone(), is(false));
        assertThat(shallow.isShallowClone(), is(true));
    }


    private TestSubprocessExecutionContext context() {
        return new TestSubprocessExecutionContext();
    }

    private GitCommand localRepoFor(GitMaterial material) {
        return new GitCommand(material.getFingerprint(), workingDir, GitMaterialConfig.DEFAULT_BRANCH, false);
    }


}
