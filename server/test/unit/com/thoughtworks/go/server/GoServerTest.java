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

import com.thoughtworks.go.helpers.FileSystemUtils;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.validators.Validation;
import org.hamcrest.CoreMatchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(org.jmock.integration.junit4.JMock.class)
public class GoServerTest {
    Mockery context = new ClassMockery();
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = new SystemEnvironment();
        systemEnvironment.set(SystemEnvironment.APP_SERVER, AppServerStub.class.getCanonicalName());
    }

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

    @Test
    public void shouldStartAppServer() throws Exception {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.set(SystemEnvironment.APP_SERVER, AppServerStub.class.getCanonicalName());
        GoServer goServer = new GoServer();

        goServer.startServer();
        AppServer appServer = (AppServer) com.thoughtworks.go.util.ReflectionUtil.getField(goServer, "server");
        assertThat(appServer instanceof AppServerStub, is(true));
        AppServerStub appServerStub = (AppServerStub) appServer;

        assertThat((String) appServerStub.calls.get("addExtraJarsToClasspath"), is(""));
        assertThat((Integer) appServerStub.calls.get("setCookieExpirePeriod"), is(1209600));
        assertThat((Boolean) appServerStub.calls.get("getUnavailableException"), is(true));
        assertThat((Boolean) appServerStub.calls.get("configure"), is(true));
        assertThat((Boolean) appServerStub.calls.get("start"), is(true));
        assertThat(appServerStub.calls.get("stop"), is(CoreMatchers.nullValue()));

        goServer.stop();
        assertThat((Boolean) appServerStub.calls.get("stop"), is(true));
    }

    @Test
    public void shouldStopServerAndThrowExceptionWhenServerFailsToStartWithAnUnhandledException() throws Exception {
        final AppServer server = mock(AppServer.class);
        when(server.getUnavailableException()).thenReturn(new RuntimeException("Some unhandled server startup exception"));

        GoServer goServer = new GoServer(){
            @Override
            AppServer configureServer() throws Exception {
                return server;
            }
        };
        doNothing().when(server).start();
        doNothing().when(server).stop();

        try {
            goServer.startServer();
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Failed to start Go server."));
            assertThat(e.getCause().getMessage(), is("Some unhandled server startup exception"));
        }

        verify(server).start();
        verify(server).getUnavailableException();
        verify(server).stop();
    }

    @Test
    public void shouldLoadAllJarsInTheAddonsDirectoryIntoClassPath() throws Exception {
        File addonsDirectory = createInAddonDir("some-addon-dir");
        FileSystemUtils.createFile("addon-1.JAR", addonsDirectory);
        FileSystemUtils.createFile("addon-2.jar", addonsDirectory);
        FileSystemUtils.createFile("addon-3.jAR", addonsDirectory);
        FileSystemUtils.createFile("some-file-which-does-not-end-with-dot-jar.txt", addonsDirectory);

        File oneAddonDirectory = createInAddonDir("one-addon-dir");
        FileSystemUtils.createFile("addon-1.jar", oneAddonDirectory);

        File noAddonDirectory = createInAddonDir("no-addon-dir");
        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[0]);


        GoServer goServerWithMultipleAddons = new GoServer(setAddonsPathTo(addonsDirectory), sslSocketFactory);
        goServerWithMultipleAddons.startServer();
        AppServerStub appServer = (AppServerStub) com.thoughtworks.go.util.ReflectionUtil.getField(goServerWithMultipleAddons, "server");
        assertExtraClasspath(appServer, "test-addons/some-addon-dir/addon-1.JAR", "test-addons/some-addon-dir/addon-2.jar", "test-addons/some-addon-dir/addon-3.jAR");

        GoServer goServerWithOneAddon = new GoServer(setAddonsPathTo(oneAddonDirectory), sslSocketFactory);
        goServerWithOneAddon.startServer();
        appServer = (AppServerStub) com.thoughtworks.go.util.ReflectionUtil.getField(goServerWithOneAddon, "server");
        assertExtraClasspath(appServer, "test-addons/one-addon-dir/addon-1.jar");

        GoServer goServerWithNoAddon = new GoServer(setAddonsPathTo(noAddonDirectory), sslSocketFactory);
        goServerWithNoAddon.startServer();
        appServer = (AppServerStub) com.thoughtworks.go.util.ReflectionUtil.getField(goServerWithNoAddon, "server");
        assertExtraClasspath(appServer, "");

        GoServer goServerWithInaccessibleAddonDir = new GoServer(setAddonsPathTo(new File("non-existent-directory")), sslSocketFactory);
        goServerWithInaccessibleAddonDir.startServer();
        appServer = (AppServerStub) com.thoughtworks.go.util.ReflectionUtil.getField(goServerWithNoAddon, "server");
        assertExtraClasspath(appServer, "");
    }

    @Test
    public void shouldTurnOffJrubyObjectProxyCacheByDefault(){
        new GoServer();
        assertThat(new SystemEnvironment().getPropertyImpl("jruby.ji.objectProxyCache"), is("false"));
    }

    private void assertExtraClasspath(AppServerStub appServer, String... expectedClassPathJars) {
        String extraJars = (String) appServer.calls.get("addExtraJarsToClasspath");
        List<String> actualExtraClassPath = Arrays.asList(extraJars.split(","));

        assertEquals("Number of jars wrong. Expected: " + Arrays.asList(expectedClassPathJars) + ". Actual: " + actualExtraClassPath, expectedClassPathJars.length, actualExtraClassPath.size());
        for (String expectedClassPathJar : expectedClassPathJars) {
            String platformIndependantNameOfExpectedJar = expectedClassPathJar.replace("/", File.separator);
            assertTrue("Expected " + extraJars + " to contain: " + platformIndependantNameOfExpectedJar, actualExtraClassPath.contains(platformIndependantNameOfExpectedJar));
        }
    }

    private File createInAddonDir(String dirInsideAddonDir) {
        File addonDir = new File("test-addons");
        File dirWhichWillContainAddons = new File(addonDir, dirInsideAddonDir);
        dirWhichWillContainAddons.mkdirs();
        return dirWhichWillContainAddons;
    }

    private SystemEnvironment setAddonsPathTo(File path) {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.APP_SERVER)).thenReturn(AppServerStub.class.getCanonicalName());
        doReturn(path.getPath()).when(systemEnvironment).get(SystemEnvironment.ADDONS_PATH);
        return systemEnvironment;
    }


    private class StubGoServer extends GoServer {
        private boolean wasStarted = false;
        private Validation validation;

        public StubGoServer(SystemEnvironment systemEnvironment, Validation validation) {
            super(systemEnvironment,null);
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
