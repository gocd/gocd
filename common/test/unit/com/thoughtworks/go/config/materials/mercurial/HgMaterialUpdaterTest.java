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

package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialUpdater;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static com.thoughtworks.go.util.DateUtils.parseISO8601;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class HgMaterialUpdaterTest extends BuildSessionBasedTestCase {
    private HgMaterial hgMaterial;
    private static final Date FROM = parseISO8601("2008-03-03 18:40:37 +0800");
    private static final Date TO = parseISO8601("2008-03-03 23:13:50 +0800");
    private HgTestRepo hgTestRepo;
    private File workingFolder;
    private InMemoryStreamConsumer outputStreamConsumer;
    private static final String LINUX_HG_094 = "Mercurial Distributed SCM (version 0.9.4)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String LINUX_HG_101 = "Mercurial Distributed SCM (version 1.0.1)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String LINUX_HG_10 = "Mercurial Distributed SCM (version 1.0)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String WINDOWS_HG_OFFICAL_102 = "Mercurial Distributed SCM (version 1.0.2+20080813)\n"
            + "\n"
            + "Copyright (C) 2005-2008 Matt Mackall <mpm@selenic.com>; and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String WINDOWS_HG_TORTOISE = "Mercurial Distributed SCM (version 626cb86a6523+tortoisehg)";
    private static final StringRevision REVISION_0 = new StringRevision("b61d12de515d82d3a377ae3aae6e8abe516a2651");
    private static final StringRevision REVISION_1 = new StringRevision("35ff2159f303ecf986b3650fc4299a6ffe5a14e1");
    private static final StringRevision REVISION_2 = new StringRevision("ca3ebb67f527c0ad7ed26b789056823d8b9af23f");

    @Before
    public void setUp() throws Exception {
        hgTestRepo = new HgTestRepo("hgTestRepo1");
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl());
        workingFolder = TestFileUtil.createTempFolder("workingFolder");
        outputStreamConsumer = inMemoryConsumer();
    }

    @After
    public void teardown() {
        FileUtil.deleteFolder(workingFolder);
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldUpdateToSpecificRevision() throws Exception {
        updateTo(hgMaterial, new RevisionContext(REVISION_0), JobResult.Passed);
        File end2endFolder = new File(workingFolder, "end2end");
        assertThat(end2endFolder.listFiles().length, is(3));
        updateTo(hgMaterial, new RevisionContext(REVISION_1), JobResult.Passed);
        assertThat(end2endFolder.listFiles().length, is(4));
    }

    @Test
    public void shouldUpdateToDestinationFolder() throws Exception {
        hgMaterial.setFolder("dest");
        updateTo(hgMaterial, new RevisionContext(REVISION_0), JobResult.Passed);
        File end2endFolder = new File(workingFolder, "dest/end2end");
        assertThat(new File(workingFolder, "dest").exists(), is(true));
        assertThat(end2endFolder.exists(), is(true));
    }

    @Test
    public void shouldLogRepoInfoToConsoleOutWithoutFolder() throws Exception {
        updateTo(hgMaterial, new RevisionContext(new StringRevision("0")), JobResult.Passed);
        assertThat(console.output(), containsString(
                format("Start updating %s at revision %s from %s", "files", "0",
                        hgMaterial.getUrl())));
    }

    @Test
    public void failureCommandShouldNotLeakPasswordOnUrl() throws Exception {
        HgMaterial material = MaterialsMother.hgMaterial("https://foo:foopassword@this.is.absolute.not.exists");
        updateTo(material, new RevisionContext(new StringRevision("origin/master")), JobResult.Failed);
        assertThat(console.output(), containsString("https://foo:******@this.is.absolute.not.exists"));
        assertThat(console.output(), not(containsString("foopassword")));
    }

    private void updateTo(HgMaterial material, RevisionContext revisionContext, JobResult expectedResult) {
        BuildSession buildSession = newBuildSession();
        JobResult result = buildSession.build(new HgMaterialUpdater(material).updateTo(workingFolder.toString(), revisionContext));
        assertThat(buildInfo(), result, is(expectedResult));
    }
}
