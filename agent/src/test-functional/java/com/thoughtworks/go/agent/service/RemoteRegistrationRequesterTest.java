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
package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.AgentAutoRegistrationPropertiesImpl;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.agent.testhelpers.AgentCertificateMother;
import com.thoughtworks.go.config.DefaultAgentRegistry;
import com.thoughtworks.go.config.TokenService;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.security.RegistrationJSONizer;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class RemoteRegistrationRequesterTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        tokenService.store("token-from-server");
    }

    @AfterEach
    void tearDown() {
        tokenService.delete();
    }

    @Test
    void shouldPassAllParametersToPostForRegistrationOfNonElasticAgent() throws IOException, ClassNotFoundException {
        String url = "http://cruise.com/go";
        GoAgentServerHttpClient httpClient = mock(GoAgentServerHttpClient.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final ProtocolVersion protocolVersion = new ProtocolVersion("https", 1, 2);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(protocolVersion, HttpStatus.OK.value(), null));
        when(response.getEntity()).thenReturn(new StringEntity(RegistrationJSONizer.toJson(createRegistration())));
        when(httpClient.execute(isA(HttpRequestBase.class))).thenReturn(response);
        final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
        Properties properties = new Properties();
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");

        remoteRegistryRequester(url, httpClient, defaultAgentRegistry, 200).requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
        verify(httpClient).execute(argThat(hasAllParams(defaultAgentRegistry.uuid(), "", "")));
    }

    @Test
    void shouldPassAllParametersToPostForRegistrationOfElasticAgent() throws IOException, ClassNotFoundException {
        String url = "http://cruise.com/go";
        GoAgentServerHttpClient httpClient = mock(GoAgentServerHttpClient.class);
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final ProtocolVersion protocolVersion = new ProtocolVersion("https", 1, 2);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(protocolVersion, HttpStatus.OK.value(), null));
        when(response.getEntity()).thenReturn(new StringEntity(RegistrationJSONizer.toJson(createRegistration())));
        when(httpClient.execute(isA(HttpRequestBase.class))).thenReturn(response);

        final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
        Properties properties = new Properties();
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID, "42");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID, "tw.go.elastic-agent.docker");

        remoteRegistryRequester(url, httpClient, defaultAgentRegistry, 200).requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
        verify(httpClient).execute(argThat(hasAllParams(defaultAgentRegistry.uuid(), "42", "tw.go.elastic-agent.docker")));
    }

    private ArgumentMatcher<HttpRequestBase> hasAllParams(final String uuid, final String elasticAgentId, final String elasticPluginId) {
        return new ArgumentMatcher<HttpRequestBase>() {
            @Override
            public boolean matches(HttpRequestBase item) {
                try {
                    HttpEntityEnclosingRequestBase postMethod = (HttpEntityEnclosingRequestBase) item;
                    List<NameValuePair> params = URLEncodedUtils.parse(postMethod.getEntity());

                    assertThat(getParameter(params, "hostname"), is("cruise.com"));
                    assertThat(getParameter(params, "uuid"), is(uuid));
                    String workingDir = SystemUtil.currentWorkingDirectory();
                    assertThat(getParameter(params, "location"), is(workingDir));
                    assertThat(getParameter(params, "operatingSystem"), not(nullValue()));
                    assertThat(getParameter(params, "agentAutoRegisterKey"), is("t0ps3cret"));
                    assertThat(getParameter(params, "agentAutoRegisterResources"), is("linux, java"));
                    assertThat(getParameter(params, "agentAutoRegisterEnvironments"), is("uat, staging"));
                    assertThat(getParameter(params, "agentAutoRegisterHostname"), is("agent01.example.com"));
                    assertThat(getParameter(params, "elasticAgentId"), is(elasticAgentId));
                    assertThat(getParameter(params, "elasticPluginId"), is(elasticPluginId));
                    assertThat(getParameter(params, "token"), is("token-from-server"));
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            private String getParameter(List<NameValuePair> params, String paramName) {
                for (NameValuePair param : params) {
                    if (param.getName().equals(paramName)) {
                        return param.getValue();
                    }
                }
                return null;
            }
        };
    }

    private SslInfrastructureService.RemoteRegistrationRequester remoteRegistryRequester(final String url, final GoAgentServerHttpClient httpClient, final DefaultAgentRegistry defaultAgentRegistry, final int statusCode) {
        return new SslInfrastructureService.RemoteRegistrationRequester(url, defaultAgentRegistry, httpClient) {
            @Override
            protected int getStatusCode(CloseableHttpResponse response) {
                return statusCode;
            }

            protected Registration readResponse(String responseBody) {
                return null;
            }
        };
    }

    private Registration createRegistration() throws IOException {
        Registration certificates = AgentCertificateMother.agentCertificate(temporaryFolder);
        return new Registration(certificates.getPrivateKey(), certificates.getChain());
    }
}
