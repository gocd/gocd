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

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import java.util.*;

/**
 * Proxy Configurator for {@link ServerCall}.
 */
public class ProxyConfigurator {

    private final HttpMethod method;
    private final String httpProxyHost;
    private final int httpProxyPort;
    private final String nonProxyHosts;

    private ProxyConfigurator(final HttpMethod method, String httpProxyHost, int httpProxyPort, String nonProxyHosts) {
        this.method = method;
        this.httpProxyHost = httpProxyHost;
        this.httpProxyPort = httpProxyPort;
        this.nonProxyHosts = nonProxyHosts;
    }

    private ProxyHost getProxy() {
        final ProxyHost proxyHost = new ProxyHost(this.httpProxyHost, httpProxyPort);
        final String host;
        try {
            final URI uri = method.getURI();
            host = uri.getHost();
        } catch (URIException e) {
            throw new RuntimeException(e);
        }
        final Set<String> nonProxySet;
        if (nonProxyHosts != null) {
            final List<String> strings = Arrays.asList(nonProxyHosts.split("\\|"));
            nonProxySet = new HashSet<>(strings);
        } else {
            nonProxySet = Collections.emptySet();
        }
        for (String nonProxy : nonProxySet) {
            if (nonProxy.contains("*.") && host.endsWith(nonProxy.substring(2))) {
                return null;
            }
        }
        return nonProxySet.contains(host) ? null : proxyHost;
    }

    /**
     * Creates a {@link ProxyHost} for the given method or null
     * when the properties are not set or the host of the method
     * matches nonProxyHosts.
     *
     * @param method            - called HttpMethod
     * @param httpProxyHost     - http.proxyHost from System.properties
     * @param httpProxyPort     - http.proxyPort from System.properties
     * @param httpsProxyHost    - https.proxyHost from System.properties
     * @param httpsProxyPort    - https.proxyPort from System.properties
     * @param nonProxyHosts     - nonProxyHosts from System.properties  @return the ProxyHost to be used or null
     */
    public static ProxyHost create(final HttpMethod method, final String httpProxyHost, final String httpProxyPort, final String httpsProxyHost, final String httpsProxyPort, final String nonProxyHosts) {
        final String scheme;
        try {
            scheme = method.getURI().getScheme();
        } catch (URIException e) {
            throw new IllegalArgumentException("Method is illegal", e);
        }
        if (scheme.equals("http")) {
            return (httpProxyHost == null || httpProxyPort == null) ? null : new ProxyConfigurator(
                    method,
                    httpProxyHost, Integer.valueOf(httpProxyPort),
                    nonProxyHosts).getProxy();
        } else if (scheme.equals("https")) {
            return (httpsProxyHost == null || httpsProxyPort == null) ? null : new ProxyConfigurator(
                    method,
                    httpsProxyHost, Integer.valueOf(httpsProxyPort),
                    nonProxyHosts).getProxy();
        } else {
            return null;
        }
    }
}
