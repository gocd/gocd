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
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
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

    private GitMaterial material;
    private GitTestRepo repo;
    private File workingDir;


    @Before
    public void setup() throws Exception {
        repo = new GitTestRepo();
        material = new GitMaterial(repo.projectRepositoryUrl(), true);
        workingDir = TestFileUtil.createUniqueTempFolder("working");
    }


    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldGetLatestModificationWithShallowClone() throws IOException {
        List<Modification> mods = material.latestModification(workingDir, context());
        assertThat(mods.size(), is(1));
        assertThat(mods.get(0).getComment(), Matchers.is("Added 'run-till-file-exists' ant target"));
        assertThat(workingRepo().isShallow(), is(true));
        assertThat(workingRepo().hasRevision(REVISION_0), is(false));
        assertThat(workingRepo().currentRevision(), is(REVISION_4.getRevision()));
    }

    @Test
    public void shouldGetModificationSinceANotInitialyClonedRevision() {
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
        material.updateTo(inMemoryConsumer(), REVISION_2, workingDir, context());
        assertThat(workingRepo().currentRevision(), is(REVISION_2.getRevision()));
    }

    @Test
    public void configShouldIncludesShallowFlag() {
        assertThat(((GitMaterialConfig) material.config()).isShallowClone(), is(true));
    }

    @Test
    public void attributesShouldIncludeShallowFlag() {
        Map gitConfig = (Map) (material.getAttributes(false).get("git-configuration"));
        assertThat(gitConfig.get("shallow-clone"), Is.<Object>is(true));
    }



    private TestSubprocessExecutionContext context() {
        return new TestSubprocessExecutionContext();
    }

    private GitCommand workingRepo() {
        return new GitCommand(material.getFingerprint(), workingDir, GitMaterialConfig.DEFAULT_BRANCH, false);
    }


}
