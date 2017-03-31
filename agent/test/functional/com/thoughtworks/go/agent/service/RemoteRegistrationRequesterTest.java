/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.DefaultAgentRegistry;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class RemoteRegistrationRequesterTest {
    public static final File EXPECTED_WORKING_DIR = new File("some-working-dir");
    @Mock
    private GoAgentServerHttpClient httpClient;
    @Mock
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(httpClient.execute(argThat(isA(HttpUriRequest.class)))).thenReturn(mock(CloseableHttpResponse.class));
        when(systemEnvironment.resolveAgentWorkingDirectory()).thenReturn(EXPECTED_WORKING_DIR);
    }

    @Test
    public void shouldPassAllParametersToPostForRegistrationOfNonElasticAgent() throws IOException, ClassNotFoundException {
        String url = "http://cruise.com/go";

        final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
        Properties properties = new Properties();
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");

        remoteRegistryRequester(url, httpClient, defaultAgentRegistry, 200, systemEnvironment).requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
        verify(httpClient).execute(argThat(hasAllParams(defaultAgentRegistry.uuid(), "", "")));
    }

    @Test
    public void shouldPassAllParametersToPostForRegistrationOfElasticAgent() throws IOException, ClassNotFoundException {
        String url = "http://cruise.com/go";

        final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
        Properties properties = new Properties();
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID, "42");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID, "tw.go.elastic-agent.docker");

        remoteRegistryRequester(url, httpClient, defaultAgentRegistry, 200, systemEnvironment).requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
        verify(httpClient).execute(argThat(hasAllParams(defaultAgentRegistry.uuid(), "42", "tw.go.elastic-agent.docker")));
    }

    private TypeSafeMatcher<HttpRequestBase> hasAllParams(final String uuid, final String elasticAgentId, final String elasticPluginId) {
        return new TypeSafeMatcher<HttpRequestBase>() {
            @Override
            public boolean matchesSafely(HttpRequestBase item) {
                try {
                    HttpEntityEnclosingRequestBase postMethod = (HttpEntityEnclosingRequestBase) item;
                    List<NameValuePair> params = URLEncodedUtils.parse(postMethod.getEntity());

                    assertThat(getParameter(params, "hostname"), is("cruise.com"));
                    assertThat(getParameter(params, "uuid"), is(uuid));
                    assertThat(getParameter(params, "location"), is(EXPECTED_WORKING_DIR.getCanonicalPath()));
                    assertThat(getParameter(params, "operatingSystem"), not(nullValue()));
                    assertThat(getParameter(params, "agentAutoRegisterKey"), is("t0ps3cret"));
                    assertThat(getParameter(params, "agentAutoRegisterResources"), is("linux, java"));
                    assertThat(getParameter(params, "agentAutoRegisterEnvironments"), is("uat, staging"));
                    assertThat(getParameter(params, "agentAutoRegisterHostname"), is("agent01.example.com"));
                    assertThat(getParameter(params, "elasticAgentId"), is(elasticAgentId));
                    assertThat(getParameter(params, "elasticPluginId"), is(elasticPluginId));
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

            public void describeTo(Description description) {
                description.appendText("params containing");
            }
        };
    }

    private SslInfrastructureService.RemoteRegistrationRequester remoteRegistryRequester(final String url, final GoAgentServerHttpClient httpClient, final DefaultAgentRegistry defaultAgentRegistry, final int statusCode, SystemEnvironment systemEnvironment) {
        return new SslInfrastructureService.RemoteRegistrationRequester(url, defaultAgentRegistry, httpClient, systemEnvironment) {
            @Override
            protected int getStatusCode(CloseableHttpResponse response) {
                return statusCode;
            }

            @Override
            protected Registration readResponse(String responseBody) {
                return null;
            }
        };
    }


}
