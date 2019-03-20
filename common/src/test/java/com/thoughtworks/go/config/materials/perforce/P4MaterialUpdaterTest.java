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

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.perforce.P4Fixture;
import com.thoughtworks.go.domain.materials.perforce.P4MaterialUpdater;
import com.thoughtworks.go.helper.P4TestRepo;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class P4MaterialUpdaterTest extends P4MaterialUpdaterTestBase {
    @BeforeEach
    void setup() throws Exception {
        repo = P4TestRepo.createP4TestRepo(temporaryFolder, temporaryFolder.newFolder());
        repo.onSetup();
        p4Fixture = new P4Fixture();
        p4Fixture.setRepo(this.repo);
        workingDir = temporaryFolder.newFolder("p4Client");
        if (workingDir == null) {
            throw new RuntimeException();
        }
        p4 = p4Fixture.createClient();
    }

    @AfterEach
    void teardown() {
        p4Fixture.stop(p4);
        FileUtils.deleteQuietly(workingDir);
    }

    @Test
    void shouldNotDisplayPassword() {
        P4Material material = p4Fixture.material(VIEW);
        material.setPassword("wubba lubba dub dub");
        updateTo(material, new RevisionContext(REVISION_2), JobResult.Passed);
        assertThat(console.output()).doesNotContain("wubba lubba dub dub");
    }

    @Test
    void shouldUsePasswordForCommandLineWhileBuildingAnCommand() {
        P4Material material = p4Fixture.material(VIEW);
        material.setPassword("#{SECRET[secret_config_id][lookup_pass]}");

        material.getSecretParams().findFirst("lookup_pass").ifPresent(secretParam -> secretParam.setValue("resolved_password"));

        final BuildCommand buildCommand = new P4MaterialUpdater(material).updateTo("baseDir", new RevisionContext(mock(Revision.class)));

        assertThat(buildCommand.dump())
                .contains("resolved_password")
                .doesNotContain("#{SECRET[secret_config_id][lookup_pass]}");

    }
}
