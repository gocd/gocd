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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
            Assert.fail("Was expecting an exception!");
        } catch (ExitException e) {
            assertThat(e.getStatus(), is(1));
            assertThat(errorStream.toString(), containsString("The following option is required: [-serverUrl]"));
            assertThat(errorStream.toString(), containsString("Usage: java -jar agent-bootstrapper.jar"));
        }
    }

    @Test
    public void serverURLMustBeAValidURL() {
        try {
            agentCLI.parse("-serverUrl", "foobar");
            Assert.fail("Was expecting an exception!");
        } catch (ExitException e) {
            assertThat(e.getStatus(), is(1));
            assertThat(errorStream.toString(), containsString("-serverUrl is not a valid url"));
            assertThat(errorStream.toString(), containsString("Usage: java -jar agent-bootstrapper.jar"));
        }
    }

    @Test
    public void shouldPassIfCorrectArgumentsAreProvided() {
        AgentBootstrapperArgs agentBootstrapperArgs = agentCLI.parse("-serverUrl", "https://go.example.com:8154/go", "-sslVerificationMode", "NONE");
        assertThat(agentBootstrapperArgs.getServerUrl().toString(), is("https://go.example.com:8154/go"));
        assertThat(agentBootstrapperArgs.getSslVerificationMode(), is(AgentBootstrapperArgs.SslMode.NONE));
    }

    @Test
    public void shouldRaisExceptionWhenInvalidSslModeIsPassed() {
        try {
            agentCLI.parse("-serverUrl", "https://go.example.com:8154/go", "-sslVerificationMode", "FOOBAR");
            Assert.fail("Was expecting an exception!");
        } catch (ExitException e) {
            assertThat(e.getStatus(), is(1));
            assertThat(errorStream.toString(), containsString("Invalid value for -sslVerificationMode parameter. Allowed values:[FULL, NONE, NO_VERIFY_HOST]"));
            assertThat(errorStream.toString(), containsString("Usage: java -jar agent-bootstrapper.jar"));
        }
    }

    @Test
    public void shouldDefaultsTheSslModeToFullWhenNotSpecified() {
        AgentBootstrapperArgs agentBootstrapperArgs = agentCLI.parse("-serverUrl", "https://go.example.com/go");
        assertThat(agentBootstrapperArgs.getSslVerificationMode(), is(AgentBootstrapperArgs.SslMode.FULL));
    }

    @Test
    public void printsHelpAndExitsWith0() {
        try {
            agentCLI.parse("-help");
            Assert.fail("Was expecting an exception!");
        } catch (ExitException e) {
            assertThat(e.getStatus(), is(0));
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
