/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.materials.perforce;

import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class P4CommandTestBase extends PerforceFixture {
    private static final String EMPTY_VIEW = "//depot/dir1/old.* //cws/renamed/new.*";
    private InMemoryStreamConsumer output = inMemoryConsumer();

    @Test public void shouldCheckConnection() throws Exception {
        ConsoleResult info = p4.checkConnection();
        assertThat(info.returnValue(), is(0));//unable to verify failure for wrong password
        assertThat(info.error().size(), is(0));
    }
    @Test(expected = RuntimeException.class)
    public void shouldCheckConnectionAndReturnErrorIfIncorrectDepotMentioned() throws Exception {
        p4 = p4Fixture.createClient("client", "//NonExistantDepot/... //client/..." );
        p4.checkConnection();
    }

    @Test
    public void shouldCheckConnectionAndReturnErrorIfIncorrectViewMentioned() throws Exception {
        p4 = p4Fixture.createClient("client", "//depot/FolderThatDoesNotExist... //client/..." );
        try{
            p4.checkConnection();
        }
        catch(Exception e) {
            assertThat(e, is(instanceOf(RuntimeException.class)));
            assertThat(e.getMessage(), containsString("STDERR: //client/... - no such file(s)"));
        }
    }

    @Test public void shouldInitializeClient() throws Exception {
        InMemoryStreamConsumer output = inMemoryConsumer();
        p4.client(clientConfig("new_p4_client", clientFolder), output, true);
        assertThat(output.getStdOut(), containsString("Client new_p4_client saved."));
        p4.client(clientConfig("new_p4_client", clientFolder), output, true);
        assertThat(output.getStdOut(), containsString("Client new_p4_client not changed."));
    }

    @Test
    public void shouldCreateClientSpecWithProvidedValues(){
        InMemoryStreamConsumer output = inMemoryConsumer();
        p4.execute(p4.p4("client", "-o"), null, output, true);
        String actualClientSpec = output.getStdOut();
        assertThat(actualClientSpec.contains("Client:\tp4test_1"), is(true));
        assertThat(actualClientSpec.contains("Options:\tnoallwrite clobber nocompress unlocked nomodtime rmdir"), is(true));
        assertThat(actualClientSpec.contains("View:\n\t//depot/... //p4test_1/..."), is(true));
        assertThat(actualClientSpec.contains("LineEnd:\tlocal"), is(true));
    }

    @Test
    public void shouldGetLatestChange() throws Exception {
        List<Modification> modificationList = p4.latestChange();
        assertThat(modificationList.size(), is(1));
        assertThat(modificationList.get(0).getRevision(), is("4"));
    }

    @Test
    public void shouldGetChangesSinceARevision() throws Exception {
        List<Modification> output = p4.changesSince(new StringRevision("1"));

        assertThat(output.size(), is(3));
        assertThat(output.get(0).getRevision(), is("4"));
        assertThat(output.get(1).getRevision(), is("3"));
        assertThat(output.get(2).getRevision(), is("2"));
    }

    @Test public void shouldSync() throws Exception {
        assertThat(output.getStdOut(), is(""));
        p4.sync(2, false, output);
        assertThat(output.getAllOutput(), containsString("//depot/"));
        assertThat(clientFolder.listFiles().length, is(7));
        p4.sync(3, false, output);
        assertThat(clientFolder.listFiles().length, is(6));
    }

    @Test
    public void shouldBombForNonZeroReturnCode() throws Exception{
        ProcessOutputStreamConsumer outputStreamConsumer = Mockito.mock(ProcessOutputStreamConsumer.class);
        CommandLine line = Mockito.mock(CommandLine.class);
        when(line.run(outputStreamConsumer, null, "foo")).thenReturn(1);
        try {
            p4.execute(line, "foo", outputStreamConsumer, true);
            fail("did't bomb for non zero return code");
        } catch (Exception ignored) {
        }
        verify(line).run(outputStreamConsumer, null, "foo");
    }

    @Test
    public void shouldDescribeChange() throws Exception {
        String output = p4.describe(2);
        assertThat(output, containsString("... //depot/cruise-config.xml#1 add"));
        output = p4.describe(3);
        assertThat(output, containsString("... //depot/cruise-output/log.xml#2 delete"));
    }

    @Test
    public void shouldReturnEmptyModificationListWhenP4OutputIsEmpty() throws Exception {
        P4Material anotherMaterial = p4Fixture.material(EMPTY_VIEW);
        List<Modification> materialRevisions = anotherMaterial.latestModification(clientFolder, new TestSubprocessExecutionContext());
        assertThat(materialRevisions.size(), is(0));

        List<Modification> mods = anotherMaterial.modificationsSince(clientFolder, new StringRevision("1"), new TestSubprocessExecutionContext());
        assertThat(mods.size(), is(0));
    }
}
