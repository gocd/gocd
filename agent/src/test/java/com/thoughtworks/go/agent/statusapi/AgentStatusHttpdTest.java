/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.statusapi;

import com.sun.net.httpserver.HttpServer;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentStatusHttpdTest {

    @Mock
    private AgentHealthHolder agentHealthHolder;
    @Mock
    private SystemEnvironment systemEnvironment;

    private AgentStatusHttpd agentStatusHttpd;

    private String hostName = "localhost";
    private int port = 8093;

    @BeforeEach
    void setUp() {
        this.agentStatusHttpd = new AgentStatusHttpd(systemEnvironment, new IsConnectedToServerV1(agentHealthHolder));
    }

    @AfterEach
    void tearDown() {
        this.agentStatusHttpd.destroy();
        reset(systemEnvironment);
    }

    @Test
    void shouldReturnMethodNotAllowedOnNonGetNonHeadRequests() {
        setupAgentStatusParameters();
        startAgentStatusEndpointServer();
        processHttpRequest(createPostRequest("/health/latest/isConnectedToServer"), (response, httpEntity) -> {
            assertThat(statusCode(response)).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);
            assertThat(contentType(httpEntity)).isEqualTo("text/plain; charset=utf-8");
            assertThat(responseBody(httpEntity)).isEqualTo("This method is not allowed. Please use GET or HEAD.");
        });
    }

    @Test
    void shouldReturnNotFoundForBadUrl() {
        setupAgentStatusParameters();
        startAgentStatusEndpointServer();
        processHttpRequest(createGetRequest("/foo"), (response, httpEntity) -> {
            assertThat(statusCode(response)).isEqualTo(HttpStatus.SC_NOT_FOUND);
            assertThat(contentType(httpEntity)).isEqualTo("text/plain; charset=utf-8");
            assertThat(responseBody(httpEntity)).isEqualTo("The page you requested was not found");
        });
    }

    @Test
    void shouldRouteToIsConnectedToServerHandler() {
        setupAgentStatusParameters();
        startAgentStatusEndpointServer();
        processHttpRequest(createGetRequest("/health/latest/isConnectedToServer"), (response, httpEntity) -> {
            assertThat(statusCode(response)).isEqualTo(HttpStatus.SC_OK);
            assertThat(contentType(httpEntity)).isEqualTo("text/plain; charset=utf-8");
            assertThat(responseBody(httpEntity)).isEqualTo("OK!");
        });
    }


    @Test
    void shouldRouteToIsConnectedToServerV1Handler() {
        when(agentHealthHolder.hasLostContact()).thenReturn(false);
        setupAgentStatusParameters();
        startAgentStatusEndpointServer();

        processHttpRequest(createGetRequest("/health/v1/isConnectedToServer"), (response, httpEntity) -> {
            assertThat(statusCode(response)).isEqualTo(HttpStatus.SC_OK);
            assertThat(contentType(httpEntity)).isEqualTo("text/plain; charset=utf-8");
            assertThat(responseBody(httpEntity)).isEqualTo("OK!");
        });
    }

    @Test
    void shouldNotInitializeServerIfSettingIsTurnedOff() {
        try (MockedStatic<HttpServer> mockedStaticHttpServer = mockStatic(HttpServer.class)) {
            when(systemEnvironment.getAgentStatusEnabled()).thenReturn(false);
            agentStatusHttpd.init();
            mockedStaticHttpServer.verifyNoInteractions();
        }
    }

    @Test
    void shouldInitializeServerIfSettingIsTurnedOn() {
        try (MockedStatic<HttpServer> mockedStaticHttpServer = mockStatic(HttpServer.class)) {
            var serverSocket = setupAgentStatusParameters();
            var spiedHttpServer = spy(HttpServer.class);
            when(spiedHttpServer.getAddress()).thenReturn(serverSocket);

            mockedStaticHttpServer.when(() -> HttpServer.create(eq(serverSocket), anyInt())).thenReturn(spiedHttpServer);

            agentStatusHttpd.init();

            verify(spiedHttpServer).start();
        }
    }

    @Test
    void initShouldNotBlowUpIfServerDoesNotStart() {

        try (MockedStatic<HttpServer> mockedStaticHttpServer = mockStatic(HttpServer.class)) {
            setupAgentStatusParameters();

            var serverSocket = new InetSocketAddress(hostName, port);
            var spiedHttpServer = spy(HttpServer.class);
            doThrow(new RuntimeException("Server had a problem starting up!")).when(spiedHttpServer).start();

            mockedStaticHttpServer.when(() -> HttpServer.create(eq(serverSocket), anyInt())).thenReturn(spiedHttpServer);

            assertThatNoException().isThrownBy(() -> agentStatusHttpd.init());
        }
    }

    private InetSocketAddress setupAgentStatusParameters() {
        when(systemEnvironment.getAgentStatusEnabled()).thenReturn(true);
        when(systemEnvironment.getAgentStatusHostname()).thenReturn(hostName);
        when(systemEnvironment.getAgentStatusPort()).thenReturn(port);

        return new InetSocketAddress(hostName, port);
    }

    private void startAgentStatusEndpointServer() {
        try {
            this.agentStatusHttpd.init();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start the server for the test as a pre-requisite", e);
        }
    }

    private static String responseBody(HttpEntity httpEntity) throws IOException {
        return EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
    }

    private static String contentType(HttpEntity httpEntity) {
        return httpEntity.getContentType().getValue();
    }

    private static int statusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    private HttpUriRequest createGetRequest(String uri) {
        return new HttpGet(agentStatusUrl(uri));
    }

    private HttpUriRequest createPostRequest(String uri) {
        return new HttpPost(agentStatusUrl(uri));
    }

    private String agentStatusUrl(String path) {
        return String.format("http://%s:%d/%s", hostName, port, path);
    }

    private void processHttpRequest(HttpUriRequest httpRequest, HttpResponseConsumer consumer) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault(); CloseableHttpResponse response = httpclient.execute(httpRequest)) {
            HttpEntity entity = response.getEntity();
            consumer.accept(response, entity);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve the http response", e);
        }
    }

    private interface HttpResponseConsumer {
        void accept(HttpResponse response, HttpEntity httpEntity) throws Exception;
    }
}
