/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class AgentCLITest {

    private ByteArrayOutputStream errorStream;
    private AgentCLI agentCLI;

    @Before
    public void setUp() {
        errorStream = new ByteArrayOutputStream();
        AgentCLI.SystemExitter exitter = status -> {
            throw new ExitException(status);
        };
        agentCLI = new AgentCLI(new PrintStream(errorStream), exitter);
    }

    @Test
    public void shouldDieIfNoArguments() {
        try {
            agentCLI.parse();
            fail("Was expecting an exception!");
        } catch (ExitException e) {
            assertThat(e.getStatus()).isEqualTo(1);
            assertThat(errorStream.toString()).contains("The following option is required: [-serverUrl]");
            assertThat(errorStream.toString()).contains("Usage: java -jar agent-bootstrapper.jar");
        }
    }

    @Test
    public void serverURLMustBeAValidURL() {
        try {
            agentCLI.parse("-serverUrl", "foobar");
            fail("Was expecting an exception!");
        } catch (ExitException e) {
            assertThat(e.getStatus()).isEqualTo(1);
            assertThat(errorStream.toString()).contains("-serverUrl is not a valid url");
            assertThat(errorStream.toString()).contains("Usage: java -jar agent-bootstrapper.jar");
        }
    }

    @Test
    public void shouldPassIfCorrectArgumentsAreProvided() {
        AgentBootstrapperArgs agentBootstrapperArgs = agentCLI.parse("-serverUrl", "https://go.example.com:8154/go", "-sslVerificationMode", "NONE");
        assertThat(agentBootstrapperArgs.getServerUrl().toString()).isEqualTo("https://go.example.com:8154/go");
        assertThat(agentBootstrapperArgs.getSslVerificationMode()).isEqualTo(AgentBootstrapperArgs.SslMode.NONE);
    }

    @Test
    public void shouldRaisExceptionWhenInvalidSslModeIsPassed() {
        assertThatCode(() -> {
            agentCLI.parse("-serverUrl", "https://go.example.com:8154/go", "-sslVerificationMode", "FOOBAR");
        })
                .isInstanceOf(ExitException.class)
                .satisfies(o -> {
                    assertThat(((ExitException) o).getStatus()).isEqualTo(1);
                });

        assertThat(errorStream.toString()).contains("Invalid value for -sslVerificationMode parameter. Allowed values:[FULL, NONE, NO_VERIFY_HOST]");
        assertThat(errorStream.toString()).contains("Usage: java -jar agent-bootstrapper.jar");
    }

    @Test
    public void shouldRaiseExceptionWhenRootCertificateFileIsNotPresent() {
        assertThatCode(() -> {
            agentCLI.parse("-serverUrl", "http://example.com/go", "-rootCertFile", UUID.randomUUID().toString());
        })
                .isInstanceOf(ExitException.class)
                .satisfies(o -> {
                    assertThat(((ExitException) o).getStatus()).isEqualTo(1);
                });

        assertThat(errorStream.toString()).contains("-rootCertFile must be a file that is readable.");
    }

    @Test
    public void shouldRaiseExceptionWhenSSLCertificateFileIsNotPresent() {
        assertThatCode(() -> {
            agentCLI.parse("-serverUrl", "http://example.com/go", "-sslCertificateFile", UUID.randomUUID().toString());
        })
                .isInstanceOf(ExitException.class)
                .satisfies(o -> {
                    assertThat(((ExitException) o).getStatus()).isEqualTo(1);
                });

        assertThat(errorStream.toString()).contains("-sslCertificateFile must be a file that is readable.");
    }

    @Test
    public void shouldRaiseExceptionWhenSSLPrivateKeyPassphraseFileIsNotPresent() {
        assertThatCode(() -> {
            agentCLI.parse("-serverUrl", "http://example.com/go", "-sslPrivateKeyPassphraseFile", UUID.randomUUID().toString());
        })
                .isInstanceOf(ExitException.class)
                .satisfies(o -> {
                    assertThat(((ExitException) o).getStatus()).isEqualTo(1);
                });

        assertThat(errorStream.toString()).contains("-sslPrivateKeyPassphraseFile must be a file that is readable.");
    }

    @Test
    public void shouldRaiseExceptionWhenSSLPrivateKeyFileIsNotPresent() {
        assertThatCode(() -> {
            agentCLI.parse("-serverUrl", "http://example.com/go", "-sslPrivateKeyFile", UUID.randomUUID().toString());
        })
                .isInstanceOf(ExitException.class)
                .satisfies(o -> {
                    assertThat(((ExitException) o).getStatus()).isEqualTo(1);
                });

        assertThat(errorStream.toString()).contains("-sslPrivateKeyFile must be a file that is readable.");
    }

    @Test
    public void shouldDefaultsTheSslModeToFullWhenNotSpecified() {
        AgentBootstrapperArgs agentBootstrapperArgs = agentCLI.parse("-serverUrl", "https://go.example.com/go");
        assertThat(agentBootstrapperArgs.getSslVerificationMode()).isEqualTo(AgentBootstrapperArgs.SslMode.FULL);
    }

    @Test
    public void printsHelpAndExitsWith0() {
        try {
            agentCLI.parse("-help");
            fail("Was expecting an exception!");
        } catch (ExitException e) {
            assertThat(e.getStatus()).isEqualTo(0);
        }
    }

    static class ExitException extends RuntimeException {
        private final int status;

        public ExitException(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
