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
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClient;
import com.thoughtworks.go.agent.common.ssl.GoAgentServerHttpClientBuilder;
import com.thoughtworks.go.config.DefaultAgentRegistry;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class RemoteRegistrationRequesterTest {

    @Test
    public void shouldPassAllParametersToPostForRegistrationOfNonElasticAgent() throws Exception {
        String url = "http://cruise.com/go";
        try (GoAgentServerHttpClient httpClient = spy(new GoAgentServerHttpClient(new GoAgentServerHttpClientBuilder(new SystemEnvironment())))) {
            httpClient.init();
            ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
            doReturn(mock(ContentResponse.class)).when(httpClient).execute(argumentCaptor.capture());

            final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
            Properties properties = new Properties();
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");

            SslInfrastructureService.RemoteRegistrationRequester remoteRegistryRequester = remoteRegistryRequester(url, httpClient, defaultAgentRegistry, 200);

            remoteRegistryRequester.requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
            Request value = argumentCaptor.getValue();
            assertThat(value, hasAllParams(defaultAgentRegistry.uuid(), "", ""));
        }
    }

    @Test
    public void shouldPassAllParametersToPostForRegistrationOfElasticAgent() throws Exception {
        String url = "http://cruise.com/go";
        try (GoAgentServerHttpClient httpClient = spy(new GoAgentServerHttpClient(new GoAgentServerHttpClientBuilder(new SystemEnvironment())))) {
            httpClient.init();
            ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
            doReturn(mock(ContentResponse.class)).when(httpClient).execute(argumentCaptor.capture());

            final DefaultAgentRegistry defaultAgentRegistry = new DefaultAgentRegistry();
            Properties properties = new Properties();
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_KEY, "t0ps3cret");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_RESOURCES, "linux, java");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ENVIRONMENTS, "uat, staging");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID, "42");
            properties.put(AgentAutoRegistrationPropertiesImpl.AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID, "tw.go.elastic-agent.docker");

            SslInfrastructureService.RemoteRegistrationRequester registrationRequester = remoteRegistryRequester(url, httpClient, defaultAgentRegistry, 200);
            registrationRequester.requestRegistration("cruise.com", new AgentAutoRegistrationPropertiesImpl(null, properties));
            Request value = argumentCaptor.getValue();
            assertThat(value, hasAllParams(defaultAgentRegistry.uuid(), "42", "tw.go.elastic-agent.docker"));
        }
    }

    private TypeSafeMatcher<Request> hasAllParams(final String uuid, final String elasticAgentId, final String elasticPluginId) {
        return new TypeSafeMatcher<Request>() {
            @Override
            public boolean matchesSafely(Request request) {
                try {
                    ContentProvider content = request.getContent();

                    MultiMap<String> params = toMap(content);

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
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            private String getParameter(MultiMap<String> params, String paramName) {
                return params.getString(paramName);
            }

            public void describeTo(Description description) {
                description.appendText("params containing");
            }
        };
    }

    private SslInfrastructureService.RemoteRegistrationRequester remoteRegistryRequester(final String url, final GoAgentServerHttpClient httpClient, final DefaultAgentRegistry defaultAgentRegistry, final int statusCode) {
        return new SslInfrastructureService.RemoteRegistrationRequester(url, defaultAgentRegistry, httpClient) {
            @Override
            protected int getStatusCode(Response response) {
                return statusCode;
            }

            @Override
            protected Registration readResponse(String responseBody) {
                return null;
            }
        };
    }


    private MultiMap<String> toMap(ContentProvider actual) throws IOException {
        String content = contentProviderToString(actual);

        MultiMap<String> map = new MultiMap<>();
        UrlEncoded.decodeTo(content, map, UTF_8, 9999);
        return map;
    }

    private static String contentProviderToString(ContentProvider actual) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        for (ByteBuffer byteBuffer : actual) {
            byteArrayOutputStream.write(byteBuffer.array());
        }

        return byteArrayOutputStream.toString("utf-8");
    }


}
