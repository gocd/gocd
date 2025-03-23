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

import com.thoughtworks.go.util.SslVerificationMode;
import com.thoughtworks.go.util.SystemEnvironment;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static com.thoughtworks.go.util.TestFileUtil.resourceToTempFile;
import static com.thoughtworks.go.util.TestFileUtil.resourceToTempPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoAgentServerClientBuilderTest {

    @Nested
    class AgentTruststore {
        @Test
        void agentTrustStoreShouldBeEmptyWithNoCerts() throws Exception {
            GoAgentServerClientBuilder<Object> builder = builderWith(null, SslVerificationMode.NONE, null, null, null);
            assertThat(builder.agentTruststore()).isNull();
        }

        @Test
        void agentTrustStoreShouldParseCertFromPem() throws Exception {
            GoAgentServerClientBuilder<Object> builder = builderWith(resourceToTempFile("/testdata/root-ca-ec.crt"), SslVerificationMode.NONE, null, null, null);
            KeyStore store = builder.agentTruststore();
            assertThat(store.aliases())
                    .extracting(Collections::list, InstanceOfAssertFactories.list(String.class))
                    .containsExactly("cn=example root ca");
            assertThat(store.getCertificate("cn=example root ca"))
                    .isInstanceOf(X509Certificate.class)
                    .satisfies(cert -> assertThat(((X509Certificate) cert).getSubjectX500Principal().getName()).isEqualTo("CN=Example Root CA"));
        }

        @Test
        void agentTrustStoreShouldParseMultipleCertsFromPem() throws Exception {
            GoAgentServerClientBuilder<Object> builder = builderWith(resourceToTempFile("/testdata/server-localhost-ec-chain.crt"), SslVerificationMode.NONE, null, null, null);
            KeyStore store = builder.agentTruststore();
            assertThat(store.aliases())
                    .extracting(Collections::list, InstanceOfAssertFactories.list(String.class))
                    .containsExactly("cn=localhost", "cn=example root ca");
        }
    }

    @Nested
    class AgentKeystore {
        @Test
        void agentKeystoreShouldBeEmptyWithNoCertsOrKey() throws Exception {
            assertThat(builderWith(null, SslVerificationMode.NONE, null, null, null).agentKeystore())
                    .isNull();
            assertThat(builderWith(null, SslVerificationMode.NONE, resourceToTempFile("/testdata/agent-client-cert-ec.crt"), null, null).agentKeystore())
                    .isNull();
            assertThat(builderWith(null, SslVerificationMode.NONE, null, resourceToTempFile("/testdata/agent-client-cert-ec-key.pk1"), null).agentKeystore())
                    .isNull();
        }

        @Test
        void agentKeystoreRequiresPrivateKeyInPem() throws Exception {
            GoAgentServerClientBuilder<Object> builder = builderWith(null, SslVerificationMode.NONE, resourceToTempFile("/testdata/agent-client-cert-ec.crt"), resourceToTempFile("/testdata/agent-client-cert-ec.crt"), null);
            assertThatThrownBy(builder::agentKeystore)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to parse key of type");
        }

        @ParameterizedTest
        @CsvSource({"ec, pk1", "ec, pk8", "rsa, pk1", "rsa, pk8"})
        void agentKeystoreRequiresPrivateKeyPassphraseWhenEncrypted(String keyType, String keyFormat) throws Exception {
            GoAgentServerClientBuilder<Object> builder = builderWith(null, SslVerificationMode.NONE, resourceToTempFile(String.format("/testdata/agent-client-cert-%s.crt", keyType)), resourceToTempFile(String.format("/testdata/agent-client-cert-%s-key.%s", keyType, keyFormat)), null);
            assertThatThrownBy(builder::agentKeystore)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("SSL private key passphrase not specified!");
        }

        @ParameterizedTest
        @CsvSource({"ec, pk1", "ec, pk8", "rsa, pk1", "rsa, pk8"})
        void agentKeystoreShouldParseWhenCertEncryptedKeyAndPassphraseSupplied(String keyType, String keyFormat) throws Exception {
            Path agentSslPrivateKeyPassphraseFile = resourceToTempPath("/testdata/agent-client-cert.pass");
            KeyStore store = builderWith(null, SslVerificationMode.NONE, resourceToTempFile(String.format("/testdata/agent-client-cert-%s.crt", keyType)), resourceToTempFile(String.format("/testdata/agent-client-cert-%s-key.%s", keyType, keyFormat)), agentSslPrivateKeyPassphraseFile.toFile()).agentKeystore();
            assertThat(store.aliases())
                    .extracting(Collections::list, InstanceOfAssertFactories.list(String.class))
                    .containsExactly("1");
            assertThat(store.getCertificate("1"))
                    .isInstanceOf(X509Certificate.class)
                    .satisfies(cert -> assertThat(((X509Certificate) cert).getSubjectX500Principal().getName()).isEqualTo(String.format("CN=agent-client-cert-%s", keyType)));
        }

        @ParameterizedTest
        @CsvSource({"ec, pk1", "ec, pk8", "rsa, pk1", "rsa, pk8"})
        void agentKeystoreShouldParseWhenCertAndKeySupplied(String keyType, String keyFormat) throws Exception {
            KeyStore store = builderWith(null, SslVerificationMode.NONE, resourceToTempFile(String.format("/testdata/agent-client-cert-%s-nopass.crt", keyType)), resourceToTempFile(String.format("/testdata/agent-client-cert-%s-nopass-key.%s", keyType, keyFormat)), null).agentKeystore();
            assertThat(store.aliases())
                    .extracting(Collections::list, InstanceOfAssertFactories.list(String.class))
                    .containsExactly("1");
            assertThat(store.getCertificate("1"))
                    .isInstanceOf(X509Certificate.class)
                    .satisfies(cert -> assertThat(((X509Certificate) cert).getSubjectX500Principal().getName()).isEqualTo(String.format("CN=agent-client-cert-%s-nopass", keyType)));
        }
    }

    private static GoAgentServerClientBuilder<Object> builderWith(final File rootCertificate, final SslVerificationMode sslVerificationMode, final File agentSslCertificate, final File agentSslPrivateKey, final File agentSslPrivateKeyPassphraseFile) {
        return new GoAgentServerClientBuilder<>(new SystemEnvironment(), rootCertificate, sslVerificationMode, agentSslCertificate, agentSslPrivateKey, agentSslPrivateKeyPassphraseFile) {
            @Override
            public Object build() {
                return null;
            }
        };
    }

}