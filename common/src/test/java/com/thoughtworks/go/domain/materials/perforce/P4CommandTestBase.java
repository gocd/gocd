/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.perforce;

import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.MaterialFingerprintTag;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

abstract class P4CommandTestBase extends PerforceFixture {
    private static final String EMPTY_VIEW = "//depot/dir1/old.* //cws/renamed/new.*";
    private InMemoryStreamConsumer output = inMemoryConsumer();

    @Test
    void shouldCheckConnection() {
        ConsoleResult info = p4.checkConnection();
        assertThat(info.returnValue()).isEqualTo(0);//unable to verify failure for wrong password
        assertThat(info.error().size()).isEqualTo(0);
    }

    @Test
    void shouldCheckConnectionAndReturnErrorIfIncorrectDepotMentioned() {
        assertThatCode(() -> p4 = p4Fixture.createClient("client", "//NonExistantDepot/... //client/..."))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldCheckConnectionAndReturnErrorIfIncorrectViewMentioned() throws Exception {
        p4 = p4Fixture.createClient("client", "//depot/FolderThatDoesNotExist... //client/...");

        assertThatCode(() -> p4.checkConnection())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("STDERR: //client/... - no such file(s)");
    }

    @Test
    void shouldInitializeClient() {
        InMemoryStreamConsumer output = inMemoryConsumer();
        p4.client(clientConfig("new_p4_client", clientFolder), output, true);
        assertThat(output.getStdOut()).contains("Client new_p4_client saved.");
        p4.client(clientConfig("new_p4_client", clientFolder), output, true);
        assertThat(output.getStdOut()).contains("Client new_p4_client not changed.");
    }

    @Test
    void shouldCreateClientSpecWithProvidedValues() {
        InMemoryStreamConsumer output = inMemoryConsumer();
        p4.execute(p4.p4("client", "-o"), null, output, true);
        String actualClientSpec = output.getStdOut();
        assertThat(actualClientSpec.contains("Client:\tp4test_1")).isTrue();
        assertThat(actualClientSpec.contains("Options:\tnoallwrite clobber nocompress unlocked nomodtime rmdir")).isTrue();
        assertThat(actualClientSpec.contains("View:\n\t//depot/... //p4test_1/...")).isTrue();
        assertThat(actualClientSpec.contains("LineEnd:\tlocal")).isTrue();
    }

    @Test
    void shouldGetLatestChange() {
        List<Modification> modificationList = p4.latestChange();
        assertThat(modificationList.size()).isEqualTo(1);
        assertThat(modificationList.get(0).getRevision()).isEqualTo("4");
    }

    @Test
    void shouldGetChangesSinceARevision() {
        List<Modification> output = p4.changesSince(new StringRevision("1"));

        assertThat(output.size()).isEqualTo(3);
        assertThat(output.get(0).getRevision()).isEqualTo("4");
        assertThat(output.get(1).getRevision()).isEqualTo("3");
        assertThat(output.get(2).getRevision()).isEqualTo("2");
    }

    @Test
    void shouldSync() {
        assertThat(output.getStdOut()).isEqualTo("");
        p4.sync(2, false, output);
        assertThat(output.getAllOutput()).contains("//depot/");
        assertThat(clientFolder.listFiles().length).isEqualTo(7);
        p4.sync(3, false, output);
        assertThat(clientFolder.listFiles().length).isEqualTo(6);
    }

    @Test
    void shouldBombForNonZeroReturnCode() {
        ProcessOutputStreamConsumer outputStreamConsumer = Mockito.mock(ProcessOutputStreamConsumer.class);
        CommandLine line = Mockito.mock(CommandLine.class);
        when(line.run(outputStreamConsumer, new MaterialFingerprintTag(null), "foo")).thenReturn(1);
        try {
            p4.execute(line, "foo", outputStreamConsumer, true);
            fail("did't bomb for non zero return code");
        } catch (Exception ignored) {
        }
        verify(line).run(outputStreamConsumer, new MaterialFingerprintTag(null), "foo");
    }

    @Test
    void shouldDescribeChange() {
        String output = p4.describe(2);
        assertThat(output).contains("... //depot/cruise-config.xml#1 add");
        output = p4.describe(3);
        assertThat(output).contains("... //depot/cruise-output/log.xml#2 delete");
    }

    @Test
    void shouldReturnEmptyModificationListWhenP4OutputIsEmpty() {
        P4Material anotherMaterial = p4Fixture.material(EMPTY_VIEW);
        List<Modification> materialRevisions = anotherMaterial.latestModification(clientFolder, new TestSubprocessExecutionContext());
        assertThat(materialRevisions.size()).isEqualTo(0);

        List<Modification> mods = anotherMaterial.modificationsSince(clientFolder, new StringRevision("1"), new TestSubprocessExecutionContext());
        assertThat(mods.size()).isEqualTo(0);
    }
}
