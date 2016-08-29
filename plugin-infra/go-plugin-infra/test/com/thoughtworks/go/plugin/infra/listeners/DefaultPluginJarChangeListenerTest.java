/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.infra.listeners;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.thoughtworks.go.util.OperatingSystem;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.plugin.api.info.PluginDescriptorAware;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ExceptionHandler;
import com.thoughtworks.go.plugin.infra.GoPluginFrameworkException;
import com.thoughtworks.go.plugin.infra.GoPluginOSGiFramework;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptorBuilder;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginOSGiManifest;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginOSGiManifestGenerator;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DefaultPluginJarChangeListenerTest {
    private static final String TEST_BUNDLES_DIR = "test-bundles-dir";
    private static final String TEST_PLUGINS_DIR = "test-plugins-dir";
    private File PLUGIN_DIR;
    private File BUNDLE_DIR;
    private DefaultPluginRegistry registry;
    private GoPluginOSGiManifestGenerator osgiManifestGenerator;
    private DefaultPluginJarChangeListener listener;
    private GoPluginOSGiFramework osgiFramework;
    private SystemEnvironment systemEnvironment;
    private GoPluginDescriptorBuilder goPluginDescriptorBuilder;

    @Before
    public void setUp() throws Exception {
        BUNDLE_DIR = new File(TEST_BUNDLES_DIR);
        PLUGIN_DIR = new File(TEST_PLUGINS_DIR);

        registry = mock(DefaultPluginRegistry.class);
        osgiManifestGenerator = mock(GoPluginOSGiManifest.DefaultGoPluginOSGiManifestCreator.class);
        osgiFramework = mock(GoPluginOSGiFramework.class);
        goPluginDescriptorBuilder = mock(GoPluginDescriptorBuilder.class);
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_ACTIVATOR_JAR_PATH)).thenReturn("defaultFiles/go-plugin-activator.jar");
        when(systemEnvironment.get(PLUGIN_BUNDLE_PATH)).thenReturn(TEST_BUNDLES_DIR);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Linux");

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(PLUGIN_DIR);
        FileUtils.deleteQuietly(BUNDLE_DIR);
    }

    @Test
    public void shouldCopyPluginToBundlePathAndInformRegistryAndUpdateTheOSGiManifestWhenAPluginIsAdded() throws Exception {
        String pluginId = "testplugin.descriptorValidator";
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);
        File expectedBundleDirectory = new File(TEST_BUNDLES_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId(pluginId, pluginJarFile.getAbsolutePath(), expectedBundleDirectory, true);
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(null);
        doNothing().when(registry).loadPlugin(descriptor);

        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(expectedBundleDirectory.exists(), is(true));
        verify(registry).getPluginByIdOrFileName(pluginId, pluginJarFileName);
        verify(registry).loadPlugin(descriptor);
        verify(osgiManifestGenerator).updateManifestOf(descriptor);
        verify(osgiFramework).loadPlugin(descriptor);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(registry);
        assertThat(new File(expectedBundleDirectory, "lib/go-plugin-activator.jar").exists(), is(true));
    }

    @Test
    public void shouldOverwriteAFileCalledGoPluginActivatorInLibWithOurOwnGoPluginActivatorEvenIfItExists() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);
        File expectedBundleDirectory = new File(TEST_BUNDLES_DIR, pluginJarFileName);
        File activatorFileLocation = new File(expectedBundleDirectory, "lib/go-plugin-activator.jar");
        FileUtils.writeStringToFile(activatorFileLocation, "SOME-DATA");

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginJarFile.getAbsolutePath(), expectedBundleDirectory, true);
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        doNothing().when(registry).loadPlugin(descriptor);

        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(new File(expectedBundleDirectory, "lib/go-plugin-activator.jar").exists(), is(true));
        assertThat(FileUtils.readFileToString(activatorFileLocation), is(not("SOME-DATA")));
    }

    @Test
    public void shouldCopyPluginToBundlePathAndInformRegistryAndUpdateTheOSGiManifestWhenAPluginIsUpdated() throws Exception {
        DefaultPluginJarChangeListener spy = spy(listener);
        String pluginId = "plugin-id";
        String pluginJarFileName = "jarName";
        File pluginJarFile = mock(File.class);
        File oldPluginBundleDirectory = new File(TEST_BUNDLES_DIR, "old-bundle");
        FileUtils.forceMkdir(oldPluginBundleDirectory);

        final File explodedDirectory = mock(File.class);
        doNothing().when(spy).explodePluginJarToBundleDir(pluginJarFile, explodedDirectory);
        doNothing().when(spy).installActivatorJarToBundleDir(explodedDirectory);

        GoPluginDescriptor oldDescriptor = mock(GoPluginDescriptor.class);
        Bundle oldBundle = mock(Bundle.class);
        when(oldDescriptor.bundle()).thenReturn(oldBundle);
        when(oldDescriptor.fileName()).thenReturn(pluginJarFileName);
        when(oldDescriptor.bundleLocation()).thenReturn(oldPluginBundleDirectory);

        GoPluginDescriptor newDescriptor = mock(GoPluginDescriptor.class);
        when(newDescriptor.id()).thenReturn(pluginId);
        when(newDescriptor.isInvalid()).thenReturn(false);
        when(newDescriptor.bundleLocation()).thenReturn(explodedDirectory);
        when(newDescriptor.fileName()).thenReturn(pluginJarFileName);
        when(newDescriptor.isCurrentOSValidForThisPlugin(systemEnvironment.getOperatingSystemFamilyName())).thenReturn(true);
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(newDescriptor);

        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldDescriptor);
        when(registry.unloadPlugin(newDescriptor)).thenReturn(oldDescriptor);
        doNothing().when(registry).loadPlugin(newDescriptor);

        spy.pluginJarUpdated(new PluginFileDetails(pluginJarFile, true));

        assertThat(oldPluginBundleDirectory.exists(), is(false));

        verify(registry).getPluginByIdOrFileName(pluginId, pluginJarFileName);
        verify(registry).unloadPlugin(newDescriptor);
        verify(registry).loadPlugin(newDescriptor);
        verify(osgiManifestGenerator).updateManifestOf(newDescriptor);
        verify(osgiFramework).unloadPlugin(oldDescriptor);
        verify(osgiFramework).loadPlugin(newDescriptor);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(registry);
    }

    @Test
    public void shouldRemovePluginFromBundlePathAndInformRegistryWhenAPluginIsRemoved() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);
        File removedBundleDirectory = new File(BUNDLE_DIR, pluginJarFileName);

        Bundle bundle = mock(Bundle.class);
        GoPluginDescriptor descriptorOfThePluginWhichWillBeRemoved = GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginJarFile.getAbsolutePath(), removedBundleDirectory, true);
        descriptorOfThePluginWhichWillBeRemoved.setBundle(bundle);

        when(registry.getPluginByIdOrFileName(null,descriptorOfThePluginWhichWillBeRemoved.fileName())).thenReturn(descriptorOfThePluginWhichWillBeRemoved);
        when(registry.unloadPlugin(descriptorOfThePluginWhichWillBeRemoved)).thenReturn(descriptorOfThePluginWhichWillBeRemoved);

        copyPluginToTheDirectory(BUNDLE_DIR, pluginJarFileName);

        listener.pluginJarRemoved(new PluginFileDetails(pluginJarFile, true));

        verify(registry).unloadPlugin(descriptorOfThePluginWhichWillBeRemoved);
        verify(osgiFramework).unloadPlugin(descriptorOfThePluginWhichWillBeRemoved);
        assertThat(removedBundleDirectory.exists(), is(false));
    }

    @Test
    public void shouldNotTryAndUpdateManifestOfAnAddedInvalidPlugin() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);
        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        File expectedBundleDirectory = new File(TEST_BUNDLES_DIR, pluginJarFileName);

        GoPluginDescriptor descriptorForInvalidPlugin = GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginJarFile.getAbsolutePath(), expectedBundleDirectory, true).markAsInvalid(
                asList("For a test"), null);
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptorForInvalidPlugin);
        doNothing().when(registry).loadPlugin(descriptorForInvalidPlugin);


        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(expectedBundleDirectory.exists(), is(true));
        verify(registry).loadPlugin(descriptorForInvalidPlugin);
        verifyNoMoreInteractions(osgiManifestGenerator);
    }

    @Test
    public void shouldNotTryAndUpdateManifestOfAnUpdatedInvalidPlugin() throws Exception {
        DefaultPluginJarChangeListener spy = spy(listener);
        String pluginId = "plugin-id";
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginFile = new File(PLUGIN_DIR, pluginJarFileName);
        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        File expectedBundleDirectoryForInvalidPlugin = new File(TEST_BUNDLES_DIR, pluginJarFileName);
        File bundleDirectoryForOldPlugin = new File(TEST_BUNDLES_DIR, "descriptor-aware-test-plugin-old.jar");
        FileUtils.forceMkdir(bundleDirectoryForOldPlugin);

        GoPluginDescriptor descriptorForInvalidPlugin = GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginFile.getAbsolutePath(), expectedBundleDirectoryForInvalidPlugin,
                true).markAsInvalid(
                asList("For a test"), null);

        Bundle oldBundle = mock(Bundle.class);
        GoPluginDescriptor oldPluginDescriptor = GoPluginDescriptor.usingId("some.old.id", "some/path/to/plugin.jar", bundleDirectoryForOldPlugin, true).setBundle(oldBundle);

        when(goPluginDescriptorBuilder.build(pluginFile, true)).thenReturn(descriptorForInvalidPlugin);
        when(registry.getPlugin(pluginId)).thenReturn(oldPluginDescriptor);
        when(registry.unloadPlugin(descriptorForInvalidPlugin)).thenReturn(oldPluginDescriptor);
        doNothing().when(registry).loadPlugin(descriptorForInvalidPlugin);


        spy.pluginJarUpdated(new PluginFileDetails(pluginFile, true));

        assertThat(expectedBundleDirectoryForInvalidPlugin.exists(), CoreMatchers.is(true));
        assertThat(bundleDirectoryForOldPlugin.exists(), CoreMatchers.is(false));
        verify(registry).unloadPlugin(descriptorForInvalidPlugin);
        verify(osgiFramework).unloadPlugin(oldPluginDescriptor);
        verify(registry).loadPlugin(descriptorForInvalidPlugin);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(osgiFramework);
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailToLoadAPluginWhenActivatorJarIsNotAvailable() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_ACTIVATOR_JAR_PATH)).thenReturn("some-path-which-does-not-exist.jar");


        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);
        File bundleDirectory = new File(TEST_BUNDLES_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("some.old.id", pluginJarFile.getAbsolutePath(), bundleDirectory, true);
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));
    }

    @Test
    public void shouldNotReplaceBundledPluginWhenExternalPluginIsAdded() throws Exception {
        String pluginId = "external";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginDescriptor externalPluginDescriptor = new GoPluginDescriptor(pluginId, "1.0", null, pluginJarFile.getAbsolutePath(), new File(pluginJarFileName), false);
        when(goPluginDescriptorBuilder.build(pluginJarFile, false)).thenReturn(externalPluginDescriptor);

        GoPluginDescriptor bundledPluginDescriptor = new GoPluginDescriptor("bundled", "1.0", null, null, null, true);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(bundledPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Found bundled plugin with ID: [bundled], external plugin could not be loaded"));
        }
        verify(spy, never()).explodePluginJarToBundleDir(pluginJarFile, externalPluginDescriptor.bundleLocation());
    }

    @Test
    public void shouldNotUpdatePluginWhenThereIsExistingPluginWithSameId() throws Exception {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginDescriptor newPluginDescriptor = new GoPluginDescriptor(pluginId, "1.0", null, pluginJarFile.getAbsolutePath(), new File(pluginJarFileName), true);
        when(goPluginDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        GoPluginDescriptor oldPluginDescriptor = new GoPluginDescriptor(pluginId, "1.0", null, "location-old", new File("location-old"), true);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            spy.pluginJarUpdated(new PluginFileDetails(pluginJarFile, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Found another plugin with ID: plugin-id"));
        }
        verify(spy, never()).explodePluginJarToBundleDir(pluginJarFile, newPluginDescriptor.bundleLocation());

    }

    @Test
    public void shouldNotUpdateBundledPluginWithExternalPlugin() throws Exception {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginDescriptor newPluginDescriptor = new GoPluginDescriptor(pluginId, "1.0", null, null, new File(pluginJarFileName), false);
        when(goPluginDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        GoPluginDescriptor oldPluginDescriptor = new GoPluginDescriptor(pluginId, "1.0", null, null, null, true);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            spy.pluginJarUpdated(new PluginFileDetails(pluginJarFile, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Found bundled plugin with ID: [plugin-id], external plugin could not be loaded"));
        }
        verify(spy, never()).explodePluginJarToBundleDir(pluginJarFile, newPluginDescriptor.bundleLocation());
    }

    @Test
    public void shouldNotRemoveBundledPluginExternalPluginJarRemovedWithSameId() throws Exception {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginDescriptor oldPluginDescriptor = new GoPluginDescriptor(pluginId, "1.0", null, null, null, true);
        when(registry.getPluginByIdOrFileName(null, pluginJarFileName)).thenReturn(oldPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        spy.pluginJarRemoved(new PluginFileDetails(pluginJarFile, false));
        verify(registry, never()).unloadPlugin(oldPluginDescriptor);
        verify(osgiFramework, never()).unloadPlugin(oldPluginDescriptor);
    }

    @Test
    public void shouldNotLoadAPluginWhenCurrentOSIsNotAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXML() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Linux", "Mac OS X")), null,
                new File(pluginJarFileName), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verifyZeroInteractions(osgiFramework);

        assertThat(descriptor.getStatus().getMessages().size(), is(1));
        assertThat(descriptor.getStatus().getMessages().get(0),
                is("Plugin with ID (some.old.id) is not valid: Incompatible with current operating system 'Windows'. Valid operating systems are: [Linux, Mac OS X]."));
    }

    @Test
    public void shouldNotLoadAPluginWhenCurrentOSIsNotAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXMLForUpdatePath() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);
        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);

        String pluginID = "some.id";
        GoPluginDescriptor newPluginDescriptor = new GoPluginDescriptor(pluginID, "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Mac OS X")), null,
                new File(pluginJarFileName), true);
        when(goPluginDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        GoPluginDescriptor oldPluginDescriptor = new GoPluginDescriptor(pluginID, "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Linux", "Mac OS X")), null, new File(pluginJarFileName), true);
        when(registry.getPluginByIdOrFileName(pluginID, pluginJarFileName)).thenReturn(oldPluginDescriptor);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Linux");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(newPluginDescriptor);
        when(registry.unloadPlugin(newPluginDescriptor)).thenReturn(oldPluginDescriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarUpdated(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(newPluginDescriptor);

        assertThat(newPluginDescriptor.getStatus().getMessages().size(), is(1));
        assertThat(newPluginDescriptor.getStatus().getMessages().get(0),
                is("Plugin with ID (some.id) is not valid: Incompatible with current operating system 'Linux'. Valid operating systems are: [Mac OS X]."));
    }

    @Test
    public void shouldLoadAPluginWhenCurrentOSIsAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXML() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Windows", "Linux")), null,
                new File(pluginJarFileName), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).loadPlugin(descriptor);
    }

    @Test
    public void shouldLoadAPluginWhenAListOfTargetOSesIsNotDeclaredByThePluginInItsXML() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, null, null, null, null), null, new File(pluginJarFileName), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).loadPlugin(descriptor);
    }

    @Test
    public void shouldLoadAPluginAndProvidePluginDescriptorIfThePluginImplementsDescriptorAware() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0",
                new GoPluginDescriptor.About(null, null, null, null, null, null), null, new File(pluginJarFileName), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        when(osgiFramework.hasReferenceFor(PluginDescriptorAware.class,descriptor.id())).thenReturn(true);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return null;
            }
        }).when(osgiFramework).doOnAllWithExceptionHandlingForPlugin(
                eq(PluginDescriptorAware.class), eq(descriptor.id()), Matchers.<Action<PluginDescriptorAware>>anyObject(),
                Matchers.<ExceptionHandler<PluginDescriptorAware>>anyObject());

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).hasReferenceFor(PluginDescriptorAware.class, descriptor.id());
        verify(osgiFramework, times(1)).doOnAllWithExceptionHandlingForPlugin(eq(PluginDescriptorAware.class),
                eq(descriptor.id()), Matchers.<Action<PluginDescriptorAware>>anyObject()
                ,Matchers.<ExceptionHandler<PluginDescriptorAware>>anyObject());
    }

    @Test
    public void shouldNotProvidePluginDescriptorIfThePluginIsInvalidatedDuringLoad() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        final GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0",
                new GoPluginDescriptor.About(null, null, null, null, null, null), null, new File(pluginJarFileName), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        when(osgiFramework.hasReferenceFor(PluginDescriptorAware.class,descriptor.id())).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                descriptor.markAsInvalid(Arrays.asList("Marking invalid for test"),new Exception("dummy test exception"));
                return null;
            }
        }).when(osgiFramework).loadPlugin(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, never()).hasReferenceFor(PluginDescriptorAware.class,descriptor.id());
        verify(osgiFramework, never()).doOnAllForPlugin(eq(PluginDescriptorAware.class), eq(descriptor.id()), Matchers.<Action<PluginDescriptorAware>>anyObject());
    }

    @Test
    public void shouldNotThrowExceptionIfThePluginImplementsDescriptorAwareIsNotAvailable() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0",
                new GoPluginDescriptor.About(null, null, null, null, null, null), null, new File(pluginJarFileName), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        when(osgiFramework.hasReferenceFor(PluginDescriptorAware.class,descriptor.id())).thenReturn(false);

        doThrow(new GoPluginFrameworkException("Failed to find service reference")).when(osgiFramework).
                doOnAllForPlugin(eq(PluginDescriptorAware.class), eq(descriptor.id()), Matchers.<Action<PluginDescriptorAware>>anyObject());

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).hasReferenceFor(PluginDescriptorAware.class,descriptor.id());
        verify(osgiFramework, never()).doOnAllForPlugin(eq(PluginDescriptorAware.class), eq(descriptor.id()), Matchers.<Action<PluginDescriptorAware>>anyObject());
    }

    @Test
    public void pluginDescriptorAwareCallbackErrorShouldNotBeFatal() throws Exception {
        String pluginJarFileName = "descriptor-aware-test-plugin.jar";
        File pluginJarFile = new File(PLUGIN_DIR, pluginJarFileName);

        copyPluginToTheDirectory(PLUGIN_DIR, pluginJarFileName);
        GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0",
                new GoPluginDescriptor.About(null, null, null, null, null, null), null, new File(pluginJarFileName), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        when(osgiFramework.hasReferenceFor(PluginDescriptorAware.class,descriptor.id())).thenReturn(true);

        doThrow(new RuntimeException("Exception in plugin descriptor")).when(osgiFramework).doOnAllForPlugin(eq(PluginDescriptorAware.class), eq(descriptor.id()),
                Matchers.<Action<PluginDescriptorAware>>anyObject());

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, osgiFramework, goPluginDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).loadPlugin(descriptor);
        verify(osgiFramework, times(1)).hasReferenceFor(PluginDescriptorAware.class,descriptor.id());
        verify(osgiFramework, times(1)).doOnAllWithExceptionHandlingForPlugin(eq(PluginDescriptorAware.class), eq(descriptor.id()), Matchers.<Action<PluginDescriptorAware>>anyObject(),
                Matchers.<ExceptionHandler<PluginDescriptorAware>>anyObject());
    }

    private void copyPluginToTheDirectory(File destinationDir, String destinationFilenameOfPlugin) throws IOException, URISyntaxException {
        FileUtils.copyFile(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.jar"), new File(destinationDir, destinationFilenameOfPlugin));
    }

    private File pathOfFileInDefaultFiles(String filePath) {
        return new File(getClass().getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

}
