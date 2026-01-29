/*
 * Copyright Thoughtworks, Inc.
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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentBootstrapperArgsTest {

    @Test
    public void shouldSerializeToPropertiesWhenCertFileIsSet() throws Exception {
        AgentBootstrapperArgs original = new AgentBootstrapperArgs().setServerUrl(URI.create("https://go.example.com/go").toURL()).setRootCertFile(new File("/path/to/certfile")).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE);
        Map<String, String> properties = original.toProperties();

        AgentBootstrapperArgs reHydrated = AgentBootstrapperArgs.fromProperties(properties);

        assertThat(reHydrated).isEqualTo(original);
    }


    @Test
    public void shouldSerializeToPropertiesWhenInsecureIsSet() throws Exception {
        AgentBootstrapperArgs original = new AgentBootstrapperArgs().setServerUrl(URI.create("https://go.example.com/go").toURL()).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE);
        Map<String, String> properties = original.toProperties();

        AgentBootstrapperArgs reHydrated = AgentBootstrapperArgs.fromProperties(properties);

        assertThat(reHydrated).isEqualTo(original);
    }

}
