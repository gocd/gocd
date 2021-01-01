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
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.apache.felix.framework.util.FelixConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class FelixGoPluginOSGiFrameworkIntegrationTest {
    private FelixGoPluginOSGiFramework pluginOSGiFramework;
    private File descriptorBundleDir;
    private File errorGeneratingDescriptorBundleDir;
    private File exceptionThrowingAtLoadDescriptorBundleDir;
    private File validMultipleExtensionPluginBundleDir;
    private File pluginToTestClassloadPluginBundleDir;
    private DefaultPluginRegistry registry;
    private SystemEnvironment systemEnvironment;
    private static final String PLUGIN_ID = "testplugin.descriptorValidator";
    private FileHelper temporaryFolder;

    @BeforeEach
    void setUp(@TempDir File rootDir) throws Exception {
        temporaryFolder = new FileHelper(rootDir);
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

    @AfterEach
    void tearDown() {
        pluginOSGiFramework.stop();
    }

    @Test
    void shouldLoadAValidGoPluginOSGiBundle() throws Exception {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(PLUGIN_ID, descriptorBundleDir));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        assertThat(bundle.getState()).isEqualTo(Bundle.ACTIVE);

        BundleContext context = bundle.getBundleContext();
        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), null);
        assertThat(allServiceReferences.length).isEqualTo(1);

        try {
            GoPlugin service = (GoPlugin) context.getService(allServiceReferences[0]);
            service.pluginIdentifier();
            assertThat(getIntField(service, "loadCalled")).as("@Load should have been called").isEqualTo(1);
        } catch (Exception e) {
            fail(String.format("pluginIdentifier should have been called. Exception: %s", e.getMessage()));
        }
    }

    private GoPluginDescriptor getPluginDescriptor(String pluginId, File descriptorBundleDir) {
        return GoPluginDescriptor.builder().id(pluginId)
                .bundleLocation(descriptorBundleDir)
                .isBundledPlugin(true)
                .build();
    }

    @Test
    void shouldLoadAValidGoPluginOSGiBundleAndShouldBeDiscoverableThroughPluginIDFilter() throws Exception {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(PLUGIN_ID, descriptorBundleDir));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        assertThat(bundle.getState()).isEqualTo(Bundle.ACTIVE);

        String filterByPluginID = String.format("(%s=%s)", "PLUGIN_ID", "testplugin.descriptorValidator");
        BundleContext context = bundle.getBundleContext();
        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), filterByPluginID);
        assertThat(allServiceReferences.length).isEqualTo(1);

        try {
            GoPlugin service = (GoPlugin) context.getService(allServiceReferences[0]);
            service.pluginIdentifier();
        } catch (Exception e) {
            fail(String.format("pluginIdentifier should have been called. Exception: %s", e.getMessage()));
        }
    }

    @Test
    void shouldHandleErrorGeneratedByAValidGoPluginOSGiBundleAtUsageTime() {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(PLUGIN_ID, errorGeneratingDescriptorBundleDir));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        assertThat(bundle.getState()).isEqualTo(Bundle.ACTIVE);
        assertThat(bundleDescriptor.isInvalid()).isFalse();

        ActionWithReturn<GoPlugin, Object> action = (goPlugin, goPluginDescriptor) -> {
            goPlugin.initializeGoApplicationAccessor(null);
            return null;
        };

        try {
            pluginOSGiFramework.doOn(GoPlugin.class, PLUGIN_ID, "extension-1", action);
            fail("Should Throw An Exception");
        } catch (Exception ex) {
            ex.printStackTrace();
            assertThat(ex.getCause() instanceof AbstractMethodError).isTrue();
        }
    }

    @Test
    void shouldPassInCorrectDescriptorToAction() {
        final GoPluginDescriptor goPluginDescriptor = getPluginDescriptor(PLUGIN_ID, descriptorBundleDir);
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(goPluginDescriptor);
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);
        assertThat(bundle.getState()).isEqualTo(Bundle.ACTIVE);

        ActionWithReturn<GoPlugin, Object> action = (plugin, pluginDescriptor) -> {
            assertThat(pluginDescriptor).isEqualTo(goPluginDescriptor);
            plugin.pluginIdentifier();
            return null;
        };
        pluginOSGiFramework.doOn(GoPlugin.class, PLUGIN_ID, "notification", action);
    }

    @Test
    void shouldUnloadALoadedPlugin() throws Exception {
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(PLUGIN_ID, descriptorBundleDir));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);

        BundleContext context = bundle.getBundleContext();

        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), null);
        assertThat(allServiceReferences.length).isEqualTo(1);
        GoPlugin service = (GoPlugin) context.getService(allServiceReferences[0]);
        assertThat(getIntField(service, "loadCalled")).as("@Load should have been called").isEqualTo(1);

        bundleDescriptor.setBundle(bundle);
        pluginOSGiFramework.unloadPlugin(bundleDescriptor);

        assertThat(bundle.getState()).isEqualTo(Bundle.UNINSTALLED);
        assertThat(getIntField(service, "unloadCalled")).as("@UnLoad should have been called").isEqualTo(1);
    }

    @Test
    void shouldMarkAPluginInvalidIfAtLoadOfAnyExtensionPointInItFails() {
        String id = "com.tw.go.exception.throwing.at.loadplugin";
        GoPluginBundleDescriptor pluginDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(id, exceptionThrowingAtLoadDescriptorBundleDir));
        registry.loadPlugin(pluginDescriptor);
        assertThat(pluginDescriptor.isInvalid()).isFalse();

        pluginOSGiFramework.loadPlugin(pluginDescriptor);

        assertThat(pluginDescriptor.isInvalid()).isTrue();
    }

    @Test
    void shouldLoadAValidPluginWithMultipleExtensions_ImplementingDifferentExtensions() throws Exception {
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor("valid-plugin-with-multiple-extensions", validMultipleExtensionPluginBundleDir));
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);
        assertThat(bundle.getState()).isEqualTo(Bundle.ACTIVE);

        BundleContext context = bundle.getBundleContext();
        String taskExtensionFilter = String.format("(&(%s=%s)(%s=%s))", "PLUGIN_ID", "valid-plugin-with-multiple-extensions", Constants.BUNDLE_CATEGORY, "task");
        String analyticsExtensionFilter = String.format("(&(%s=%s)(%s=%s))", "PLUGIN_ID", "valid-plugin-with-multiple-extensions", Constants.BUNDLE_CATEGORY, "analytics");

        ServiceReference<?>[] taskExtensionServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), taskExtensionFilter);
        assertThat(taskExtensionServiceReferences.length).isEqualTo(1);
        assertThat(((GoPlugin) context.getService(taskExtensionServiceReferences[0])).pluginIdentifier().getExtension()).isEqualTo("task");

        ServiceReference<?>[] analyticsExtensionServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), analyticsExtensionFilter);
        assertThat(analyticsExtensionServiceReferences.length).isEqualTo(1);
        assertThat(((GoPlugin) context.getService(analyticsExtensionServiceReferences[0])).pluginIdentifier().getExtension()).isEqualTo("analytics");
    }

    @Test
    void shouldSetCurrentThreadContextClassLoaderToBundleClassLoaderToAvoidDependenciesFromApplicationClassloaderMessingAroundWithThePluginBehavior() {
        systemEnvironment.setProperty("gocd.plugins.classloader.old", "false");
        final GoPluginDescriptor goPluginDescriptor = getPluginDescriptor("plugin.to.test.classloader", pluginToTestClassloadPluginBundleDir);
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(goPluginDescriptor);
        registry.loadPlugin(bundleDescriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(bundleDescriptor);
        assertThat(bundle.getState()).isEqualTo(Bundle.ACTIVE);

        ActionWithReturn<GoPlugin, Object> action = (plugin, pluginDescriptor) -> {
            assertThat(pluginDescriptor).isEqualTo(goPluginDescriptor);
            assertThat(Thread.currentThread().getContextClassLoader().getClass().getCanonicalName()).isEqualTo(BundleClassLoader.class.getCanonicalName());
            plugin.pluginIdentifier();
            return null;
        };
        pluginOSGiFramework.doOn(GoPlugin.class, "plugin.to.test.classloader", "notification", action);
    }

    @Test
    void shouldUseOldClassLoaderBehaviourWhenSystemPropertyIsSet() {
        systemEnvironment.setProperty("gocd.plugins.classloader.old", "true");
        final GoPluginDescriptor goPluginDescriptor = getPluginDescriptor("plugin.to.test.classloader", pluginToTestClassloadPluginBundleDir);
        final GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(goPluginDescriptor);
        registry.loadPlugin(descriptor);
        Bundle bundle = pluginOSGiFramework.loadPlugin(descriptor);
        assertThat(bundle.getState()).isEqualTo(Bundle.ACTIVE);

        ActionWithReturn<GoPlugin, Object> action = (plugin, pluginDescriptor) -> {
            assertThat(pluginDescriptor).isEqualTo(goPluginDescriptor);
            assertThat(Thread.currentThread().getContextClassLoader().getClass().getCanonicalName()).isNotEqualTo(BundleClassLoader.class.getCanonicalName());
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
