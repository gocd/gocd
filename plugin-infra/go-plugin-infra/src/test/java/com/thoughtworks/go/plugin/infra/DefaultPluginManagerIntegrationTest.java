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

import com.thoughtworks.go.plugin.activation.DefaultGoPluginActivator;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
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

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/applicationContext-plugin-infra.xml"})
@DirtiesContext
public class DefaultPluginManagerIntegrationTest {
    @ClassRule
    public static final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();
    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static final String PLUGIN_DESC_PROPERTY_SET_BY_TEST_PLUGIN_1 = "testplugin.descriptorValidator.setPluginDescriptor.invoked";
    private static final String PLUGIN_ID_1 = "testplugin.descriptorValidator";
    private static final String PLUGIN_TO_TEST_CLASSLOADER = "plugin.to.test.classloader";
    private static File bundleDir;
    @Autowired
    DefaultPluginManager pluginManager;
    @Autowired
    DefaultPluginJarChangeListener jarChangeListener;
    @Autowired
    SystemEnvironment systemEnvironment;

    @BeforeClass
    public static void overrideProperties() throws IOException {
        System.clearProperty("gocd.plugins.classloader.old");
        System.setProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName(), "defaultFiles/go-plugin-activator.jar");
        bundleDir = temporaryFolder.newFolder("bundleDir");
        System.setProperty(PLUGIN_BUNDLE_PATH.propertyName(), bundleDir.getAbsolutePath());
    }

    private static File pathOfFileInDefaultFiles(String filePath) {
        return new File(DefaultPluginManagerIntegrationTest.class.getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

    @Test
    public void shouldRegisterTheExtensionClassesOfAPluginAndInitializeAccessorUponFirstCall() throws Exception {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_ID_1);
        assertThat(plugin.id(), is(PLUGIN_ID_1));

        assertThat(plugin.bundleDescriptor().bundleSymbolicName(), is(PLUGIN_ID_1));
        assertThat(plugin.bundleDescriptor().bundleClassPath(), is("lib/go-plugin-activator.jar,.,lib/dependency.jar"));
        assertThat(plugin.bundleDescriptor().bundleActivator(), is(DefaultGoPluginActivator.class.getCanonicalName()));
        assertThat(plugin.isInvalid(), is(false));

        String extensionType = "notification";
        pluginManager.submitTo(PLUGIN_ID_1, extensionType, new DefaultGoPluginApiRequest(extensionType, "2.0", "test-request"));
        assertThat(System.getProperty(PLUGIN_DESC_PROPERTY_SET_BY_TEST_PLUGIN_1), is("PluginLoad: 1, InitAccessor"));
    }

    @Test
    public void shouldSetCurrentThreadContextClassLoaderToBundleClassLoaderToAvoidDependenciesFromWebappClassloaderMessingAroundWithThePluginBehavior() {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_TO_TEST_CLASSLOADER);
        assertThat(plugin.id(), is(PLUGIN_TO_TEST_CLASSLOADER));
        String extensionType = "notification";
        GoPluginApiResponse goPluginApiResponse = pluginManager.submitTo(PLUGIN_TO_TEST_CLASSLOADER, extensionType,
                new DefaultGoPluginApiRequest(extensionType, "2.0",
                        "Thread.currentThread.getContextClassLoader"));
        assertThat(goPluginApiResponse.responseBody(), is(BundleClassLoader.class.getCanonicalName()));
        goPluginApiResponse = pluginManager.submitTo(PLUGIN_TO_TEST_CLASSLOADER, extensionType,
                new DefaultGoPluginApiRequest(extensionType, "2.0",
                        "this.getClass.getClassLoader"));
        assertThat(goPluginApiResponse.responseBody(), is(BundleClassLoader.class.getCanonicalName()));
    }

    @Before
    public void setUpPluginInfrastructure() {
        try {
            pluginManager.startInfrastructure(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        jarChangeListener.pluginJarAdded(new PluginFileDetails(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.jar"), false));
        jarChangeListener.pluginJarAdded(new PluginFileDetails(pathOfFileInDefaultFiles("dumb.plugin.that.responds.with.classloader.name.jar"), false));
    }

    @After
    public void tearDown() {
        pluginManager.stopInfrastructure();
    }

    //TODO: Write Test to handle OSGIFWK and PLugin Manager Interaction.
}
