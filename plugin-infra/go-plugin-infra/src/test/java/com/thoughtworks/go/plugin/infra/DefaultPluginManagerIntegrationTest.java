/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.BundleOrPluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_WORK_DIR;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DirtiesContext
@ContextConfiguration(locations = {"classpath:/applicationContext-plugin-infra.xml"})
class DefaultPluginManagerIntegrationTest {
    private static final String PLUGIN_DESC_PROPERTY_SET_BY_TEST_PLUGIN_1 = "testplugin.descriptorValidator.setPluginDescriptor.invoked";
    private static final String PLUGIN_ID_1 = "testplugin.descriptorValidator";
    private static final String PLUGIN_TO_TEST_CLASSLOADER = "plugin.to.test.classloader";
    private static File bundleDir;
    @Autowired
    DefaultPluginManager pluginManager;
    @Autowired
    DefaultPluginJarChangeListener jarChangeListener;
    @Autowired
    SystemEnvironment systemEnvironment;

    @BeforeAll
    static void overrideProperties(@TempDir File rootDir) {
        FileHelper temporaryFolder = new FileHelper(rootDir);
        bundleDir = temporaryFolder.newFolder("bundleDir");

        System.clearProperty("gocd.plugins.classloader.old");
        System.setProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName(), "defaultFiles/go-plugin-activator.jar");
        System.setProperty(PLUGIN_WORK_DIR.propertyName(), bundleDir.getAbsolutePath());
    }

    @BeforeEach
    void setUpPluginInfrastructure() {
        try {
            pluginManager.startInfrastructure(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        jarChangeListener.pluginJarAdded(new BundleOrPluginFileDetails(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.jar"), false, bundleDir));
        jarChangeListener.pluginJarAdded(new BundleOrPluginFileDetails(pathOfFileInDefaultFiles("dumb.plugin.that.responds.with.classloader.name.jar"), false, bundleDir));
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("gocd.plugins.classloader.old");
        System.clearProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName());
        System.clearProperty(PLUGIN_WORK_DIR.propertyName());
        pluginManager.stopInfrastructure();
    }

    private static File pathOfFileInDefaultFiles(String filePath) {
        return new File(DefaultPluginManagerIntegrationTest.class.getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

    @Test
    void shouldRegisterTheExtensionClassesOfAPluginAndInitializeAccessorUponFirstCall() {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_ID_1);
        assertThat(plugin.id()).isEqualTo(PLUGIN_ID_1);

        assertThat(plugin.bundleDescriptor().bundleSymbolicName()).isEqualTo(PLUGIN_ID_1);
        assertThat(plugin.isInvalid()).isFalse();

        String extensionType = "notification";
        pluginManager.submitTo(PLUGIN_ID_1, extensionType, new DefaultGoPluginApiRequest(extensionType, "2.0", "test-request"));
        assertThat(System.getProperty(PLUGIN_DESC_PROPERTY_SET_BY_TEST_PLUGIN_1)).isEqualTo("PluginLoad: 1, InitAccessor");
    }

    @Test
    void shouldSetCurrentThreadContextClassLoaderToBundleClassLoaderToAvoidDependenciesFromWebappClassloaderMessingAroundWithThePluginBehavior() {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_TO_TEST_CLASSLOADER);
        assertThat(plugin.id()).isEqualTo(PLUGIN_TO_TEST_CLASSLOADER);
        String extensionType = "notification";
        GoPluginApiResponse goPluginApiResponse = pluginManager.submitTo(PLUGIN_TO_TEST_CLASSLOADER, extensionType,
                new DefaultGoPluginApiRequest(extensionType, "2.0",
                        "Thread.currentThread.getContextClassLoader"));
        assertThat(goPluginApiResponse.responseBody()).isEqualTo(BundleClassLoader.class.getCanonicalName());
        goPluginApiResponse = pluginManager.submitTo(PLUGIN_TO_TEST_CLASSLOADER, extensionType,
                new DefaultGoPluginApiRequest(extensionType, "2.0",
                        "this.getClass.getClassLoader"));
        assertThat(goPluginApiResponse.responseBody()).isEqualTo(BundleClassLoader.class.getCanonicalName());
    }
}
