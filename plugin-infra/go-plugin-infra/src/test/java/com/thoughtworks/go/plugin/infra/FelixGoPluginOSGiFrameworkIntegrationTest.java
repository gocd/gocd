/*************************GO-LICENSE-START*********************************
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.FileUtil.recreateDirectory;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FelixGoPluginOSGiFrameworkIntegrationTest {
    private static final Random RANDOM = new Random();
    private FelixGoPluginOSGiFramework pluginOSGiFramework;
    private File TMP_DIR;
    private File descriptorBundleDir;
    private File errorGeneratingDescriptorBundleDir;
    private File exceptionThrowingAtLoadDescriptorBundleDir;
    private File validMultipleExtensionPluginBundleDir;
    private DefaultPluginRegistry registry;

    @Before
    public void setUp() throws Exception {
        TMP_DIR = new File("./tmp" + RANDOM.nextFloat());
        recreateDirectory(TMP_DIR);

        registry = new DefaultPluginRegistry();
        pluginOSGiFramework = new FelixGoPluginOSGiFramework(registry, new SystemEnvironment());
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
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(TMP_DIR);
        pluginOSGiFramework.stop();
    }

    @Test
    public void shouldLoadAValidGoPluginOSGiBundle() throws Exception {
        Bundle bundle = pluginOSGiFramework.loadPlugin(new GoPluginDescriptor(null, null, null, null, descriptorBundleDir, true));

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
    public void shouldNotifyListenersWhenPluginLoaded() {
        PluginChangeListener pluginChangeListener = mock(PluginChangeListener.class);
        pluginOSGiFramework.addPluginChangeListener(pluginChangeListener);
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(null, null, null, null, descriptorBundleDir, true);
        pluginOSGiFramework.loadPlugin(pluginDescriptor);
        verify(pluginChangeListener).pluginLoaded(pluginDescriptor);
    }

    @Test
    public void shouldNotifyListenersWhenPluginUnLoaded() {
        PluginChangeListener pluginChangeListener = mock(PluginChangeListener.class);
        pluginOSGiFramework.addPluginChangeListener(pluginChangeListener);
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(null, null, null, null, descriptorBundleDir, true);
        Bundle bundle = pluginOSGiFramework.loadPlugin(pluginDescriptor);
        pluginDescriptor.setBundle(bundle);

        pluginOSGiFramework.unloadPlugin(pluginDescriptor);
        verify(pluginChangeListener).pluginUnLoaded(pluginDescriptor);
    }

    @Test
    public void shouldLoadAValidGoPluginOSGiBundleAndShouldBeDiscoverableThroughSymbolicNameFilter() throws Exception {
        Bundle bundle = pluginOSGiFramework.loadPlugin(new GoPluginDescriptor(null, null, null, null, descriptorBundleDir, true));

        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        String filterBySymbolicName = String.format("(%s=%s)", Constants.BUNDLE_SYMBOLICNAME, "testplugin.descriptorValidator");
        BundleContext context = bundle.getBundleContext();
        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), filterBySymbolicName);
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
        Bundle bundle = pluginOSGiFramework.loadPlugin(new GoPluginDescriptor(null, null, null, null, errorGeneratingDescriptorBundleDir, true));
        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        ActionWithReturn<GoPlugin, Object> action = new ActionWithReturn<GoPlugin, Object>() {
            @Override
            public Object execute(GoPlugin goPlugin, GoPluginDescriptor goPluginDescriptor) {
                goPlugin.pluginIdentifier();
                return null;
            }
        };

        try {
            pluginOSGiFramework.doOn(GoPlugin.class, "testplugin.descriptorValidator", "CANNOT_FIND_EXTENSION_TYPE", action);
            fail("Should Throw An Exception");
        } catch (Exception ex) {
            assertThat(ex.getCause() instanceof AbstractMethodError, is(true));
        }
    }

    @Test
    public void shouldPassInCorrectDescriptorToAction() {
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("testplugin.descriptorValidator", null, null, null, descriptorBundleDir, true);
        Bundle bundle = pluginOSGiFramework.loadPlugin(descriptor);
        registry.loadPlugin(descriptor);
        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        ActionWithReturn<GoPlugin, Object> action = new ActionWithReturn<GoPlugin, Object>() {
            @Override
            public Object execute(GoPlugin plugin, GoPluginDescriptor pluginDescriptor) {
                assertThat(pluginDescriptor, is(descriptor));
                plugin.pluginIdentifier();
                return null;
            }
        };
        pluginOSGiFramework.doOn(GoPlugin.class, "testplugin.descriptorValidator", "notification", action);
    }

    @Test
    public void shouldUnloadALoadedPlugin() throws Exception {
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(null, null, null, null, descriptorBundleDir, true);
        Bundle bundle = pluginOSGiFramework.loadPlugin(pluginDescriptor);

        BundleContext context = bundle.getBundleContext();

        ServiceReference<?>[] allServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), null);
        assertThat(allServiceReferences.length, is(1));
        GoPlugin service = (GoPlugin) context.getService(allServiceReferences[0]);
        assertThat("@Load should have been called", getIntField(service, "loadCalled"), is(1));

        pluginDescriptor.setBundle(bundle);
        pluginOSGiFramework.unloadPlugin(pluginDescriptor);

        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));
        assertThat("@UnLoad should have been called", getIntField(service, "unloadCalled"), is(1));
    }

    @Test
    public void shouldMarkAPluginInvalidAnUnloadPluginIfAtLoadOfAnyExtensionPointInItFails() throws Exception {
        String id = "com.tw.go.exception.throwing.at.loadplugin";
        GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor(id, null, null, null, exceptionThrowingAtLoadDescriptorBundleDir, true);
        registry.loadPlugin(pluginDescriptor);
        assertThat(pluginDescriptor.isInvalid(), is(false));
        Bundle bundle = pluginOSGiFramework.loadPlugin(pluginDescriptor);
        assertThat(pluginDescriptor.isInvalid(),is(true));
        assertThat(bundle.getState(),is(Bundle.UNINSTALLED));
    }

    @Test
    public void shouldLoadAValidPluginWithMultipleExtensions_ImplementingDifferentExtensions() throws Exception {
        Bundle bundle = pluginOSGiFramework.loadPlugin(new GoPluginDescriptor(null, null, null, null, validMultipleExtensionPluginBundleDir, true));
        assertThat(bundle.getState(), is(Bundle.ACTIVE));

        BundleContext context = bundle.getBundleContext();
        String taskExtensionFilter = String.format("(&(%s=%s)(%s=%s))", Constants.BUNDLE_SYMBOLICNAME, "valid-plugin-with-multiple-extensions", Constants.BUNDLE_CATEGORY, "task");
        String analyticsExtensionFilter = String.format("(&(%s=%s)(%s=%s))", Constants.BUNDLE_SYMBOLICNAME, "valid-plugin-with-multiple-extensions", Constants.BUNDLE_CATEGORY, "analytics");

        ServiceReference<?>[] taskExtensionServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), taskExtensionFilter);
        assertThat(taskExtensionServiceReferences.length, is(1));
        assertThat(((GoPlugin) context.getService(taskExtensionServiceReferences[0])).pluginIdentifier().getExtension(), is("task"));

        ServiceReference<?>[] analyticsExtensionServiceReferences = context.getServiceReferences(GoPlugin.class.getCanonicalName(), analyticsExtensionFilter);
        assertThat(analyticsExtensionServiceReferences.length, is(1));
        assertThat(((GoPlugin) context.getService(analyticsExtensionServiceReferences[0])).pluginIdentifier().getExtension(), is("analytics"));
    }

    private int getIntField(Object service, String fieldName) {
        return Integer.parseInt(ReflectionUtil.getField(service, fieldName) + "");
    }

    private File pathOfFileInDefaultFiles(String filePath) {
        return new File(getClass().getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

    private File explodeBundleIntoDirectory(ZipInputStream src, String destinationDir) throws IOException, URISyntaxException {
        File destinationPluginBundleLocation = new File(TMP_DIR, destinationDir);
        destinationPluginBundleLocation.mkdirs();
        new ZipUtil().unzip(src, destinationPluginBundleLocation);
        return destinationPluginBundleLocation;
    }
}
