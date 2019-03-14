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

package com.thoughtworks.go.config.materials.svn;

import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.svn.SubversionRevision;
import com.thoughtworks.go.domain.materials.svn.SvnMaterialUpdater;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;


class SvnMaterialUpdaterTest extends BuildSessionBasedTestCase {
    private SvnTestRepo svnTestRepo;
    private SvnMaterial svnMaterial;
    private SubversionRevision revision = new SubversionRevision("1");
    private File workingDir;

    @BeforeEach
    void setUp() throws Exception {
        this.svnTestRepo = new SvnTestRepo(temporaryFolder);
        this.workingDir = temporaryFolder.newFolder("workingFolder");
        svnMaterial = MaterialsMother.svnMaterial(svnTestRepo.projectRepositoryUrl());
    }

    @AfterEach
    void tearDown() {
        FileUtils.deleteQuietly(workingDir);
        TestRepo.internalTearDown();
    }

    @Test
    void shouldNotUpdateIfCheckingOutAFreshCopy() {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(console.output()).contains("Checked out revision");
        assertThat(console.output()).doesNotContain("Updating");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldUpdateIfNotCheckingOutFreshCopy() {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        console.clear();
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(console.output()).doesNotContain("Checked out revision");
        assertThat(console.output()).contains("Updating");
    }

    @Test
    void shouldUpdateToDestinationFolder() {
        svnMaterial.setFolder("dest");
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(new File(workingDir, "dest").exists()).isTrue();
        assertThat(new File(workingDir, "dest/.svn").exists()).isTrue();
    }

    @Test
    void shouldDoAFreshCheckoutIfDestIsNotARepo() {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        console.clear();
        FileUtils.deleteQuietly(new File(workingDir, "svnDir/.svn"));
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(console.output()).contains("Checked out revision");
        assertThat(console.output()).doesNotContain("Updating");
    }

    @Test
    void shouldDoFreshCheckoutIfUrlChanges() throws Exception {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        console.clear();
        File shouldBeRemoved = new File(workingDir, "svnDir/shouldBeRemoved");
        shouldBeRemoved.createNewFile();
        assertThat(shouldBeRemoved.exists()).isTrue();

        String repositoryUrl = new SvnTestRepo(temporaryFolder).projectRepositoryUrl();
        assertThat(repositoryUrl).isNotEqualTo(svnTestRepo.projectRepositoryUrl());
        SvnMaterial material = MaterialsMother.svnMaterial(repositoryUrl);
        updateTo(material, new RevisionContext(revision), JobResult.Passed);
        assertThat(material.getUrl()).isEqualTo(repositoryUrl);
        assertThat(console.output()).contains("Checked out revision");
        assertThat(console.output()).doesNotContain("Updating");
        assertThat(shouldBeRemoved.exists()).isFalse();
    }

    @Test
    void shouldNotLeakPasswordInUrlIfCheckoutFails() {
        SvnMaterial material = MaterialsMother.svnMaterial("https://foo:foopassword@thisdoesnotexist.io/repo");
        updateTo(material, new RevisionContext(revision), JobResult.Failed);
        assertThat(console.output()).contains("https://foo:******@thisdoesnotexist.io/repo");
        assertThat(console.output()).doesNotContain("foopassword");
    }

    private void updateTo(SvnMaterial material, RevisionContext revisionContext, JobResult expectedResult) {
        BuildSession buildSession = newBuildSession();
        JobResult result = buildSession.build(new SvnMaterialUpdater(material).updateTo(workingDir.toString(), revisionContext));
        assertThat(result).as(buildInfo()).isEqualTo(expectedResult);
    }
}
