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

import com.thoughtworks.go.domain.materials.perforce.P4Fixture;
import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.After;
import org.junit.Before;

import static com.thoughtworks.go.util.FileUtil.deleteFolder;

public class P4MaterialUpdaterTicketTest extends P4MaterialUpdaterTestBase {
    @Before
    public void setup() throws Exception {
        repo = P4TestRepo.createP4TestRepoWithTickets();
        repo.onSetup();
        p4Fixture = new P4Fixture();
        p4Fixture.setRepo(this.repo);
        workingDir = TestFileUtil.createTempFolder("p4Client");
        if (workingDir == null) {
            throw new RuntimeException();
        }
        p4 = p4Fixture.createClient();
    }

    @After
    public void teardown() throws Exception {
        p4Fixture.stop(p4);
        deleteFolder(workingDir);
    }
}
