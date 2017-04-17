/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;


public class SvnMaterialUpdaterTest extends BuildSessionBasedTestCase {
    private SvnTestRepo svnTestRepo;
    private SvnMaterial svnMaterial;
    SubversionRevision revision = new SubversionRevision("1");
    private File workingDir;

    @Before
    public void setUp() throws Exception {
        this.svnTestRepo = new SvnTestRepo("testsvnrepo");
        this.workingDir = TestFileUtil.createTempFolder("workingFolder");
        svnMaterial = MaterialsMother.svnMaterial(svnTestRepo.projectRepositoryUrl());
    }

    @After
    public void tearDown() throws Exception {
        FileUtil.deleteFolder(workingDir);
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldNotUpdateIfCheckingOutAFreshCopy() throws IOException {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(console.output(), containsString("Checked out revision"));
        assertThat(console.output(), not(containsString("Updating")));
    }

    @Test
    public void shouldUpdateIfNotCheckingOutFreshCopy() throws IOException {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        console.clear();
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(console.output(), not(containsString("Checked out revision")));
        assertThat(console.output(), containsString("Updating"));
    }

    @Test
    public void shouldUpdateToDestinationFolder() throws Exception {
        svnMaterial.setFolder("dest");
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(new File(workingDir, "dest").exists(), is(true));
        assertThat(new File(workingDir, "dest/.svn").exists(), is(true));
    }

    @Test
    public void shouldDoAFreshCheckoutIfDestIsNotARepo() throws Exception {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        console.clear();
        FileUtil.deleteFolder(new File(workingDir, "svnDir/.svn"));
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(console.output(), containsString("Checked out revision"));
        assertThat(console.output(), not(containsString("Updating")));
    }

    @Test
    public void shouldDoFreshCheckoutIfUrlChanges() throws Exception {
        updateTo(svnMaterial, new RevisionContext(revision), JobResult.Passed);
        console.clear();
        File shouldBeRemoved = new File(workingDir, "svnDir/shouldBeRemoved");
        shouldBeRemoved.createNewFile();
        assertThat(shouldBeRemoved.exists(), is(true));

        String repositoryUrl = new SvnTestRepo().projectRepositoryUrl();
        assertNotEquals(svnTestRepo.projectRepositoryUrl(), repositoryUrl);
        SvnMaterial material = MaterialsMother.svnMaterial(repositoryUrl);
        updateTo(material, new RevisionContext(revision), JobResult.Passed);
        assertThat(material.getUrl(), is(repositoryUrl));
        assertThat(console.output(), containsString("Checked out revision"));
        assertThat(console.output(), not(containsString("Updating")));
        assertThat(shouldBeRemoved.exists(), is(false));
    }

    @Test
    public void shouldNotLeakPasswordInUrlIfCheckoutFails() throws Exception {
        SvnMaterial material = MaterialsMother.svnMaterial("https://foo:foopassword@thisdoesnotexist.io/repo");
        updateTo(material, new RevisionContext(revision), JobResult.Failed);
        assertThat(console.output(), containsString("https://foo:******@thisdoesnotexist.io/repo"));
        assertThat(console.output(), not(containsString("foopassword")));
    }

    private void updateTo(SvnMaterial material, RevisionContext revisionContext, JobResult expectedResult) {
        BuildSession buildSession = newBuildSession();
        JobResult result = buildSession.build(new SvnMaterialUpdater(material).updateTo(workingDir.toString(), revisionContext));
        assertThat(buildInfo(), result, is(expectedResult));
    }
}
