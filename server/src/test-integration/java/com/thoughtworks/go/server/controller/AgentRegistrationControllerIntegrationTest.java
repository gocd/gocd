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
import com.thoughtworks.go.security.RegistrationJSONizer;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.AgentConfigService;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class AgentRegistrationControllerIntegrationTest {
    @Autowired
    private AgentRegistrationController controller;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentConfigService agentConfigService;

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
    public void shouldRegisterLocalAgent() throws Exception {
        System.setProperty(SystemEnvironment.AUTO_REGISTER_LOCAL_AGENT_ENABLED.propertyName(), "true");
        String uuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity responseEntity = controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, null, null, null, null, null, false, token(uuid, goConfigService.serverConfig().getTokenGenerationKey()), request);
        AgentConfig agentConfig = goConfigService.agentByUuid(uuid);

        assertThat(agentConfig.getHostname(), is("hostname"));
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getHeaders().getContentType(), is(MediaType.APPLICATION_JSON));
        assertTrue(RegistrationJSONizer.fromJson(responseEntity.getBody().toString()).isValid());
    }

    @Test
    public void shouldRegisterElasticAgent() throws Exception {
        String autoRegisterKey = goConfigService.serverConfig().getAgentAutoRegisterKey();
        String uuid = UUID.randomUUID().toString();
        String elasticAgentId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity responseEntity = controller.agentRequest("elastic-agent-hostname",
                uuid,
                "sandbox",
                "100",
                "Alpine Linux v3.5",
                autoRegisterKey,
                "",
                "",
                "hostname",
                elasticAgentId,
                "elastic-plugin-id",
                false,
                token(uuid, goConfigService.serverConfig().getTokenGenerationKey()),
                request);
        AgentConfig agentConfig = goConfigService.agentByUuid(uuid);

        assertTrue(agentConfig.isElastic());
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getHeaders().getContentType(), is(MediaType.APPLICATION_JSON));
        assertTrue(RegistrationJSONizer.fromJson(responseEntity.getBody().toString()).isValid());
    }

    @Test
    public void shouldNotRegisterElasticAgentWithDuplicateElasticAgentID() throws Exception {
        String autoRegisterKey = goConfigService.serverConfig().getAgentAutoRegisterKey();
        String uuid = UUID.randomUUID().toString();
        String elasticAgentId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();

        controller.agentRequest("elastic-agent-hostname",
                uuid,
                "sandbox",
                "100",
                "Alpine Linux v3.5",
                autoRegisterKey,
                "",
                "",
                "hostname",
                elasticAgentId,
                "elastic-plugin-id",
                false,
                token(uuid, goConfigService.serverConfig().getTokenGenerationKey()),
                request);
        AgentConfig agentConfig = goConfigService.agentByUuid(uuid);
        assertTrue(agentConfig.isElastic());

        final ResponseEntity responseEntity = controller.agentRequest("elastic-agent-hostname",
                uuid,
                "sandbox",
                "100",
                "Alpine Linux v3.5",
                autoRegisterKey,
                "",
                "",
                "hostname",
                elasticAgentId,
                "elastic-plugin-id",
                false,
                token(uuid, goConfigService.serverConfig().getTokenGenerationKey()),
                request);

        assertThat(responseEntity.getStatusCode(), is(UNPROCESSABLE_ENTITY));
        assertThat(responseEntity.getBody(), is("Duplicate Elastic agent Id used to register elastic agent."));
    }

    @Test
    public void shouldAddAgentInPendingState() throws Exception {
        System.setProperty(SystemEnvironment.AUTO_REGISTER_LOCAL_AGENT_ENABLED.propertyName(), "false");
        String uuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity responseEntity = controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, null, null, null, null, null, false, token(uuid, goConfigService.serverConfig().getTokenGenerationKey()), request);
        AgentInstance agentInstance = agentService.findAgent(uuid);

        assertTrue(agentInstance.isPending());
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.ACCEPTED));
        assertThat(responseEntity.getHeaders().getContentType(), is(MediaType.APPLICATION_JSON));
        assertFalse(RegistrationJSONizer.fromJson(responseEntity.getBody().toString()).isValid());
    }

    @Test
    public void shouldAutoRegisterRemoteAgent() throws Exception {
        System.setProperty(SystemEnvironment.AUTO_REGISTER_LOCAL_AGENT_ENABLED.propertyName(), "false");
        String uuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity responseEntity = controller.agentRequest("hostname", uuid, "sandbox", "100", null, goConfigService.serverConfig().getAgentAutoRegisterKey(), "", "", null, null, null, false, token(uuid, goConfigService.serverConfig().getTokenGenerationKey()), request);
        AgentInstance agentInstance = agentService.findAgent(uuid);

        assertTrue(agentInstance.isIdle());
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getHeaders().getContentType(), is(MediaType.APPLICATION_JSON));
        assertTrue(RegistrationJSONizer.fromJson(responseEntity.getBody().toString()).isValid());
    }

    @Test
    public void shouldNotRegisterAgentWhenValidationFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        int totalAgentsBeforeRegistrationRequest = goConfigService.agents().size();
        final ResponseEntity responseEntity = controller.agentRequest("hostname", "", "sandbox", "100", null, null, null, null, null, null, null, false, token("", goConfigService.serverConfig().getTokenGenerationKey()), request);
        int totalAgentsAfterRegistrationRequest = goConfigService.agents().size();
        assertThat(totalAgentsBeforeRegistrationRequest, is(totalAgentsAfterRegistrationRequest));

        assertThat(responseEntity.getStatusCode(), is(UNPROCESSABLE_ENTITY));
        assertThat(responseEntity.getBody(), is("Error occurred during agent registration process: UUID cannot be empty"));
    }

    @Test
    public void shouldGenerateToken() throws Exception {
        final String token = token("uuid-from-agent", goConfigService.serverConfig().getTokenGenerationKey());

        final ResponseEntity responseEntity = controller.getToken("uuid-from-agent");

        assertThat(responseEntity.getStatusCode(), Matchers.is(HttpStatus.OK));
        assertThat(responseEntity.getBody(), Matchers.is(token));
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfAgentIsInPendingState() throws Exception {
        System.setProperty(SystemEnvironment.AUTO_REGISTER_LOCAL_AGENT_ENABLED.propertyName(), "false");
        final String uuid = UUID.randomUUID().toString();
        final AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(new AgentConfig(uuid, "hostname", "127.0.01"), false, "sandbox", 0l, "linux", false);
        agentService.requestRegistration(agentService.agentUsername(uuid, "127.0.0.1", "hostname"), agentRuntimeInfo);
        final AgentInstance agentInstance = agentService.findAgent(uuid);
        assertTrue(agentInstance.isPending());

        final ResponseEntity responseEntity = controller.getToken(uuid);

        assertThat(responseEntity.getStatusCode(), Matchers.is(CONFLICT));
        assertThat(responseEntity.getBody(), Matchers.is("A token has already been issued for this agent."));
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfAgentIsInConfig() throws Exception {
        System.setProperty(SystemEnvironment.AUTO_REGISTER_LOCAL_AGENT_ENABLED.propertyName(), "false");
        final String uuid = UUID.randomUUID().toString();
        agentConfigService.addAgent(new AgentConfig(uuid, "hostname", "127.0.01"), Username.ANONYMOUS);
        assertTrue(agentService.findAgent(uuid).getAgentConfigStatus().equals(AgentConfigStatus.Enabled));

        final ResponseEntity responseEntity = controller.getToken(uuid);

        assertThat(responseEntity.getStatusCode(), Matchers.is(CONFLICT));
        assertThat(responseEntity.getBody(), Matchers.is("A token has already been issued for this agent."));
    }

    @Test
    public void shouldRejectGenerateTokenRequestIfUUIDIsEmpty() throws Exception {
        final ResponseEntity responseEntity = controller.getToken("               ");

        assertThat(responseEntity.getStatusCode(), Matchers.is(CONFLICT));
        assertThat(responseEntity.getBody(), Matchers.is("UUID cannot be blank."));
    }

    @Test
    public void shouldRejectAgentRegistrationRequestWhenTokenIsInvalid() throws Exception {
        String uuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity responseEntity = controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, null, null, null, null, null, false, "invalid-token", request);

        AgentInstance agentInstance = agentService.findAgent(uuid);

        assertTrue(agentInstance.isNullAgent());
        assertThat(responseEntity.getStatusCode(), is(FORBIDDEN));
        assertThat(responseEntity.getBody(), is("Not a valid token."));
    }

    @Test
    public void shouldReIssueCertificateIfRegisteredAgentAsksForRegistrationWithoutAutoRegisterKeys() throws Exception {
        String uuid = UUID.randomUUID().toString();
        agentConfigService.addAgent(new AgentConfig(uuid, "hostname", "127.0.01"), Username.ANONYMOUS);
        assertTrue(agentService.findAgent(uuid).getAgentConfigStatus().equals(AgentConfigStatus.Enabled));

        MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity responseEntity = controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, null, null, null, null, null, false, token(uuid, goConfigService.serverConfig().getTokenGenerationKey()), request);

        AgentInstance agentInstance = agentService.findAgent(uuid);

        assertTrue(agentInstance.isIdle());
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertTrue(RegistrationJSONizer.fromJson(responseEntity.getBody().toString()).isValid());
    }

    @Test
    public void shouldReIssueCertificateIfRegisteredAgentAsksForRegistrationWithAutoRegisterKeys() throws Exception {
        String uuid = UUID.randomUUID().toString();
        agentConfigService.addAgent(new AgentConfig(uuid, "hostname", "127.0.01"), Username.ANONYMOUS);
        assertTrue(agentService.findAgent(uuid).getAgentConfigStatus().equals(AgentConfigStatus.Enabled));

        MockHttpServletRequest request = new MockHttpServletRequest();
        final ResponseEntity responseEntity = controller.agentRequest("hostname", uuid, "sandbox", "100", null, goConfigService.serverConfig().getAgentAutoRegisterKey(), "", "", null, null, null, false, token(uuid, goConfigService.serverConfig().getTokenGenerationKey()), request);

        AgentInstance agentInstance = agentService.findAgent(uuid);

        assertTrue(agentInstance.isIdle());
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertTrue(RegistrationJSONizer.fromJson(responseEntity.getBody().toString()).isValid());
    }

    private String token(String uuid, String tokenGenerationKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(tokenGenerationKey.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(uuid.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
