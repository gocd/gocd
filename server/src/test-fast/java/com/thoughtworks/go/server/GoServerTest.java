/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.validators.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
public class GoServerTest {

    @SystemStub
    SystemProperties systemProperties;

    @BeforeEach
    public void setUp() {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.set(SystemEnvironment.APP_SERVER, AppServerStub.class.getCanonicalName());
    }

    @Test
    public void shouldValidateOnServerStartup() throws Exception {

        final SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        StubGoServer goServer = new StubGoServer(systemEnvironment, Validation.SUCCESS);
        goServer.subprocessLogger = mock(SubprocessLogger.class);
        final File tmpFile = TestFileUtil.createTempFile("keystore.tmp");
        tmpFile.deleteOnExit();


        when(systemEnvironment.getServerPort()).thenReturn(9153);
        goServer.go();
        assertThat(goServer.wasStarted(), is(true));
    }

    @Test
    public void shouldRegisterSubprocessLoggerAsExit() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        Validation validation = mock(Validation.class);
        when(validation.isSuccessful()).thenReturn(true);
        StubGoServer goServer = new StubGoServer(systemEnvironment, validation);
        goServer.subprocessLogger = mock(SubprocessLogger.class);
        goServer.go();
        verify(goServer.subprocessLogger).registerAsExitHook("Following processes were alive at shutdown: ");
    }

    @Test
    public void shouldCreateASubprocessLoggerInConstructor() {
        GoServer goServer = new GoServer();
        assertThat(goServer.subprocessLogger, not(nullValue()));
    }

    @Test
    public void shouldNotStartServerIfValidationFails() throws Exception {
        final SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        Validation validation = new Validation().addError(new Exception("Server Port occupied"));
        StubGoServer goServer = new StubGoServer(systemEnvironment, validation);

        goServer.go();
        assertThat(goServer.wasStarted(), is(false));
    }

    @Test
    public void shouldStartAppServer() throws Exception {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.set(SystemEnvironment.APP_SERVER, AppServerStub.class.getCanonicalName());
        GoServer goServer = new GoServer();

        goServer.startServer();
        AppServer appServer = com.thoughtworks.go.util.ReflectionUtil.getField(goServer, "server");
        assertThat(appServer instanceof AppServerStub, is(true));
        AppServerStub appServerStub = (AppServerStub) appServer;

        assertThat(appServerStub.calls.get("setSessionCookieConfig"), is("something"));
        assertThat(appServerStub.calls.get("hasStarted"), is(true));
        assertThat(appServerStub.calls.get("configure"), is(true));
        assertThat(appServerStub.calls.get("start"), is(true));
        assertThat(appServerStub.calls.get("stop"), is(nullValue()));

        goServer.stop();
        assertThat(appServerStub.calls.get("stop"), is(true));
    }

    @Test
    public void shouldStopServerAndThrowExceptionWhenServerFailsToStartWithAnUnhandledException() throws Exception {
        final AppServer server = mock(AppServer.class);
        when(server.hasStarted()).thenReturn(false);
        when(server.getUnavailableException()).thenReturn(new RuntimeException("Some unhandled server startup exception"));

        GoServer goServer = new GoServer(){
            @Override
            AppServer configureServer() {
                return server;
            }
        };
        doNothing().when(server).start();
        doNothing().when(server).stop();

        try {
            goServer.startServer();
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Failed to start GoCD server."));
            assertThat(e.getCause().getMessage(), is("Some unhandled server startup exception"));
        }

        verify(server).start();
        verify(server).getUnavailableException();
        verify(server).stop();
    }

    private static class StubGoServer extends GoServer {
        private boolean wasStarted = false;
        private final Validation validation;

        public StubGoServer(SystemEnvironment systemEnvironment, Validation validation) {
            super(systemEnvironment);
            this.validation = validation;
        }

        @Override
        protected void startServer() {
            wasStarted = true;
        }

        public Boolean wasStarted() {
            return wasStarted;
        }

        @Override
        Validation validate() {
            return validation;
        }
    }

}
