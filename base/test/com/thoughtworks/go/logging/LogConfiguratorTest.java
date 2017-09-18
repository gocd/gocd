/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LogConfiguratorTest {

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private PrintStream originalErr;
    private PrintStream originalOut;

    @Before
    public void setUp() throws Exception {
        originalErr = System.err;
        originalOut = System.out;

        System.setErr(new PrintStream(stderr));
        System.setOut(new PrintStream(stdout));
    }

    @After
    public void tearDown() throws Exception {
        System.setErr(originalErr);
        System.setOut(originalOut);
    }

    @Test
    public void shouldUseDefaultLog4jConfigIfUserSpecifiedConfigFileIsNotFound() throws Exception {
        final boolean[] defaultLoggingInvoked = {false};
        LogConfigurator logConfigurator = new LogConfigurator("non-existant-dir", "non-existant.properties") {
            @Override
            protected void configureDefaultLogging() {
                defaultLoggingInvoked[0] = true;
            }
        };
        logConfigurator.initialize();

        assertTrue(defaultLoggingInvoked[0]);

        assertThat(stderr.toString(), containsString(String.format("Could not find file `%s'. Attempting to load from classpath.", new File("non-existant-dir", "non-existant.properties"))));
        assertThat(stderr.toString(), containsString("Could not find classpath resource `config/non-existant.properties'. Falling back to using a default logback configuration that writes to stdout."));
        assertThat(stdout.toString(), is(""));
    }

    @Test
    public void shouldUseDefaultLog4jConfigFromClasspathIfUserSpecifiedConfigFileIsNotFound() throws Exception {
        final URL[] initializeFromPropertyResource = {null};
        LogConfigurator logConfigurator = new LogConfigurator("xxx", "logging-test-logback.xml") {
            @Override
            protected void configureWith(URL resource) {
                initializeFromPropertyResource[0] = resource;
            }
        };
        logConfigurator.initialize();

        URL expectedResource = getClass().getClassLoader().getResource("config/logging-test-logback.xml");
        assertThat(initializeFromPropertyResource[0], equalTo(expectedResource));

        assertThat(stderr.toString(), containsString("Using classpath resource `" + expectedResource + "'"));
        assertThat(stdout.toString(), is(""));
    }

    @Test
    public void shouldFallbackToDefaultFileIfLog4jConfigFound() throws Exception {
        File configFile = folder.newFile("foo.properties");
        final URL[] initializeFromPropertiesFile = {null};
        LogConfigurator logConfigurator = new LogConfigurator(configFile.getParentFile().getAbsolutePath(), configFile.getName()) {
            @Override
            protected void configureWith(URL resource) {
                initializeFromPropertiesFile[0] = resource;
            }
        };

        logConfigurator.initialize();

        assertThat(initializeFromPropertiesFile[0], is(configFile.toURI().toURL()));

        assertThat(stderr.toString(), containsString(String.format("Using logback configuration from file %s", configFile)));
        assertThat(stdout.toString(), is(""));
    }
}
