/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.service.ServerConfigService;
import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeploymentContextWriterTest {
    private String originalSslPort;

    @Before
    public void setUp() {
        originalSslPort = System.getProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT);
        System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, "5050");
        ServletHelper.init();
    }

    @After
    public void tearDown() {
        if (originalSslPort != null) {
            System.setProperty(SystemEnvironment.CRUISE_SERVER_SSL_PORT, originalSslPort);
        }
    }

    @Test
    public void shouldSetSslPortAsRequestAttribute() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertThat(req.getAttributeNames().hasMoreElements(), is(false));
        DeploymentContextWriter writer = new DeploymentContextWriter();
        writer.writeSslPort(req);
        assertThat((Integer) req.getAttribute("ssl_port"), is(5050));
    }

    @Test
    public void shouldSetSecureSiteURLWhenSiteUrlIsConfigured() throws URISyntaxException {
        final ServerConfigService serverConfigService = mock(ServerConfigService.class);
        when(serverConfigService.hasAnyUrlConfigured()).thenReturn(true);
        when(serverConfigService.siteUrlFor("http://url/go/admin?tab=oAuth", true)).thenReturn("https://url/go/admin?tab=oAuth");

        Request request = new Request(mock(HttpChannel.class), mock(HttpInput.class));
        request.setUri(new HttpURI("/go/admin?tab=oAuth"));
        request.setServerName("url");

        DeploymentContextWriter writer = new DeploymentContextWriter() {
            @Override protected BaseUrlProvider getBaseUrlProvider(HttpServletRequest req) {
                return serverConfigService;
            }
        };
        writer.writeSecureSiteUrl(request);
        assertThat((String) request.getAttribute("secure_site"), is("https://url/go/admin?tab=oAuth"));
        assertThat((String) request.getAttribute("force_ssl"), is("true"));
    }

    @Test
    public void shouldSkipRedirectWhenSiteUrlIsNotConfigured() throws URISyntaxException {
        final ServerConfigService serverConfigService = mock(ServerConfigService.class);
        when(serverConfigService.hasAnyUrlConfigured()).thenReturn(false);

        Request req = new Request(mock(HttpChannel.class), mock(HttpInput.class));
        req.setUri(new HttpURI("/go/admin?tab=oAuth"));
        req.setServerName("url");
        req.setServerPort(8153);
        //req.setProtocol("http");

        DeploymentContextWriter writer = new DeploymentContextWriter() {
            @Override protected BaseUrlProvider getBaseUrlProvider(HttpServletRequest req) {
                return serverConfigService;
            }
        };
        writer.writeSecureSiteUrl(req);
        assertThat((String) req.getAttribute("secure_site"), is(nullValue()));
        assertThat((String) req.getAttribute("force_ssl"), is(nullValue()));
    }

    @Test
    public void shouldSetShouldRedirectToFalseWhenSecureSiteURLIsNotSetAndSiteUrlIsNonHTTPS() throws URISyntaxException {
        final ServerConfigService serverConfigService = mock(ServerConfigService.class);
        when(serverConfigService.hasAnyUrlConfigured()).thenReturn(true);
        when(serverConfigService.siteUrlFor("http://url:8153/go/admin?tab=oAuth", true)).thenReturn("http://url:8153/go/admin?tab=oAuth");

        Request req = new Request(mock(HttpChannel.class), mock(HttpInput.class));
        req.setUri(new HttpURI("/go/admin?tab=oAuth"));
        req.setServerName("url");
        req.setServerPort(8153);
        //req.setProtocol("http");

        DeploymentContextWriter writer = new DeploymentContextWriter() {
            @Override protected BaseUrlProvider getBaseUrlProvider(HttpServletRequest req) {
                return serverConfigService;
            }
        };
        writer.writeSecureSiteUrl(req);
        assertThat((String) req.getAttribute("secure_site"), is(nullValue()));
        assertThat((String) req.getAttribute("force_ssl"), is(nullValue()));
    }
}
