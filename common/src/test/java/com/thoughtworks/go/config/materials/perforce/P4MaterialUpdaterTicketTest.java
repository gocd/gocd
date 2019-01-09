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

package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.perforce.P4Fixture;
import com.thoughtworks.go.helper.P4TestRepo;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class P4MaterialUpdaterTicketTest extends P4MaterialUpdaterTestBase {
    @Before
    public void setup() throws Exception {
        repo = P4TestRepo.createP4TestRepoWithTickets(temporaryFolder, temporaryFolder.newFolder());
        repo.onSetup();
        p4Fixture = new P4Fixture();
        p4Fixture.setRepo(this.repo);
        workingDir = temporaryFolder.newFolder("p4Client");
        if (workingDir == null) {
            throw new RuntimeException();
        }
        p4 = p4Fixture.createClient();
    }

    @After
    public void teardown() throws Exception {
        p4Fixture.stop(p4);
        FileUtils.deleteQuietly(workingDir);
    }

    @Test
    public void shouldNotDisplayPassword() throws Exception {
        P4Material material = p4Fixture.material(VIEW);
        material.setPassword("wubba lubba dub dub");
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Failed);
        assertThat(console.output(), not(containsString("wubba lubba dub dub")));
    }
}
