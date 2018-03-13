/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.activation.DefaultGoPluginActivator;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/applicationContext-plugin-infra.xml"})
public class MultipleExtensionPluginWithPluginManagerIntegrationTest {
    private static final String EXTENSION_1_PROPERTY_PREFIX = "valid-plugin-with-multiple-extensions.task_extension.";
    private static final String EXTENSION_2_PROPERTY_PREFIX = "valid-plugin-with-multiple-extensions.analytics_extension.";

    private static final String PLUGIN_DIR_NAME = "./tmp-DefPlgnMgrIntTest";
    private static final String BUNDLE_DIR_NAME = "./tmp-bundles-DefPlgnMgrIntTest";
    private static final File PLUGIN_DIR = new File(PLUGIN_DIR_NAME);
    private static final File BUNDLE_DIR = new File(BUNDLE_DIR_NAME);
    private static final String PLUGIN_ID = "valid-plugin-with-multiple-extensions";

    @Autowired DefaultPluginManager pluginManager;
    @Autowired DefaultPluginJarChangeListener jarChangeListener;
    @Autowired SystemEnvironment systemEnvironment;

    static {
        System.setProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName(), "defaultFiles/go-plugin-activator.jar");
        System.setProperty(PLUGIN_BUNDLE_PATH.propertyName(), BUNDLE_DIR_NAME);
        System.setProperty(PLUGIN_GO_PROVIDED_PATH.propertyName(), PLUGIN_DIR_NAME);
        System.setProperty(PLUGIN_EXTERNAL_PROVIDED_PATH.propertyName(), PLUGIN_DIR_NAME);
    }

    @Before
    public void setUpPluginInfrastructure() throws IOException {
        clearProperties();

        PLUGIN_DIR.mkdirs();
        BUNDLE_DIR.mkdirs();

        pluginManager.startInfrastructure(false);

        URL multiExtensionJar = MultipleExtensionPluginWithPluginManagerIntegrationTest.class.getClassLoader().getResource("defaultFiles/valid-plugin-with-multiple-extensions.jar");
        jarChangeListener.pluginJarAdded(new PluginFileDetails(new File(multiExtensionJar.getFile()), false));
    }

    @Test
    public void shouldInitializeAccessorForEveryExtensionJustBeforeSendingTheFirstEverRequest() throws Exception {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_ID);
        assertThat(plugin.id(), is(PLUGIN_ID));

        assertThat(plugin.bundleSymbolicName(), is(PLUGIN_ID));
        assertThat(plugin.bundleClassPath(), is("lib/go-plugin-activator.jar,."));
        assertThat(plugin.bundleActivator(), is(DefaultGoPluginActivator.class.getCanonicalName()));
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

        FileUtils.deleteQuietly(PLUGIN_DIR);
        FileUtils.deleteQuietly(BUNDLE_DIR);
        FileUtils.deleteQuietly(new File("felix-cache"));

        clearProperties();
    }

    private void clearProperties() {
        List<String> propertySuffixes = asList("initialize_accessor.count", "initialize_accessor.value", "request.count", "request.name", "request.body", "plugin_identifier.count", "plugin_identifier.value");

        for (String suffix : propertySuffixes) {
            System.clearProperty(EXTENSION_1_PROPERTY_PREFIX + suffix);
            System.clearProperty(EXTENSION_2_PROPERTY_PREFIX + suffix);
        }
    }

}
