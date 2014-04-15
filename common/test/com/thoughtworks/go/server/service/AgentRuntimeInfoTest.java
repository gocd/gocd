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

package com.thoughtworks.go.server.service;

import java.io.File;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AgentRuntimeInfoTest {
    private static final int OLD_IDX = 0;
    private static final int NEW_IDX = 1;
    private File pipelinesFolder;

    @Before public void setup() throws Exception {
        pipelinesFolder = new File("pipelines");
        pipelinesFolder.mkdirs();
    }

    @After public void teardown() throws Exception {
        FileUtils.deleteQuietly(pipelinesFolder);
    }

    @Test(expected = Exception.class)
    public void should() throws Exception {
        AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "127.0.0.1"), false, "", 0L, "linux");
    }

    @Test
    public void shouldUsingIdleWhenRegistrationRequestIsFromLocalAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new AgentConfig("uuid", "localhost", "127.0.0.1"), false, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), Is.is(AgentRuntimeStatus.Idle));
    }

    @Test
    public void shouldBeUnknownWhenRegistrationRequestIsFromLocalAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new AgentConfig("uuid", "localhost", "176.19.4.1"), false, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Unknown));
    }

    @Test
    public void shouldUsingIdleWhenRegistrationRequestIsFromAlreadyRegisteredAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
    }
    
    @Test
    public void shouldNotifyStatusChangeListenerOnStatusUpdate() {
        final AgentRuntimeStatus[] oldAndNewStatus = new AgentRuntimeStatus[2];
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
        assertThat(oldAndNewStatus[OLD_IDX], is(nullValue()));
        agentRuntimeInfo.setRuntimeStatus(AgentRuntimeStatus.Building, new AgentRuntimeStatus.ChangeListener() {
            public void statusUpdateRequested(AgentRuntimeInfo runtimeInfo, AgentRuntimeStatus newStatus) {
                oldAndNewStatus[OLD_IDX] = runtimeInfo.getRuntimeStatus();
                oldAndNewStatus[NEW_IDX] = newStatus;
            }
        });
        assertThat(oldAndNewStatus[OLD_IDX], is(AgentRuntimeStatus.Idle));
        assertThat(oldAndNewStatus[NEW_IDX], is(AgentRuntimeStatus.Building));
        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Building));
    }
    
    @Test
    public void shouldNotUpdateStatusWhenOldValueIsEqualToNewValue() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
        AgentRuntimeStatus.ChangeListener listener = mock(AgentRuntimeStatus.ChangeListener.class);
        agentRuntimeInfo.setRuntimeStatus(AgentRuntimeStatus.Idle, listener);
        verify(listener, never()).statusUpdateRequested(any(AgentRuntimeInfo.class), any(AgentRuntimeStatus.class));
        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
    }

    @Test
    public void shouldNotMatchRuntimeInfosWithDifferentOperatingSystems() {
        AgentRuntimeInfo linux = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");
        AgentRuntimeInfo osx = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "foo bar");
        assertThat(linux, is(not(osx)));
    }

    @Test
    public void shouldInitializeTheFreeSpaceAtAgentSide() {
        AgentIdentifier id = new AgentConfig("uuid", "localhost", "176.19.4.1").getAgentIdentifier();
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(id, "cookie", null);
        
        assertThat(agentRuntimeInfo.getUsableSpace(), is(not(0L)));
    }

    @Test
    public void shouldNotBeLowDiskSpaceForMissingAgent(){
        assertThat(AgentRuntimeInfo.initialState(new AgentConfig("uuid")).isLowDiskSpace(10L), is(false));
    }

    @Test
    public void shouldReturnTrueIfUsableSpaceLessThanLimit(){
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.initialState(new AgentConfig("uuid"));
        agentRuntimeInfo.setUsableSpace(10L);
        assertThat(agentRuntimeInfo.isLowDiskSpace(20L), is(true));
    }

    @Test
    public void shouldUnderstandOperatingSystem() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "abc"), "cookie", null);
        assertThat(agentRuntimeInfo.getOperatingSystem(), is(new SystemEnvironment().getOperatingSystemName()));
    }

    @Test
    public void shouldHaveRelevantFieldsInDebugString() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), "cookie", null);
        assertThat(agentRuntimeInfo.agentInfoDebugString(), is("Agent [localhost, 127.0.0.1, uuid, cookie]"));
    }

    @Test
    public void shouldHaveBeautifulPhigureLikeDisplayString() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), "cookie", null);
        agentRuntimeInfo.setLocation("/nim/appan/mane");
        assertThat(agentRuntimeInfo.agentInfoForDisplay(), is("Agent located at [localhost, 127.0.0.1, /nim/appan/mane]"));
    }

    @Test
    public void shouldTellIfHasCookie() throws Exception {
        assertThat(AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), "cookie", null).hasDuplicateCookie("cookie"), is(false));
        assertThat(AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), "cookie", null).hasDuplicateCookie("different"), is(true));
        assertThat(AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), null, null).hasDuplicateCookie("cookie"), is(false));
        assertThat(AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), "cookie", null).hasDuplicateCookie(null), is(false));
    }

    @Test
    public void shouldUpdateSelfForAnIdleAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("localhost", "127.0.0.1", "uuid"));
        AgentRuntimeInfo newRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("go02", "10.10.10.1", "uuid"), "cookie", "12.3");
        newRuntimeInfo.setBuildingInfo(new AgentBuildingInfo("Idle",""));
        newRuntimeInfo.setLocation("home");
        newRuntimeInfo.setUsableSpace(10L);
        newRuntimeInfo.setOperatingSystem("Linux");

        agentRuntimeInfo.updateSelf(newRuntimeInfo);

        assertThat(agentRuntimeInfo.getBuildingInfo(), is(newRuntimeInfo.getBuildingInfo()));
        assertThat(agentRuntimeInfo.getLocation(), is(newRuntimeInfo.getLocation()));
        assertThat(agentRuntimeInfo.getUsableSpace(), is(newRuntimeInfo.getUsableSpace()));
        assertThat(agentRuntimeInfo.getOperatingSystem(), is(newRuntimeInfo.getOperatingSystem()));
        assertThat(agentRuntimeInfo.getAgentLauncherVersion(), is(newRuntimeInfo.getAgentLauncherVersion()));
    }
}
