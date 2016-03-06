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
    private final String proxyHost;
    private final int proxyPort;
    private final String nonProxyHosts;

    private ProxyConfigurator(final HttpMethod method, String proxyHost, int proxyPort, String nonProxyHosts) {
        this.method = method;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.nonProxyHosts = nonProxyHosts;
    }

    private ProxyHost getProxy() {
        final ProxyHost proxyHost = new ProxyHost(this.proxyHost, proxyPort);
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
     * @param method        - called HttpMethod
     * @param proxyHost     - proxyHost from System.properties
     * @param proxyPort     - proxyPort from System.properties
     * @param nonProxyHosts - nonProxyHosts from System.properties
     * @return the ProxyHost to be used or null
     */
    public static ProxyHost create(final HttpMethod method, String proxyHost, String proxyPort, String nonProxyHosts) {
        if (method == null || proxyHost == null || proxyPort == null) {
            return null;
        } else {
            final ProxyConfigurator proxyConfigurator = new ProxyConfigurator(
                    method, proxyHost, Integer.valueOf(proxyPort), nonProxyHosts);
            return proxyConfigurator.getProxy();
        }
    }
}
