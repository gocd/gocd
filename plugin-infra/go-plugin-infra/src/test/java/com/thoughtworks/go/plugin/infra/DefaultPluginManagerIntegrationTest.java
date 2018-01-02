/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/applicationContext-plugin-infra.xml"})
public class DefaultPluginManagerIntegrationTest {
    public static final String PLUGIN_DESC_PROPERTY_SET_BY_TEST_PLUGIN_1 = "testplugin.descriptorValidator.setPluginDescriptor.invoked";
    private static final String PLUGIN_DIR_NAME = "./tmp-DefPlgnMgrIntTest";
    private static final String BUNDLE_DIR_NAME = "./tmp-bundles-DefPlgnMgrIntTest";
    private static final File PLUGIN_DIR = new File(PLUGIN_DIR_NAME);
    private static final File BUNDLE_DIR = new File(BUNDLE_DIR_NAME);
    private static final String PLUGIN_ID_1 = "testplugin.descriptorValidator";
    @Autowired DefaultPluginManager pluginManager;
    @Autowired DefaultPluginJarChangeListener jarChangeListener;
    @Autowired SystemEnvironment systemEnvironment;

    static {
        System.setProperty(PLUGIN_ACTIVATOR_JAR_PATH.propertyName(), "defaultFiles/go-plugin-activator.jar");
        System.setProperty(PLUGIN_BUNDLE_PATH.propertyName(), BUNDLE_DIR_NAME);
    }

    private static File pathOfFileInDefaultFiles(String filePath) {
        return new File(DefaultPluginManagerIntegrationTest.class.getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

    @Test
    public void shouldRegisterTheExtensionClassesOfAPluginAndExposeItInItsDescriptorAfterItIsLoaded() throws Exception {
        GoPluginDescriptor plugin = pluginManager.getPluginDescriptorFor(PLUGIN_ID_1);
        assertThat(plugin.id(), is(PLUGIN_ID_1));

        assertThat(plugin.bundleSymbolicName(), is(PLUGIN_ID_1));
        assertThat(plugin.bundleClassPath(), is("lib/go-plugin-activator.jar,.,lib/dependency.jar"));
        assertThat(plugin.bundleActivator(), is(DefaultGoPluginActivator.class.getCanonicalName()));
        assertThat(plugin.isInvalid(), is(false));
        //property set by the plugin in the setPluginDescriptor method.
        assertThat(System.getProperty(PLUGIN_DESC_PROPERTY_SET_BY_TEST_PLUGIN_1), is(plugin.toString()));
    }

    @Before
    public void setUpPluginInfrastructure() throws IOException {
        PLUGIN_DIR.mkdirs();
        BUNDLE_DIR.mkdirs();
        try {
            pluginManager.startInfrastructure(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        jarChangeListener.pluginJarAdded(new PluginFileDetails(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.jar"), false));
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(PLUGIN_DESC_PROPERTY_SET_BY_TEST_PLUGIN_1);
        FileUtils.deleteQuietly(PLUGIN_DIR);
        FileUtils.deleteQuietly(BUNDLE_DIR);
        pluginManager.stopInfrastructure();
        FileUtils.deleteQuietly(PLUGIN_DIR);
        FileUtils.deleteQuietly(BUNDLE_DIR);
    }

    //TODO: Write Test to handle OSGIFWK and PLugin Manager Interaction.
}
