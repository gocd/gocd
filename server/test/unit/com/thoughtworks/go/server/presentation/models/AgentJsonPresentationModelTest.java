/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.server.service.AgentBuildingInfo;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AgentJsonPresentationModelTest {
    private static final String PENDING = AgentStatus.Pending.toString().toLowerCase();
    private static final String DENIED = AgentStatus.Disabled.toString().toLowerCase();
    private SystemEnvironment systemEnvironment;

    @Before
    public void setup() {
        this.systemEnvironment = new SystemEnvironment();
    }

    @After
    public void teardown() {
        new SystemEnvironment().setProperty("agent.connection.timeout", "300");
    }

    @Test
    public void shouldTurnPendingAgentIntoAMapForJsonViewToConsume() {
        AgentConfig agentConfig = new AgentConfig(null, "localhost", "192.168.0.1");
        AgentInstance activity = AgentInstance.create(agentConfig, false, systemEnvironment);
        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(activity);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'hostname' : 'localhost',"
                        + "  'status' : '" + PENDING + "',"
                        + "  'canApprove' : 'true',"
                        + "  'canDeny' : 'true',"
                        + "  'ipAddress' : '192.168.0.1' }"
        );
    }

    @Test
    public void shouldTurnDeniedAgentIntoAMapForJsonViewToConsume() {
        AgentInstance instance = AgentInstance.create(new AgentConfig(null, "localhost", "192.168.0.1"),
                false,
                systemEnvironment);
        instance.enable();
        instance.deny();

        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(instance);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'hostname' : 'localhost',"
                        + "  'status' : '" + DENIED + "',"
                        + "  'ipAddress' : '192.168.0.1' }"
        );
    }

    @Test
    public void shouldTurnAnAgentIntoAMapForJsonViewToConsumeWithUuid() {
        AgentInstance activity = AgentInstance.create(new AgentConfig("12345", "localhost", "192.168.0.1"),
                false,
                systemEnvironment);
        activity.enable();
        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(activity);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'agentId' : '12345',"
                        + "  'hostname' : 'localhost',"
                        + "  'status' : '" + AgentStatus.Idle.toString().toLowerCase() + "',"
                        + "  'canDeny' : 'true',"
                        + "  'canEditResource' : 'true',"
                        + "  'ipAddress' : '192.168.0.1' }"
        );
    }

    @Test
    public void agentWithUuidReturnsStatusIdle() {
        AgentInstance activity = AgentInstance.create(new AgentConfig("1234", "localhost", "192.168.0.1"),
                false,
                systemEnvironment);
        activity.enable();
        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(activity);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'hostname' : 'localhost',"
                        + "  'status' : 'idle' }"
        );
    }

    @Test
    public void agentJsonWithBuildingStateAndInfo() {
        AgentConfig agentConfig = new AgentConfig("1234", "localhost", "192.168.0.1");
        AgentInstance agentInstance = AgentInstance.create(agentConfig, false, systemEnvironment);
        agentInstance.enable();
        String buildingInfo = "pipeline/label/stage/job";
        agentInstance.building(new AgentBuildingInfo(buildingInfo, "buildLocator"));

        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(agentInstance);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'hostname' : 'localhost',"
                        + "  'status' : 'building',"
                        + "  'buildingInfo' : '" + buildingInfo + "',"
                        + "  'buildLocator' : 'buildLocator' }"
        );
    }

    @Test
    public void buildingInfoShouldBeNullSafe() {
        AgentConfig agentConfig = new AgentConfig("1234", "localhost", "192.168.0.1");
        AgentInstance activity = AgentInstance.create(agentConfig, false, systemEnvironment);
        activity.enable();
        activity.building(new AgentBuildingInfo("something", "buildLocator"));

        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(activity);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'hostname' : 'localhost',"
                        + "  'status' : 'building' }"
        );
    }

    @Test
    public void agentWithResources() {
        AgentConfig agentConfig = new AgentConfig("1234", "localhost", "192.168.0.1");
        agentConfig.addResource(new Resource("jdk1.4"));
        agentConfig.addResource(new Resource("jdk1.5"));
        AgentInstance activity = AgentInstance.create(agentConfig, false, systemEnvironment);
        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(activity);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'resources' : [ 'jdk1.4', 'jdk1.5' ] }"
        );
    }

    @Test
    public void agentWithLostContactStatusHumanizedFormat() throws Exception {
        new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
        AgentConfig agentConfig = new AgentConfig("1234", "localhost", "192.168.0.1");
        AgentInstance agentInstance = AgentInstance.create(agentConfig, false, systemEnvironment);
        agentInstance.enable();
        agentInstance.update(AgentRuntimeInfo.fromAgent(agentConfig.getAgentIdentifier(), "cookie", null));
        agentInstance.refresh(null);

        AgentJsonPresentationModel adaptor = new AgentJsonPresentationModel(agentInstance);
        Map json = adaptor.toJsonHash();
        new JsonTester(json).shouldContain(
                "{ 'humanizedStatus' : 'lost contact',"
                        + " 'status' : 'lostcontact'}");
    }

    @Test
    public void shouldShowUnknownForMissingAgent() {
        AgentInstance instance = AgentInstanceMother.missing();
        AgentJsonPresentationModel model = new AgentJsonPresentationModel(instance);
        JsonValue json = JsonUtils.from(model.toJsonHash());
        assertThat(json.getString("usablespace"), is("unknown"));
        assertThat(json.getString("location"), is("unknown"));
        assertThat(json.getString("isLowSpace"), is("false"));
    }

    @Test
    public void shouldShowFreeDiskSpaceForOnlineAgent() {
        AgentInstance instance = AgentInstanceMother.building();
        AgentJsonPresentationModel model = new AgentJsonPresentationModel(instance);
        JsonValue json = JsonUtils.from(model.toJsonHash());
        assertThat(json.getString("usablespace"),
                is(FileUtil.byteCountToDisplaySize(instance.getUsableSpace()) + " free"));
    }

}
