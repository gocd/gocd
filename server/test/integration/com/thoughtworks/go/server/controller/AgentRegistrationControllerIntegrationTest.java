/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.controller;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.RegistrationJSONizer;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:WEB-INF/spring-pages-servlet.xml"
})
public class AgentRegistrationControllerIntegrationTest {
    @Autowired
    private AgentRegistrationController controller;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private TimeProvider timeProvider;

    static final Cloner CLONER = new Cloner();
    private Properties original;

    @Before
    public void before() {
        original = CLONER.deepClone(System.getProperties());
    }

    @After
    public void after() {
        System.setProperties(original);
    }

    @Test
    public void shouldRegisterAgentInPendingStateWhenLocalAgentAutoRegistrationIsDisabled() throws Exception {
        System.setProperty(SystemEnvironment.AUTO_REGISTER_LOCAL_AGENT_ENABLED.propertyName(), "false");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final String uuid = UUID.randomUUID().toString();

        final ModelAndView modelAndView = controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, "", "", null, null, null, false, request);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        modelAndView.getView().render(null, null, response);

        assertFalse(goConfigService.hasAgent(uuid));
        assertThat(response.getStatus(), is(202));
        assertThat(response.getContentAsString(), is(RegistrationJSONizer.toJson(Registration.createNullPrivateKeyEntry())));
        assertTrue(agentService.findAgent(uuid).getStatus().getConfigStatus() == AgentConfigStatus.Pending);
    }

    @Test
    public void shouldRegisterAgentInEnabledStateWhenLocalAgentAutoRegistrationIsEnabled() throws Exception {
        System.setProperty(SystemEnvironment.AUTO_REGISTER_LOCAL_AGENT_ENABLED.propertyName(), "true");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final String uuid = UUID.randomUUID().toString();

        final ModelAndView modelAndView = controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, "", "", null, null, null, false, request);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        modelAndView.getView().render(null, null, response);

        assertTrue(goConfigService.hasAgent(uuid));
        assertThat(response.getStatus(), is(200));
        assertTrue(RegistrationJSONizer.fromJson(response.getContentAsString()).isValid());
        assertTrue(agentService.findAgent(uuid).getStatus().getConfigStatus() == AgentConfigStatus.Enabled);
    }

    @Test
    public void shouldNotRegisterAgentWhenValidationFails() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final int totalAgentsBeforeRegistrationRequest = goConfigService.agents().size();

        final ModelAndView modelAndView = controller.agentRequest("hostname", null, "sandbox", "100", null, null, null, null, null, null, null, false, request);

        final int totalAgentsAfterRegistrationRequest = goConfigService.agents().size();
        assertThat(totalAgentsBeforeRegistrationRequest, is(totalAgentsAfterRegistrationRequest));

        final MockHttpServletResponse response = new MockHttpServletResponse();
        modelAndView.getView().render(null, null, response);

        assertThat(response.getStatus(), is(202));
        assertThat(response.getContentAsString(), is(RegistrationJSONizer.toJson(Registration.createNullPrivateKeyEntry())));

    }

    @Test
    public void shouldAutoRegisterAgentWhenAutoRegisterKeyIsProvided() throws Exception {
        final String uuid = UUID.randomUUID().toString();
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final ModelAndView modelAndView = controller.agentRequest("hostname", uuid, "sandbox", "100", null, goConfigService.serverConfig().getAgentAutoRegisterKey(), "", "", null, null, null, false, request);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        modelAndView.getView().render(null, null, response);

        assertTrue(goConfigService.hasAgent(uuid));
        assertThat(response.getStatus(), is(200));
        assertTrue(RegistrationJSONizer.fromJson(response.getContentAsString()).isValid());
        assertTrue(agentService.findAgent(uuid).getStatus().getRuntimeStatus().agentState() == AgentRuntimeStatus.Idle);
    }

    @Test
    public void shouldRegisterAgentWithoutAddingAgentConfigToConfigXML_forAlreadyRegisteredAgent() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String uuid = registerAgentWithAgentRuntimeStatus(AgentRuntimeStatus.LostContact);

        final ModelAndView modelAndView = controller.agentRequest("hostname", uuid, "sandbox", "100", null, goConfigService.serverConfig().getAgentAutoRegisterKey(), "", "", null, null, null, false, request);

        modelAndView.getView().render(null, null, response);

        assertTrue(goConfigService.hasAgent(uuid));
        assertThat(response.getStatus(), is(200));
        assertTrue(RegistrationJSONizer.fromJson(response.getContentAsString()).isValid());
        assertTrue(agentService.findAgent(uuid).getStatus().getRuntimeStatus().agentState() == AgentRuntimeStatus.Idle);
    }

    @Test
    public void shouldNotProcessRegistrationRequestWhenAutoRegisterKeyIsNotProvided_forAlreadyRegisteredAgent() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String uuid = registerAgentWithAgentRuntimeStatus(AgentRuntimeStatus.LostContact);

        final ModelAndView modelAndView = controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, "", "", null, null, null, false, request);

        modelAndView.getView().render(null, null, response);
        assertTrue(goConfigService.hasAgent(uuid));
        assertThat(response.getStatus(), is(202));
        assertThat(response.getContentAsString(), is(RegistrationJSONizer.toJson(Registration.createNullPrivateKeyEntry())));
        assertTrue(agentService.findAgent(uuid).getStatus().getRuntimeStatus().agentState() == AgentRuntimeStatus.LostContact);
    }

    @Test
    public void shouldRegisterElasticAgent() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String uuid = UUID.randomUUID().toString();

        final ModelAndView modelAndView = controller.agentRequest("hostname", uuid, "sandbox", "100", null, goConfigService.serverConfig().getAgentAutoRegisterKey(), "", "", null, "1a2dffb-4832-4fdf-b9e4-5d598cc48c8e", "cd.go.contrib.docker-swarm", false, request);

        modelAndView.getView().render(null, null, response);
        assertTrue(goConfigService.hasAgent(uuid));
        assertThat(response.getStatus(), is(200));
        assertTrue(RegistrationJSONizer.fromJson(response.getContentAsString()).isValid());

        final AgentInstance agentInstance = agentService.findAgent(uuid);
        assertTrue(agentInstance.isElastic());
        assertTrue(agentInstance.getStatus().getRuntimeStatus().agentState() == AgentRuntimeStatus.Idle);
    }

    private String registerAgentWithAgentRuntimeStatus(AgentRuntimeStatus agentRuntimeStatus) throws IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final String uuid = UUID.randomUUID().toString();

        controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, "", "", null, null, null, false, request);
        AgentConfig agentConfig = goConfigService.agentByUuid(uuid);

        assertTrue(agentService.findAgent(uuid).getStatus().getRuntimeStatus().agentState() == AgentRuntimeStatus.Idle);
        assertTrue(goConfigService.hasAgent(uuid));

        agentService.updateRuntimeInfo(new AgentRuntimeInfo(
                agentConfig.getAgentIdentifier(), agentRuntimeStatus, "sandbox", "foo", false, timeProvider)
        );

        assertTrue(agentService.findAgent(uuid).getStatus().getRuntimeStatus().agentState() == agentRuntimeStatus);

        return uuid;
    }
}