/*************************GO-LICENSE-START*********************************
 * Copyright 2016 Mirko Friedenhagen.
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

package com.thoughtworks.go.agent.launcher;

import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ProxyConfiguratorTest {

    private static final String HTTP_PROXY_HOST = "httpproxy.example.org";
    private static final String HTTP_PROXY_PORT = "3128";
    private static final String HTTPS_PROXY_HOST = "httpsproxy.example.org";
    private static final String HTTPS_PROXY_PORT = "3129";
    private static final GetMethod HTTP_METHOD = new GetMethod("http://www.example.com/foo");
    private static final GetMethod HTTPS_METHOD = new GetMethod("https://www.example.com/foo");
    private static final String NON_PROXY_HOSTS = "www.example.com|example.org";

    @Test
    public void shouldReturnNullIfRequiredVariablesAreNotSet() {
        assertNull(ProxyConfigurator.create(HTTP_METHOD, new Properties()));
    }

    @Test
    public void shouldReturnProxyHost() throws IOException {
        final Properties proxyProperties = readProxyProperties();
        proxyProperties.remove("http.nonProxyHosts");
        final ProxyHost httpProxy = ProxyConfigurator.create(HTTP_METHOD, proxyProperties);
        assertThat(httpProxy.getHostName(), is(HTTP_PROXY_HOST));
        assertThat(httpProxy.getPort(), is(Integer.valueOf(HTTP_PROXY_PORT)));
        final ProxyHost httpsProxy = ProxyConfigurator.create(HTTPS_METHOD, proxyProperties);
        assertThat(httpsProxy.getHostName(), is(HTTPS_PROXY_HOST));
        assertThat(httpsProxy.getPort(), is(Integer.valueOf(HTTPS_PROXY_PORT)));
    }

    @Test
    public void shouldReturnProxyHostWhenHostDoesNotMatchNonProxyHosts() throws IOException {
        final Properties proxyProperties = readProxyProperties();
        proxyProperties.setProperty("http.nonProxyHosts", "www.example.org");
        final ProxyHost httpProxy = ProxyConfigurator.create(HTTP_METHOD, proxyProperties);
        assertThat(httpProxy.getHostName(), is(HTTP_PROXY_HOST));
        assertThat(httpProxy.getPort(), is(Integer.valueOf(HTTP_PROXY_PORT)));
    }

    @Test
    public void shouldReturnNullWhenHostIsInNonProxyHosts() throws IOException {
        final Properties proxyProperties = readProxyProperties();
        proxyProperties.setProperty("http.nonProxyHosts", NON_PROXY_HOSTS);
        assertNull(ProxyConfigurator.create(HTTP_METHOD, proxyProperties));
        assertNull(ProxyConfigurator.create(HTTPS_METHOD, proxyProperties));
    }

    @Test
    public void shouldReturnNullWhenHostMatchesNonProxyHosts() throws IOException {
        final Properties proxyProperties = readProxyProperties();
        assertNull(ProxyConfigurator.create(HTTP_METHOD, proxyProperties));
        assertNull(ProxyConfigurator.create(HTTPS_METHOD, proxyProperties));
    }

    private Properties readProxyProperties() throws IOException {
        final Properties properties = new Properties();
        try (final InputStream stream = ProxyConfiguratorTest.class.getResourceAsStream("proxy.properties")) {
            properties.load(stream);
        }
        return properties;
    }
}