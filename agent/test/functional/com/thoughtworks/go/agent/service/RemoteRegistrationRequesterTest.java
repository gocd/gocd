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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.agent.AgentAutoRegistrationPropertiesImpl;
import com.thoughtworks.go.config.DefaultAgentRegistry;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

public class RemoteRegistrationRequesterTest {
    private String original;

    @Before
    public void before() {
        original = System.getProperty("os.name");
        System.setProperty("os.name", "minix");
    }

    @After
    public void after() {
        System.setProperty("os.name", original);
    }

    @Test
    public void shouldPassAllParametersToPostForRegistrationOfNonElasticAgent() throws IOException, ClassNotFoundException {
        String url = "http://cruise.com/go";
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
        Properties properties = new Properties();
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");

        remoteRegistryRequester(url, httpClient, defaultAgentRegistry).requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
        verify(httpClient).executeMethod(argThat(hasAllParams(defaultAgentRegistry.uuid(), "", "")));
    }

    @Test
    public void shouldPassAllParametersToPostForRegistrationOfElasticAgent() throws IOException, ClassNotFoundException {
        String url = "http://cruise.com/go";
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
        Properties properties = new Properties();
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID, "42");
        properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID, "tw.go.elastic-agent.docker");

        remoteRegistryRequester(url, httpClient, defaultAgentRegistry).requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
        verify(httpClient).executeMethod(argThat(hasAllParams(defaultAgentRegistry.uuid(), "42", "tw.go.elastic-agent.docker")));
    }

    private TypeSafeMatcher<HttpMethod> hasAllParams(final String uuid, final String elasticAgentId, final String elasticPluginId) {
        return new TypeSafeMatcher<HttpMethod>() {
            @Override
            public boolean matchesSafely(HttpMethod item) {
                PostMethod postMethod = (PostMethod) item;
                assertThat(postMethod.getParameter("hostname").getValue(), is("cruise.com"));
                assertThat(postMethod.getParameter("uuid").getValue(), is(uuid));
                String workingDir = SystemUtil.currentWorkingDirectory();
                assertThat(postMethod.getParameter("location").getValue(), is(workingDir));
                assertThat(postMethod.getParameter("operatingSystem").getValue(), is("minix"));
                assertThat(postMethod.getParameter("agentAutoRegisterKey").getValue(), is("t0ps3cret"));
                assertThat(postMethod.getParameter("agentAutoRegisterResources").getValue(), is("linux, java"));
                assertThat(postMethod.getParameter("agentAutoRegisterEnvironments").getValue(), is("uat, staging"));
                assertThat(postMethod.getParameter("agentAutoRegisterHostname").getValue(), is("agent01.example.com"));
                assertThat(postMethod.getParameter("elasticAgentId").getValue(), is(elasticAgentId));
                assertThat(postMethod.getParameter("elasticPluginId").getValue(), is(elasticPluginId));
                return true;
            }

            public void describeTo(Description description) {
                description.appendText("params containing");
            }
        };
    }

    private SslInfrastructureService.RemoteRegistrationRequester remoteRegistryRequester(final String url, final HttpClient httpClient, final DefaultAgentRegistry defaultAgentRegistry) {
        return new SslInfrastructureService.RemoteRegistrationRequester(url, defaultAgentRegistry, httpClient) {
            @Override
            protected Registration readResponse(InputStream is) throws IOException, ClassNotFoundException {
                return null;
            }
        };
    }


}
