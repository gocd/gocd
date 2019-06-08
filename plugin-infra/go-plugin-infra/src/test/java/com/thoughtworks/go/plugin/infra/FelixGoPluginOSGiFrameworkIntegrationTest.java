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

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginOSGiManifest;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.apache.felix.framework.util.FelixConstants;
import org.junit.*;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FelixGoPluginOSGiFrameworkIntegrationTest {
    @ClassRule
    public static final RestoreSystemProperties RESTORE_SYSTEM_PROPERTIES = new RestoreSystemProperties();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private FelixGoPluginOSGiFramework pluginOSGiFramework;
    private File descriptorBundleDir;
    private File errorGeneratingDescriptorBundleDir;
    private File exceptionThrowingAtLoadDescriptorBundleDir;
    private File validMultipleExtensionPluginBundleDir;
    private File pluginToTestClassloadPluginBundleDir;
    private DefaultPluginRegistry registry;
    private SystemEnvironment systemEnvironment;
    private static final String PLUGIN_ID = "testplugin.descriptorValidator";

    @Before
    public void setUp() throws Exception {
        registry = new DefaultPluginRegistry();
        systemEnvironment = new SystemEnvironment();
        pluginOSGiFramework = new FelixGoPluginOSGiFramework(registry, systemEnvironment) {
            @Override
            protected HashMap<String, String> generateOSGiFrameworkConfig() {
                HashMap<String, String> config = super.generateOSGiFrameworkConfig();
                config.put(FelixConstants.RESOLVER_PARALLELISM, "1");
                return config;
            }
        };
        pluginOSGiFramework.start();

        try (ZipInputStream zippedOSGiBundleFile = new ZipInputStream(FileUtils.openInputStream(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.osgi.jar")))) {
            descriptorBundleDir = explodeBundleIntoDirectory(zippedOSGiBundleFile, "descriptor-plugin-bundle-dir");
        }

        try (ZipInputStream zippedOSGiBundleFile = new ZipInputStream(FileUtils.openInputStream(pathOfFileInDefaultFiles("error-generating-descriptor-aware-test-plugin.osgi.jar")))) {
            errorGeneratingDescriptorBundleDir = explodeBundleIntoDirectory(zippedOSGiBundleFile, "error-generating-descriptor-plugin-bundle-dir");
        }

        try (ZipInputStream zippedOSGiBundleFile = new ZipInputStream(FileUtils.openInputStream(pathOfFileInDefaultFiles("exception-throwing-at-load-plugin.osgi.jar")))) {
            exceptionThrowingAtLoadDescriptorBundleDir = explodeBundleIntoDirectory(zippedOSGiBundleFile, "exception-throwing-at-load-plugin-bundle-dir");
        }

        try (ZipInputStream zippedOSGiBundleFile = new ZipInputStream(FileUtils.openInputStream(pathOfFileInDefaultFiles("valid-plugin-with-multiple-extensions.osgi.jar")))) {
            validMultipleExtensionPluginBundleDir = explodeBundleIntoDirectory(zippedOSGiBundleFile, "valid-plugin-with-multiple-extensions");
        }
        try (ZipInputStream zippedOSGiBundleFile = new ZipInputStream(FileUtils.openInputStream(pathOfFileInDefaultFiles("dumb.plugin.that.responds.with.classloader.name.osgi.jar")))) {
            pluginToTestClassloadPluginBundleDir = explodeBundleIntoDirectory(zippedOSGiBundleFile, "plugin-to-test-classloader");
        }
    }

    @After
    public void tearDown() {
        pluginOSGiFramework.stop();
    }

    @Test
    public void shouldLoadAValidGoPluginOSGiBundle() throws Exception {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(PLUGIN_ID, null, null, null, descriptorBundleDir, true));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        BundleContext context = bundle.getBundleContext();
        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), null);
        assertThat(allServiceReferences.length, is(1));

        try {
            GoPlugin service = (GoPlugin) context.getService(allServiceReferences[0]);
            service.pluginIdentifier();
            assertThat("@Load should have been called", getIntField(service, "loadCalled"), is(1));
        } catch (Exception e) {
            fail(String.format("pluginIdentifier should have been called. Exception: %s", e.getMessage()));
        }
    }

    @Test
    public void shouldLoadAValidGoPluginOSGiBundleAndShouldBeDiscoverableThroughPluginIDFilter() throws Exception {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(PLUGIN_ID, null, null, null, descriptorBundleDir, true));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        String filterByPluginID = String.format("(%s=%s)", "PLUGIN_ID", "testplugin.descriptorValidator");
        BundleContext context = bundle.getBundleContext();
        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), filterByPluginID);
        assertThat(allServiceReferences.length, is(1));

        try {
            GoPlugin service = (GoPlugin) context.getService(allServiceReferences[0]);
            service.pluginIdentifier();
        } catch (Exception e) {
            fail(String.format("pluginIdentifier should have been called. Exception: %s", e.getMessage()));
        }
    }

    @Test
    public void shouldHandleErrorGeneratedByAValidGoPluginOSGiBundleAtUsageTime() {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(PLUGIN_ID, null, null, null, errorGeneratingDescriptorBundleDir, true));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        assertThat(bundle.getState(), is(Bundle.ACTIVE));
        assertThat(bundleDescriptor.isInvalid(), is(false));

        ActionWithReturn<GoPlugin, Object> action = (goPlugin, goPluginDescriptor) -> {
            goPlugin.initializeGoApplicationAccessor(null);
            return null;
        };

        try {
            pluginOSGiFramework.doOn(GoPlugin.class, PLUGIN_ID, "extension-1", action);
            fail("Should Throw An Exception");
        } catch (Exception ex) {
            ex.printStackTrace();
            assertThat(ex.getCause() instanceof AbstractMethodError, is(true));
        }
    }

    @Test
    public void shouldPassInCorrectDescriptorToAction() {
        final GoPluginDescriptor goPluginDescriptor = new GoPluginDescriptor(PLUGIN_ID, null, null, null, descriptorBundleDir, true);
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(goPluginDescriptor);
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);
        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        ActionWithReturn<GoPlugin, Object> action = (plugin, pluginDescriptor) -> {
            assertThat(pluginDescriptor, is(goPluginDescriptor));
            plugin.pluginIdentifier();
            return null;
        };
        pluginOSGiFramework.doOn(GoPlugin.class, PLUGIN_ID, "notification", action);
    }

    @Test
    public void shouldUnloadALoadedPlugin() throws Exception {
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(PLUGIN_ID, null, null, null, descriptorBundleDir, true));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        BundleContext context = bundle.getBundleContext();

        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), null);
        assertThat(allServiceReferences.length, is(1));
        GoPlugin service = (GoPlugin) context.getService(allServiceReferences[0]);
        assertThat("@Load should have been called", getIntField(service, "loadCalled"), is(1));

        bundleDescriptor.setBundle(bundle);
        pluginOSGiFramework.unloadPlugin(bundleDescriptor);

        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        assertThat("@UnLoad should have been called", getIntField(service, "unloadCalled"), is(1));
    }

    @Test
    public void shouldMarkAPluginInvalidIfAtLoadOfAnyExtensionPointInItFails() {
        String id = "com.tw.go.exception.throwing.at.loadplugin";
        GoPluginBundleDescriptor pluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(id, null, null, null, exceptionThrowingAtLoadDescriptorBundleDir, true));
        registry.loadPlugin(pluginDescriptor);
        assertThat(pluginDescriptor.isInvalid(), is(false));

        pluginOSGiFramework.loadPlugin(pluginDescriptor);

        assertThat(pluginDescriptor.isInvalid(), is(true));
    }

    @Test
    public void shouldLoadAValidPluginWithMultipleExtensions_ImplementingDifferentExtensions() throws Exception {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor("valid-plugin-with-multiple-extensions", null, null, null, validMultipleExtensionPluginBundleDir, true));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);
        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        BundleContext context = bundle.getBundleContext();
        String taskExtensionFilter = String.format("(&(%s=%s)(%s=%s))", "PLUGIN_ID", "valid-plugin-with-multiple-extensions", Constants.BUNDLE_CATEGORY, "task");
        String analyticsExtensionFilter = String.format("(&(%s=%s)(%s=%s))", "PLUGIN_ID", "valid-plugin-with-multiple-extensions", Constants.BUNDLE_CATEGORY, "analytics");

        ServiceReference<?>[] taskExtensionServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), taskExtensionFilter);
        assertThat(taskExtensionServiceReferences.length, is(1));
        assertThat(((GoPlugin) context.getService(taskExtensionServiceReferences[0])).pluginIdentifier().getExtension(), is("task"));

        ServiceReference<?>[] analyticsExtensionServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), analyticsExtensionFilter);
        assertThat(analyticsExtensionServiceReferences.length, is(1));
        assertThat(((GoPlugin) context.getService(analyticsExtensionServiceReferences[0])).pluginIdentifier().getExtension(), is("analytics"));
    }

    @Test
    public void shouldSetCurrentThreadContextClassLoaderToBundleClassLoaderToAvoidDependenciesFromApplicationClassloaderMessingAroundWithThePluginBehavior() {
        systemEnvironment.setProperty("gocd.plugins.classloader.old", "false");
        final GoPluginDescriptor goPluginDescriptor = new GoPluginDescriptor("plugin.to.test.classloader", null, null, null, pluginToTestClassloadPluginBundleDir, true);
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(goPluginDescriptor);
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);
        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        ActionWithReturn<GoPlugin, Object> action = (plugin, pluginDescriptor) -> {
            assertThat(pluginDescriptor, is(goPluginDescriptor));
            assertThat(Thread.currentThread().getContextClassLoader().getClass().getCanonicalName(), is(BundleClassLoader.class.getCanonicalName()));
            plugin.pluginIdentifier();
            return null;
        };
        pluginOSGiFramework.doOn(GoPlugin.class, "plugin.to.test.classloader", "notification", action);
    }

    @Test
    public void shouldUseOldClassLoaderBehaviourWhenSystemPropertyIsSet() {
        systemEnvironment.setProperty("gocd.plugins.classloader.old", "true");
        final GoPluginDescriptor goPluginDescriptor = new GoPluginDescriptor("plugin.to.test.classloader", null, null, null, pluginToTestClassloadPluginBundleDir, true);
        final GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(goPluginDescriptor);
        registry.loadPlugin(descriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(descriptor);
        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        ActionWithReturn<GoPlugin, Object> action = (plugin, pluginDescriptor) -> {
            assertThat(pluginDescriptor, is(goPluginDescriptor));
            assertThat(Thread.currentThread().getContextClassLoader().getClass().getCanonicalName(), not(BundleClassLoader.class.getCanonicalName()));
            return null;
        };
        pluginOSGiFramework.doOn(GoPlugin.class, "plugin.to.test.classloader", "notification", action);
    }

    private int getIntField(Object service, String fieldName) {
        return Integer.parseInt(ReflectionUtil.getField(service, fieldName) + "");
    }

    private File pathOfFileInDefaultFiles(String filePath) {
        return new File(getClass().getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

    private File explodeBundleIntoDirectory(ZipInputStream src, String destinationDir) throws IOException {
        File destinationPluginBundleLocation = temporaryFolder.newFolder(destinationDir);
        new ZipUtil().unzip(src, destinationPluginBundleLocation);
        return destinationPluginBundleLocation;
    }

}
