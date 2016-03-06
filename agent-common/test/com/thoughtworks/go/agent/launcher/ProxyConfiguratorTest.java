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
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProxyConfiguratorTest {

    final GetMethod method = new GetMethod("http://www.example.com/foo");

    @Test
    public void shouldReturnNullIfRequiredVariablesAreNotSet() {
        assertNull(ProxyConfigurator.create(method, null, null, null));
    }

    @Test
    public void shouldReturnProxyHost() {
        assertNotNull(ProxyConfigurator.create(method, "proxy.example.org", "3128", null));
    }

    @Test
    public void shouldReturnProxyHostWhenHostDoesNotMatchNonProxyHosts() {
        assertNotNull(ProxyConfigurator.create(method, "proxy.example.org", "3128", "www.example.org"));
    }

    @Test
    public void shouldReturnNullWhenHostIsInNonProxyHosts() {
        assertNull(ProxyConfigurator.create(method, "proxy.example.org", "3128", "www.example.com|example.org"));
    }

    @Test
    public void shouldReturnNullWhenHostMatchesNonProxyHosts() {
        assertNull(ProxyConfigurator.create(method, "proxy.example.org", "3128", "*.example.com|example.org"));
    }
}