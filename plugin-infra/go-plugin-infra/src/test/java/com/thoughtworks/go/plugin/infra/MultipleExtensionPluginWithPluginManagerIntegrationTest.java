/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/applicationContext-plugin-infra.xml"})
@DirtiesContext
public class MultipleExtensionPluginWithPluginManagerIntegrationTest {
    @ClassRule
    public static final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();
    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String EXTENSION_1_PROPERTY_PREFIX = "valid-plugin-with-multiple-extensions.task_extension.";
    private static final String EXTENSION_2_PROPERTY_PREFIX = "valid-plugin-with-multiple-extensions.analytics_extension.";

    private static final String PLUGIN_ID = "valid-plugin-with-multiple-extensions";

    @Autowired
    DefaultPluginManager pluginManager;
    @Autowired
    DefaultPluginJarChangeListener jarChangeListener;
    @Autowired
    SystemEnvironment systemEnvironment;
    private static File bundleDir;
    private static File pluginDir;

    @BeforeClass
    public static void overrideProperties() throws IOException {
        bundleDir = temporaryFolder.newFolder("bundleDir");
        pluginDir = temporaryFolder.newFolder("pluginDir");

        System.setProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName(), "defaultFiles/go-plugin-activator.jar");
        System.setProperty(PLUGIN_BUNDLE_PATH.propertyName(), bundleDir.getAbsolutePath());
        System.setProperty(PLUGIN_GO_PROVIDED_PATH.propertyName(), pluginDir.getAbsolutePath());
        System.setProperty(PLUGIN_EXTERNAL_PROVIDED_PATH.propertyName(), pluginDir.getAbsolutePath());
    }

    @Before
    public void setUpPluginInfrastructure() {
        pluginManager.startInfrastructure(false);

        URL multiExtensionJar = MultipleExtensionPluginWithPluginManagerIntegrationTest.class.getClassLoader().getResource("defaultFiles/valid-plugin-with-multiple-extensions.jar");
        jarChangeListener.pluginJarAdded(new PluginFileDetails(new File(multiExtensionJar.getFile()), false));
    }

    @Test
    public void shouldInitializeAccessorForEveryExtensionJustBeforeSendingTheFirstEverRequest() {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_ID);
        assertThat(plugin.id(), is(PLUGIN_ID));

        assertThat(plugin.bundleDescriptor().bundleSymbolicName(), is(PLUGIN_ID));
        assertThat(plugin.isInvalid(), is(false));

        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("task", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 1 }");

        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count"), is("1"));
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "initialize_accessor.count"), is(nullValue()));

        request = new DefaultGoPluginApiRequest("analytics", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 2 }");

        pluginManager.submitTo(PLUGIN_ID, "analytics", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count"), is("1"));
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "initialize_accessor.count"), is("1"));
    }

    @Test
    public void shouldNotReinitializeWithAccessorIfAlreadyInitialized() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("task", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 1 }");

        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count"), is("1"));

        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count"), is("1"));
    }

    @Test
    public void shouldSubmitRequestToTheRightExtension() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("task", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 1 }");
        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.count"), is("1"));
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.name"), is("request1"));
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.body"), is("{ \"abc\": 1 }"));
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "request.count"), is(nullValue()));

        request = new DefaultGoPluginApiRequest("task", "1.0", "request2");
        request.setRequestBody("{ \"abc\": 2 }");
        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.count"), is("2"));
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.name"), is("request2"));
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.body"), is("{ \"abc\": 2 }"));
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "request.count"), is(nullValue()));
    }

    @After
    public void tearDown() {
        pluginManager.stopInfrastructure();

        FileUtils.deleteQuietly(new File("felix-cache"));
    }
}
