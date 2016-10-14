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

package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.util.NestedJarClassLoader;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarInputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TfsSDKCommandBuilderTest {
    private TfsSDKCommandBuilder builder;
    private final String DOMAIN = "CORPORATE";
    private final String USERNAME = "userName";
    private final String PASSWORD = "password";
    private final String computedWorkspaceName = "boo-yaa-goo-moo-foo";
    private NestedJarClassLoader mockSdkLoader;

    @Before
    public void setup() throws IOException, URISyntaxException {
        mockSdkLoader = mock(NestedJarClassLoader.class);
        builder = new TfsSDKCommandBuilder(mockSdkLoader);
    }

    @Test
    public void shouldLoadTheCorrectImplementationOfSDKCommandViaTheNestedClassLoader() throws Exception {
        when(mockSdkLoader.loadClass("com.thoughtworks.go.tfssdk.TfsSDKCommandTCLAdapter")).thenThrow(new RuntimeException());
        try {
            builder.buildTFSSDKCommand(null,new UrlArgument("url"), DOMAIN, USERNAME, PASSWORD, computedWorkspaceName, "$/project");
            fail("should have failed to load class as we are not wiring any dependencies");
        } catch (Exception e) {
            //Do not worry about load class failing. We're only asserting that load class is invoked with the right FQN for TFSSDKCommand
        }
        verify(mockSdkLoader, times(1)).loadClass("com.thoughtworks.go.tfssdk.TfsSDKCommandTCLAdapter");
    }

    @Test
    public void shouldCheckMSSDKLoggingSupportBeforeUpgradingLog4j() throws Exception {
        URL log4jJarFromClasspath = getLog4jJarFromClasspath();
        assertThat(log4jJarFromClasspath != null, is(true));
        String version = implementationVersionFromManifrest(log4jJarFromClasspath);
        assertThat(version != null, is(true));
        assertThat(version, is("1.2.17"));
    }

    private String implementationVersionFromManifrest(URL log4jJarFromClasspath) throws IOException {
        JarInputStream in = new JarInputStream(log4jJarFromClasspath.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.getManifest().write(out);
        out.close();

        List<String> lines = IOUtils.readLines(new ByteArrayInputStream(out.toByteArray()));
        for (String line : lines) {
            if (line.startsWith("Implementation-Version")) {
                 return line.split(":")[1].trim();
            }
        }
        return null;
    }

    private URL getLog4jJarFromClasspath() throws URISyntaxException {
        URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
        for (URL u : classLoader.getURLs()) {
            String jarPath = u.getPath();
            if (jarPath.endsWith(".jar") && jarPath.contains("log4j")) {
                return u;
            }
        }
        return null;
    }
}
