/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AgentRuntimeInfoTest {
    private static final int OLD_IDX = 0;
    private static final int NEW_IDX = 1;
    private File pipelinesFolder;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Mock
    private TimeProvider timeProvider;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        pipelinesFolder = new File("pipelines");
        pipelinesFolder.mkdirs();
    }

    @After
    public void teardown() throws Exception {
        FileUtils.deleteQuietly(pipelinesFolder);
    }

    @Test(expected = Exception.class)
    public void should() throws Exception {
        AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "127.0.0.1"), false, "", 0L, "linux", false, timeProvider);
    }

    @Test
    public void shouldUsingIdleWhenRegistrationRequestIsFromLocalAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new AgentConfig("uuid", "localhost", "127.0.0.1"), false, "/var/lib", 0L, "linux", false, timeProvider);

        assertThat(agentRuntimeInfo.getRuntimeStatus(), Is.is(AgentRuntimeStatus.Idle));
    }

    @Test
    public void shouldBeUnknownWhenRegistrationRequestIsFromLocalAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new AgentConfig("uuid", "localhost", "176.19.4.1"), false, "/var/lib", 0L, "linux", false, timeProvider);

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Unknown));
    }

    @Test
    public void shouldUsingIdleWhenRegistrationRequestIsFromAlreadyRegisteredAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux", false, timeProvider);

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
    }

    @Test
    public void shouldNotifyStatusChangeListenerOnStatusUpdate() {
        final AgentRuntimeStatus[] oldAndNewStatus = new AgentRuntimeStatus[2];
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux", false, timeProvider);

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
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux", false, timeProvider);

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
        AgentRuntimeStatus.ChangeListener listener = mock(AgentRuntimeStatus.ChangeListener.class);
        agentRuntimeInfo.setRuntimeStatus(AgentRuntimeStatus.Idle, listener);
        verify(listener, never()).statusUpdateRequested(any(AgentRuntimeInfo.class), any(AgentRuntimeStatus.class));
        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
    }

    @Test
    public void shouldNotMatchRuntimeInfosWithDifferentOperatingSystems() {
        AgentRuntimeInfo linux = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux", false, timeProvider);
        AgentRuntimeInfo osx = AgentRuntimeInfo.fromServer(new AgentConfig("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "foo bar", false, timeProvider);
        assertThat(linux, is(not(osx)));
    }

    @Test
    public void shouldInitializeTheFreeSpaceAtAgentSide() {
        AgentIdentifier id = new AgentConfig("uuid", "localhost", "176.19.4.1").getAgentIdentifier();
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(id, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider);

        assertThat(agentRuntimeInfo.getUsableSpace(), is(not(0L)));
    }

    @Test
    public void shouldNotBeLowDiskSpaceForMissingAgent() {
        assertThat(AgentRuntimeInfo.initialState(new AgentConfig("uuid"), timeProvider).isLowDiskSpace(10L), is(false));
    }

    @Test
    public void shouldReturnTrueIfUsableSpaceLessThanLimit() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.initialState(new AgentConfig("uuid"), timeProvider);
        agentRuntimeInfo.setUsableSpace(10L);
        assertThat(agentRuntimeInfo.isLowDiskSpace(20L), is(true));
    }

    @Test
    public void shouldHaveRelevantFieldsInDebugString() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider);
        assertThat(agentRuntimeInfo.agentInfoDebugString(), is("Agent [localhost, 127.0.0.1, uuid, cookie]"));
    }

    @Test
    public void shouldHaveBeautifulPhigureLikeDisplayString() throws Exception {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider);
        agentRuntimeInfo.setLocation("/nim/appan/mane");
        assertThat(agentRuntimeInfo.agentInfoForDisplay(), is("Agent located at [localhost, 127.0.0.1, /nim/appan/mane]"));
    }

    @Test
    public void shouldTellIfHasCookie() throws Exception {
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider).hasDuplicateCookie("cookie"), is(false));
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider).hasDuplicateCookie("different"), is(true));
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null, false, timeProvider).hasDuplicateCookie("cookie"), is(false));
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider).hasDuplicateCookie(null), is(false));
    }

    @Test
    public void shouldUpdateSelfForAnIdleAgent() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null, false, timeProvider);
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("go02", "10.10.10.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false, timeProvider);
        newRuntimeInfo.setBuildingInfo(new AgentBuildingInfo("Idle", ""));
        newRuntimeInfo.setLocation("home");
        newRuntimeInfo.setUsableSpace(10L);
        newRuntimeInfo.setOperatingSystem("Linux");

        agentRuntimeInfo.updateSelf(newRuntimeInfo);

        assertThat(agentRuntimeInfo.getBuildingInfo(), is(newRuntimeInfo.getBuildingInfo()));
        assertThat(agentRuntimeInfo.getLocation(), is(newRuntimeInfo.getLocation()));
        assertThat(agentRuntimeInfo.getUsableSpace(), is(newRuntimeInfo.getUsableSpace()));
        assertThat(agentRuntimeInfo.getOperatingSystem(), is(newRuntimeInfo.getOperatingSystem()));
    }

    @Test
    public void dataMapEncodingAndDecoding() {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("go02", "10.10.10.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", true, timeProvider);
        AgentRuntimeInfo clonedInfo = MessageEncoding.decodeData(MessageEncoding.encodeData(info), AgentRuntimeInfo.class);
        assertThat(clonedInfo, is(info));
    }

    @Test
    public void shouldUpdateLastUpdatedTimeWhenAgentBecomesBusy() throws Exception {
        when(timeProvider.currentTimeMillis()).thenReturn(1234L).thenReturn(2345L);

        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("go02", "10.10.10.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", true, timeProvider);
        Long lastUpdated = info.getLastUpdatedTime();

        info.busy(new AgentBuildingInfo("dfoo", "bar"));
        Long newlyUpdatedTime = info.getLastUpdatedTime();

        assertNotEquals(lastUpdated, newlyUpdatedTime);
        assertTrue(lastUpdated < newlyUpdatedTime);
    }

}
