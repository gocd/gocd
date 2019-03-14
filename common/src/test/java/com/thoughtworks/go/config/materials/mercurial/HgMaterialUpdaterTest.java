/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialUpdater;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.domain.materials.svn.MaterialUrl;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.TestRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static com.thoughtworks.go.helper.HgTestRepo.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

class HgMaterialUpdaterTest extends BuildSessionBasedTestCase {
    private HgMaterial hgMaterial;
    private HgTestRepo hgTestRepo;
    private File workingFolder;

    @BeforeEach
    void setUp() throws Exception {
        hgTestRepo = new HgTestRepo("hgTestRepo1", temporaryFolder);
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl());
        workingFolder = temporaryFolder.newFolder("workingFolder");
    }

    @AfterEach
    void teardown() {
        TestRepo.internalTearDown();
    }

    @Test
    void shouldUpdateToSpecificRevision() {
        updateTo(hgMaterial, new RevisionContext(REVISION_0), JobResult.Passed);
        File end2endFolder = new File(workingFolder, "end2end");
        assertThat(end2endFolder.listFiles().length).isEqualTo(3);
        updateTo(hgMaterial, new RevisionContext(REVISION_1), JobResult.Passed);
        assertThat(end2endFolder.listFiles().length).isEqualTo(4);
    }

    @Test
    void shouldUpdateToDestinationFolder() {
        hgMaterial.setFolder("dest");
        updateTo(hgMaterial, new RevisionContext(REVISION_0), JobResult.Passed);
        File end2endFolder = new File(workingFolder, "dest/end2end");
        assertThat(new File(workingFolder, "dest").exists()).isTrue();
        assertThat(end2endFolder.exists()).isTrue();
    }

    @Test
    void shouldLogRepoInfoToConsoleOutWithoutFolder() {
        updateTo(hgMaterial, new RevisionContext(new StringRevision("0")), JobResult.Passed);
        assertThat(console.output()).contains(format("Start updating %s at revision %s from %s", "files", "0",
                hgMaterial.getUrl()));
    }

    @Test
    void failureCommandShouldNotLeakPasswordOnUrl() {
        HgMaterial material = MaterialsMother.hgMaterial("https://foo:foopassword@this.is.absolute.not.exists");
        updateTo(material, new RevisionContext(REVISION_1), JobResult.Failed);
        assertThat(console.output()).contains("https://foo:******@this.is.absolute.not.exists");
        assertThat(console.output()).doesNotContain("foopassword");
    }

    @Test
    void shouldCreateBuildCommandUpdateToSpecificRevision() {
        File newFile = new File(workingFolder, "end2end/revision2.txt");
        updateTo(hgMaterial, new RevisionContext(REVISION_0), JobResult.Passed);
        assertThat(console.output()).contains("Start updating files at revision " + REVISION_0.getRevision());
        assertThat(newFile.exists()).isFalse();

        console.clear();
        updateTo(hgMaterial, new RevisionContext(REVISION_2, REVISION_1, 2), JobResult.Passed);

        assertThat(console.output()).contains("Start updating files at revision " + REVISION_2.getRevision());
        assertThat(newFile.exists()).isTrue();
    }

    @Test
    void shouldNotDeleteAndRecheckoutDirectoryUnlessUrlChanges() throws Exception {
        String repositoryUrl = new HgTestRepo(temporaryFolder).projectRepositoryUrl();
        HgMaterial material = MaterialsMother.hgMaterial(repositoryUrl);
        updateTo(material, new RevisionContext(REVISION_0), JobResult.Passed);
        File shouldNotBeRemoved = new File(workingFolder, "shouldBeRemoved");
        shouldNotBeRemoved.createNewFile();
        assertThat(shouldNotBeRemoved.exists()).isTrue();


        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        assert (MaterialUrl.sameUrl(material.getUrl(), repositoryUrl));
        assertThat(shouldNotBeRemoved.exists()).isTrue();
    }

    @Test
    void shouldDeleteAndRecheckoutDirectoryWhenUrlChanges() throws Exception {
        updateTo(hgMaterial, new RevisionContext(REVISION_0), JobResult.Passed);
        File shouldBeRemoved = new File(workingFolder, "shouldBeRemoved");
        shouldBeRemoved.createNewFile();
        assertThat(shouldBeRemoved.exists()).isTrue();

        String repositoryUrl = new HgTestRepo(temporaryFolder).projectRepositoryUrl();
        HgMaterial material = MaterialsMother.hgMaterial(repositoryUrl);
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        assertThat(material.getUrl()).isNotEqualTo(hgMaterial.getUrl());
        assert (MaterialUrl.sameUrl(material.getUrl(), repositoryUrl));
        assertThat(shouldBeRemoved.exists()).isFalse();
    }

    @Test
    void shouldPullNewChangesFromRemoteBeforeUpdating() throws Exception {
        File newWorkingFolder = temporaryFolder.newFolder("newWorkingFolder");
        updateTo(hgMaterial, new RevisionContext(REVISION_0), JobResult.Passed);
        String repositoryUrl = hgTestRepo.projectRepositoryUrl();
        HgMaterial material = MaterialsMother.hgMaterial(repositoryUrl);
        assertThat(material.getUrl()).isEqualTo(hgMaterial.getUrl());
        updateTo(material, new RevisionContext(REVISION_0), JobResult.Passed, newWorkingFolder);

        hgTestRepo.commitAndPushFile("SomeDocumentation.txt", "whatever");

        List<Modification> modification = hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());
        StringRevision revision = new StringRevision(modification.get(0).getRevision());

        updateTo(material, new RevisionContext(revision), JobResult.Passed, newWorkingFolder);
        assertThat(console.output()).contains("Start updating files at revision " + revision.getRevision());
    }

    private void updateTo(HgMaterial material, RevisionContext revisionContext, JobResult expectedResult, File workingFolder) {
        BuildSession buildSession = newBuildSession();
        JobResult result = buildSession.build(new HgMaterialUpdater(material).updateTo(workingFolder.toString(), revisionContext));
        assertThat(result).as(buildInfo()).isEqualTo(expectedResult);
    }

    private void updateTo(HgMaterial material, RevisionContext revisionContext, JobResult expectedResult) {
        BuildSession buildSession = newBuildSession();
        JobResult result = buildSession.build(new HgMaterialUpdater(material).updateTo(workingFolder.toString(), revisionContext));
        assertThat(result).as(buildInfo()).isEqualTo(expectedResult);
    }
}
