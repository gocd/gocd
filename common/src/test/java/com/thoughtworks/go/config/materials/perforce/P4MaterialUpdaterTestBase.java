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

package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.domain.materials.perforce.P4Client;
import com.thoughtworks.go.domain.materials.perforce.P4Fixture;
import com.thoughtworks.go.domain.materials.perforce.P4MaterialUpdater;
import com.thoughtworks.go.helper.P4TestRepo;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class P4MaterialUpdaterTestBase extends BuildSessionBasedTestCase {
    protected File workingDir;
    protected P4TestRepo repo;
    P4Fixture p4Fixture;
    protected P4Client p4;

    protected final StringRevision REVISION_1 = new StringRevision("1");
    protected final StringRevision REVISION_2 = new StringRevision("2");
    private final StringRevision REVISION_3 = new StringRevision("3");
    protected static final String VIEW = "//depot/... //something/...";

    @Test
    void shouldCleanWorkingDir() throws Exception {
        P4Material material = p4Fixture.material(VIEW);
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        File tmpFile = new File(workingDir, "shouldBeDeleted");
        FileUtils.writeStringToFile(tmpFile, "testing", UTF_8);
        assert (tmpFile.exists());
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        assert (!tmpFile.exists());
    }

    @Test
    void shouldSyncToSpecifiedRevision() {
        P4Material material = p4Fixture.material(VIEW);
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        assertThat(workingDir.listFiles()).hasSize(7);
        updateTo(material, new RevisionContext(REVISION_3), JobResult.Passed);
        assertThat(workingDir.listFiles()).hasSize(6);
    }

    @Test
    void shouldNotFailIfDestDoesNotExist() throws Exception {
        FileUtils.deleteDirectory(workingDir);
        assert (!workingDir.exists());
        P4Material material = p4Fixture.material(VIEW);
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        assert (workingDir.exists());
    }

    @Test
    void shouldSupportCustomDestinations() {
        P4Material material = p4Fixture.material(VIEW);
        material.setFolder("dest");
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        assertThat(workingDir.listFiles()).hasSize(1);
        assertThat(new File(workingDir, "dest").listFiles()).hasSize(7);
    }

    protected void updateTo(P4Material material, RevisionContext revisionContext, JobResult expectedResult) {
        BuildSession buildSession = newBuildSession();
        JobResult result = buildSession.build(new P4MaterialUpdater(material).updateTo(workingDir.toString(), revisionContext));
        assertThat(result).as(buildInfo()).isEqualTo(expectedResult);
    }
}
