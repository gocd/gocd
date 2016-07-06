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

package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.go.agent.testhelper.FakeBootstrapperServer;
import com.thoughtworks.go.mothers.ServerUrlGeneratorMother;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(FakeBootstrapperServer.class)
public class ServerCallTest {
    @Test
    public void shouldBeAbleToReadTheResponseBody() throws Exception {
        HttpGet getMethod = new HttpGet(DownloadableFile.AGENT.url(ServerUrlGeneratorMother.generatorFor("localhost", 9090)));
        ServerCall.ServerResponseWrapper response = new ServerCall().invoke(getMethod, null, SslVerificationMode.NONE);
        List list = IOUtils.readLines(response.body);
        assertThat(list.isEmpty(), is(false));
    }

    @Test
    public void shouldThrowSpecifiCExceptionIncaseOf404() throws Exception {
        HttpGet getMethod = new HttpGet("http://localhost:9090/go/not-found");
        try {
            new ServerCall().invoke(getMethod, null, SslVerificationMode.NONE);
            fail("Was expecting an exception!");
        } catch (Exception ex) {
            assertThat(ex.getMessage().contains(
                    "This agent might be incompatible with your Go Server."
                            + "Please fix the version mismatch between Go Server and Go Agent."), is(true));
        }
    }

    @Test
    public void shouldConnectToAServerWithACertSignedByAKnownCA() throws Exception {
        ServerCall.ServerResponseWrapper invoke = new ServerCall().invoke(new HttpGet("https://example.com"), null, SslVerificationMode.FULL);
        assertThat(IOUtils.toString(invoke.body), containsString("This domain is established to be used for illustrative examples in documents"));
    }

    @Test
    public void shouldConnectToAnSSLServerWithSelfSignedCertWhenInsecureModeIsInsecure() throws Exception {
        ServerCall.ServerResponseWrapper invoke = new ServerCall().invoke(new HttpGet("https://localhost:9091/go/hello"), null, SslVerificationMode.NONE);
        assertThat(IOUtils.toString(invoke.body).trim(), equalTo("Hello"));
    }

    @Test
    public void shouldConnectToAnSSLServerWithSelfSignedCertWhenInsecureModeIsNoVerifyHost() throws Exception {
        ServerCall.ServerResponseWrapper invoke = new ServerCall().invoke(new HttpGet("https://localhost:9091/go/hello"), new File("testdata/test_cert.pem"), SslVerificationMode.NO_VERIFY_HOST);
        assertThat(IOUtils.toString(invoke.body).trim(), equalTo("Hello"));
    }

    @Test
    public void shouldRaiseExceptionWhenSelfSignedCertDoesNotMatchTheHostName() throws Exception {
        try {
            new ServerCall().invoke(new HttpGet("https://localhost:9091/go/hello"), new File("testdata/test_cert.pem"), SslVerificationMode.FULL);
            fail("Was expecting an exception!");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Host name 'localhost' does not match the certificate subject provided by the peer"));
        }
    }
}
