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

package com.thoughtworks.go.agent.common;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class AgentBootstrapperArgsTest {

    @Test
    public void shouldSerializeToPropertiesWhenCertFileIsSet() throws Exception {
        AgentBootstrapperArgs original = new AgentBootstrapperArgs(new URL("https://go.example.com:8154/go"), new File("/path/to/certfile"), AgentBootstrapperArgs.SslMode.NONE);
        Properties properties = original.toProperties();

        AgentBootstrapperArgs reHydrated = AgentBootstrapperArgs.fromProperties(properties);

        assertEquals(original, reHydrated);
    }


    @Test
    public void shouldSerializeToPropertiesWhenInsecureIsSet() throws Exception {
        AgentBootstrapperArgs original = new AgentBootstrapperArgs(new URL("https://go.example.com:8154/go"), null, AgentBootstrapperArgs.SslMode.NONE);
        Properties properties = original.toProperties();

        AgentBootstrapperArgs reHydrated = AgentBootstrapperArgs.fromProperties(properties);

        assertEquals(original, reHydrated);
    }


}
