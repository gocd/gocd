/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server;

import java.io.File;

import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.validators.Validation;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(org.jmock.integration.junit4.JMock.class)
public class GoServerTest {
    Mockery context = new ClassMockery();

    @Test
    public void shouldValidateOnServerStartup() throws Exception {

        final SystemEnvironment systemEnvironment = context.mock(SystemEnvironment.class);
        StubGoServer goServer = new StubGoServer(systemEnvironment, Validation.SUCCESS);
        goServer.subprocessLogger = mock(SubprocessLogger.class);
        final File tmpFile = TestFileUtil.createTempFile("keystore.tmp");
        tmpFile.deleteOnExit();

        context.checking(new Expectations() {
            {
                allowing(systemEnvironment).getServerPort();
                will(returnValue(9153));
                allowing(systemEnvironment).getSslServerPort();
                will(returnValue(9443));
                allowing(systemEnvironment).keystore();
                will(returnValue(tmpFile));
                allowing(systemEnvironment).truststore();
                will(returnValue(tmpFile));
                allowing(systemEnvironment).agentkeystore();
                will(returnValue(tmpFile));
            }
        });
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
        final SystemEnvironment systemEnvironment = context.mock(SystemEnvironment.class);
        Validation validation = new Validation().addError(new Exception("Server Port occupied"));
        StubGoServer goServer = new StubGoServer(systemEnvironment, validation);

        goServer.go();
        assertThat(goServer.wasStarted(), is(false));
    }

    private class StubGoServer extends GoServer {
        private boolean wasStarted = false;
        private Validation validation;

        public StubGoServer(SystemEnvironment systemEnvironment, Validation validation) {
            super(systemEnvironment,null, null);
            this.validation = validation;
        }

        @Override
        protected void startServer() throws Exception {
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
