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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.FileHelper;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.BundleOrPluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.net.URL;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:/applicationContext-plugin-infra.xml"})
@DirtiesContext
class MultipleExtensionPluginWithPluginManagerIntegrationTest {
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
    private static File pluginWorkDir;
    private static FileHelper temporaryFolder;

    @BeforeAll
    static void overrideProperties(@TempDir File rootDir) {
        temporaryFolder = new FileHelper(rootDir);
        bundleDir = temporaryFolder.newFolder("bundleDir");
        pluginWorkDir = temporaryFolder.newFolder("pluginDir");

        System.setProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName(), "defaultFiles/go-plugin-activator.jar");
        System.setProperty(PLUGIN_WORK_DIR.propertyName(), bundleDir.getAbsolutePath());
        System.setProperty(PLUGIN_GO_PROVIDED_PATH.propertyName(), pluginWorkDir.getAbsolutePath());
        System.setProperty(PLUGIN_EXTERNAL_PROVIDED_PATH.propertyName(), pluginWorkDir.getAbsolutePath());
    }

    @BeforeEach
    void setUpPluginInfrastructure() {
        pluginManager.startInfrastructure(false);

        URL multiExtensionJar = MultipleExtensionPluginWithPluginManagerIntegrationTest.class.getClassLoader().getResource("defaultFiles/valid-plugin-with-multiple-extensions.jar");
        jarChangeListener.pluginJarAdded(new BundleOrPluginFileDetails(new File(multiExtensionJar.getFile()), false, pluginWorkDir));
    }

    @AfterEach
    void tearDown() {
        pluginManager.stopInfrastructure();
        FileUtils.deleteQuietly(new File("felix-cache"));
    }

    @AfterAll
    static void clearSystemProperties() {
        System.clearProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName());
        System.clearProperty(PLUGIN_WORK_DIR.propertyName());
        System.clearProperty(PLUGIN_GO_PROVIDED_PATH.propertyName());
        System.clearProperty(PLUGIN_EXTERNAL_PROVIDED_PATH.propertyName());
    }

    @Test
    void shouldInitializeAccessorForEveryExtensionJustBeforeSendingTheFirstEverRequest() {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_ID);
        assertThat(plugin.id()).isEqualTo(PLUGIN_ID);

        assertThat(plugin.bundleDescriptor().bundleSymbolicName()).isEqualTo(PLUGIN_ID);
        assertThat(plugin.isInvalid()).isFalse();

        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("task", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 1 }");

        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count")).isEqualTo("1");
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "initialize_accessor.count")).isNull();

        request = new DefaultGoPluginApiRequest("analytics", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 2 }");

        pluginManager.submitTo(PLUGIN_ID, "analytics", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count")).isEqualTo("1");
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "initialize_accessor.count")).isEqualTo("1");
    }

    @Test
    void shouldNotReinitializeWithAccessorIfAlreadyInitialized() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("task", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 1 }");

        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count")).isEqualTo("1");

        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "initialize_accessor.count")).isEqualTo("1");
    }

    @Test
    void shouldSubmitRequestToTheRightExtension() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("task", "1.0", "request1");
        request.setRequestBody("{ \"abc\": 1 }");
        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.count")).isEqualTo("1");
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.name")).isEqualTo("request1");
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.body")).isEqualTo("{ \"abc\": 1 }");
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "request.count")).isNull();

        request = new DefaultGoPluginApiRequest("task", "1.0", "request2");
        request.setRequestBody("{ \"abc\": 2 }");
        pluginManager.submitTo(PLUGIN_ID, "task", request);

        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.count")).isEqualTo("2");
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.name")).isEqualTo("request2");
        assertThat(System.getProperty(EXTENSION_1_PROPERTY_PREFIX + "request.body")).isEqualTo("{ \"abc\": 2 }");
        assertThat(System.getProperty(EXTENSION_2_PROPERTY_PREFIX + "request.count")).isNull();
    }
}
