/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.perforce.PerforceFixture;
import com.thoughtworks.go.helper.P4TestRepo;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.thoughtworks.go.config.MaterialRevisionsMatchers.containsModifiedFile;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class P4MultipleMaterialsTest extends PerforceFixture {
    private static final String VIEW_SRC = "//depot/src/... //something/...";
    private static final String VIEW_LIB = "//depot/lib/... //something/...";
    private P4TestRepo testRepo;

    @Override
    protected P4TestRepo createTestRepo() throws Exception {
        testRepo = super.createTestRepo();
        return testRepo;
    }

    @Test
    public void shouldUpdateToItsDestFolder() {
        P4Material p4Material = p4Fixture.material(VIEW_SRC, "dest1");

        MaterialRevision revision = new MaterialRevision(p4Material, p4Material.latestModification(clientFolder, new TestSubprocessExecutionContext()));

        revision.updateTo(clientFolder, inMemoryConsumer(), new TestSubprocessExecutionContext());

        assertThat(new File(clientFolder, "dest1/net").exists(), is(true));
    }

    @Test
    public void shouldIgnoreDestinationFolderWhenUpdateToOnServerSide() {
        P4Material p4Material = p4Fixture.material(VIEW_SRC, "dest1");

        MaterialRevision revision = new MaterialRevision(p4Material, p4Material.latestModification(clientFolder, new TestSubprocessExecutionContext()));

        revision.updateTo(clientFolder, inMemoryConsumer(), new TestSubprocessExecutionContext(true));

        assertThat(new File(clientFolder, "dest1/net").exists(), is(false));
        assertThat(new File(clientFolder, "net").exists(), is(true));
    }

    @Test
    public void shouldFoundModificationsForEachMaterial() throws Exception {
        P4Material p4Material1 = p4Fixture.material(VIEW_SRC, "src");
        P4Material p4Material2 = p4Fixture.material(VIEW_LIB, "lib");
        Materials materials = new Materials(p4Material1, p4Material2);

        testRepo.checkInOneFile(p4Material1, "filename.txt");
        testRepo.checkInOneFile(p4Material2, "filename2.txt");

        MaterialRevisions materialRevisions = materials.latestModification(clientFolder, new TestSubprocessExecutionContext());

        assertThat(materialRevisions.getRevisions().size(), is(2));
        assertThat(materialRevisions, containsModifiedFile("src/filename.txt"));
        assertThat(materialRevisions, containsModifiedFile("lib/filename2.txt"));
    }
}
