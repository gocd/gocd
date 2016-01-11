package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:WEB-INF/spring-pages-servlet.xml"
})
public class AgentRegistrationControllerIntegrationTest {
    @Autowired private AgentRegistrationController controller;
    @Autowired private GoConfigService goConfigService;

    @Test
    public void shouldRegisterAgent() throws Exception {
        String uuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, null, null, null, request);
        AgentConfig agentConfig = goConfigService.agentByUuid(uuid);
        assertThat(agentConfig.getHostname(), is("hostname"));
    }

    @Test
    public void shouldNotRegisterAgentWhenValidationFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        int totalAgentsBeforeRegistrationRequest = goConfigService.agents().size();
        controller.agentRequest("hostname", null, "sandbox", "100", null, null, null, null, null, request);
        int totalAgentsAfterRegistrationRequest = goConfigService.agents().size();
        assertThat(totalAgentsBeforeRegistrationRequest, is(totalAgentsAfterRegistrationRequest));
    }
}