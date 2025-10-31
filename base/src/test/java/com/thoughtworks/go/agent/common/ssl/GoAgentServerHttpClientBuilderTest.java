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

package com.thoughtworks.go.agent.common.ssl;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.testhelper.FakeGoServer;
import com.thoughtworks.go.agent.testhelper.FakeGoServerExtension;
import com.thoughtworks.go.agent.testhelper.GoTestResource;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static com.thoughtworks.go.mothers.ServerUrlGeneratorMother.generatorFor;
import static com.thoughtworks.go.util.TestFileUtil.resourceToTempFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("resource")
@ExtendWith(FakeGoServerExtension.class)
class GoAgentServerHttpClientBuilderTest {

    @GoTestResource
    public FakeGoServer server;

    @Test
    public void shouldMakeNormalHttpRequest() throws Exception {
        GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null);
        try (CloseableHttpResponse response = requestFor(builder, generatorFor("localhost", server.getPort()))) {
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        }
    }

    @Test
    public void shouldThrowExceptionIfTheServerIsDown() {
        GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null);
        assertThatThrownBy(() -> requestFor(builder, generatorFor("badhost", server.getPort())))
            .isExactlyInstanceOf(UnknownHostException.class);
    }

    @Nested
    class ServerCertVerification {
        @Test
        public void shouldConnectToAnSslServerWithSelfSignedCertIfNotVerifying() throws Exception {
            GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(null, SslVerificationMode.NONE, null, null, null);
            assertSuccessfulHttpsRequestFor(builder);
        }

        @ParameterizedTest
        @EnumSource(value = SslVerificationMode.class, names = {"NO_VERIFY_HOST", "FULL"})
        public void shouldConnectToAnSslServerWithSelfSignedCertWhenInsecureModeIsNoVerifyHost(SslVerificationMode mode) throws Exception {
            GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(resourceToTempFile("/testdata/root-ca-ec.crt"), mode, null, null, null);
            assertSuccessfulHttpsRequestFor(builder);
        }

        @ParameterizedTest
        @EnumSource(value = SslVerificationMode.class, names = {"NONE", "NO_VERIFY_HOST"})
        public void shouldConnectToAnSslServerWithMismatchedHostNameIfNotVerifyingHostname(SslVerificationMode mode) throws Exception {
            GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(resourceToTempFile("/testdata/root-ca-ec.crt"), mode, null, null, null);
            assertSuccessfulHttpsRequestFor(builder, generatorFor("https://127.0.0.1:" + server.getSecurePort() + "/go/"));
        }

        @Test
        public void shouldRaiseExceptionWhenSelfSignedCertDoesNotMatchTheHostName() throws Exception {
            GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(resourceToTempFile("/testdata/root-ca-ec.crt"), SslVerificationMode.FULL, null, null, null);
            assertThatThrownBy(() -> requestFor(builder, generatorFor("https://127.0.0.1:" + server.getSecurePort() + "/go/")))
                .isInstanceOf(IOException.class)
                .hasMessage("Certificate for <127.0.0.1> doesn't match any of the subject alternative names: [localhost]");
        }
    }


    @Nested
    class AgentCertMtlsVerification {
        @ParameterizedTest
        @CsvSource({"ec, pk1", "ec, pk8", "rsa, pk1", "rsa, pk8"})
        public void shouldBeAbleToConnectWithAgentCertConfiguresEvenIfOptionalOnServer(String keyType, String keyFormat) throws Exception {
            GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(resourceToTempFile("/testdata/root-ca-ec.crt"), SslVerificationMode.FULL, resourceToTempFile(String.format("/testdata/agent-client-cert-%s.crt", keyType)), resourceToTempFile(String.format("/testdata/agent-client-cert-%s-key.%s", keyType, keyFormat)), resourceToTempFile("/testdata/agent-client-cert.pass"));
            // Connect via the normal TLS port where MTLS is optional
            assertSuccessfulHttpsRequestFor(builder);
        }

        @ParameterizedTest
        @CsvSource({"ec, pk1", "ec, pk8", "rsa, pk1", "rsa, pk8"})
        public void shouldBeAbleToConnectWithAgentCertOnMtlsPort(String keyType, String keyFormat) throws Exception {
            GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(resourceToTempFile("/testdata/root-ca-ec.crt"), SslVerificationMode.FULL, resourceToTempFile(String.format("/testdata/agent-client-cert-%s.crt", keyType)), resourceToTempFile(String.format("/testdata/agent-client-cert-%s-key.%s", keyType, keyFormat)), resourceToTempFile("/testdata/agent-client-cert.pass"));
            assertSuccessfulHttpsRequestFor(builder, mtlsUrlGenerator());
        }

        @Test
        public void shouldRaiseExceptionWhenNoAgentCertPresentedOnMtlsPort() throws Exception {
            GoAgentServerHttpClientBuilder builder = new GoAgentServerHttpClientBuilder(resourceToTempFile("/testdata/root-ca-ec.crt"), SslVerificationMode.FULL, null, null, null);
            assertThatThrownBy(() -> requestFor(builder, mtlsUrlGenerator()))
                .satisfiesAnyOf(
                    t -> assertThat(t).isInstanceOf(SSLHandshakeException.class)
                        .hasMessageContaining("Received fatal alert")
                        .hasMessageContaining("certificate"),
                    v -> assertThat(v).isInstanceOf(SocketException.class) // Sometimes the server will forcibly close the connection before client reads the fatal alert
                );
        }

        private ServerUrlGenerator mtlsUrlGenerator() {
            return generatorFor("https://localhost:" + server.getSecureMtlsRequiredPort() + "/go/");
        }

    }

    private void assertSuccessfulHttpsRequestFor(GoAgentServerHttpClientBuilder builder) throws Exception {
        try (CloseableHttpResponse response = httpsRequestFor(builder)) {
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        }
    }

    private void assertSuccessfulHttpsRequestFor(GoAgentServerHttpClientBuilder builder, ServerUrlGenerator urlGen) throws Exception {
        try (CloseableHttpResponse response = requestFor(builder, urlGen)) {
            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        }
    }

    private CloseableHttpResponse httpsRequestFor(GoAgentServerHttpClientBuilder builder) throws Exception {
        return requestFor(builder, subPath -> String.format("https://localhost:%s/go/%s", server.getSecurePort(), subPath));
    }

    private static CloseableHttpResponse requestFor(GoAgentServerHttpClientBuilder builder, ServerUrlGenerator urlGen) throws Exception {
        try (CloseableHttpClient client = builder.build()) {
            return client.execute(new HttpGet(urlGen.serverUrlFor("")));
        }
    }
}
