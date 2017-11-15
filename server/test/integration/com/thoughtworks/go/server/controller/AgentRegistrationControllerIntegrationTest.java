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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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
    @Autowired
    private AgentRegistrationController controller;
    @Autowired
    private GoConfigService goConfigService;

    @Test
    public void shouldRegisterAgent() throws Exception {
        String uuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        controller.agentRequest("hostname", uuid, "sandbox", "100", null, null, null, null, null, null, null, false, token(uuid, goConfigService.serverConfig().getTokenGenerationKey()), request);
        AgentConfig agentConfig = goConfigService.agentByUuid(uuid);
        assertThat(agentConfig.getHostname(), is("hostname"));
    }

    @Test
    public void shouldNotRegisterAgentWhenValidationFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        int totalAgentsBeforeRegistrationRequest = goConfigService.agents().size();
        controller.agentRequest("hostname", "", "sandbox", "100", null, null, null, null, null, null, null, false, token("", goConfigService.serverConfig().getTokenGenerationKey()), request);
        int totalAgentsAfterRegistrationRequest = goConfigService.agents().size();
        assertThat(totalAgentsBeforeRegistrationRequest, is(totalAgentsAfterRegistrationRequest));
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