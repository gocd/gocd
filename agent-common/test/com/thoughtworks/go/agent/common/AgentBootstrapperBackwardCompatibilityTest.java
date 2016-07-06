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

import com.thoughtworks.go.util.SslVerificationMode;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AgentBootstrapperBackwardCompatibilityTest {

    @Test
    public void shouldBeBackwardCompatible() throws Exception {
        HashMap context = new HashMap();
        context.put("hostname", "ci.example.com");
        context.put("port", "8153");
        AgentBootstrapperBackwardCompatibility compatibility = new AgentBootstrapperBackwardCompatibility(context);

        assertNull(compatibility.rootCertFileAsString());
        assertNull(compatibility.rootCertFile());
        assertEquals(SslVerificationMode.NONE, compatibility.sslVerificationMode());
        assertEquals("https://ci.example.com:8154/go", compatibility.sslServerUrl("8154"));
    }

    @Test
    public void shouldReturnCLIArgsIfStuffedInContext() throws Exception {
        AgentBootstrapperArgs args = new AgentBootstrapperArgs(new URL("https://go.example.com:8154/go"), new File("/path/to/certfile"), AgentBootstrapperArgs.SslMode.NO_VERIFY_HOST);
        AgentBootstrapperBackwardCompatibility compatibility = new AgentBootstrapperBackwardCompatibility(args.toProperties());

        assertEquals("/path/to/certfile", compatibility.rootCertFileAsString());
        assertEquals(new File("/path/to/certfile"), compatibility.rootCertFile());
        assertEquals(SslVerificationMode.NO_VERIFY_HOST, compatibility.sslVerificationMode());
        assertEquals("https://go.example.com:8154/go", compatibility.sslServerUrl("8154"));
    }
}
