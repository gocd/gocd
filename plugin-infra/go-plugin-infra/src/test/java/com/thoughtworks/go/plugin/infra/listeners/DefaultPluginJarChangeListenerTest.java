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
package com.thoughtworks.go.plugin.infra.listeners;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.plugin.infra.PluginLoader;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class DefaultPluginJarChangeListenerTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String PLUGIN_JAR_FILE_NAME = "descriptor-aware-test-plugin.jar";
    private File pluginDir;
    private File bundleDir;
    private DefaultPluginRegistry registry;
    private GoPluginOSGiManifestGenerator osgiManifestGenerator;
    private DefaultPluginJarChangeListener listener;
    private PluginLoader pluginLoader;
    private SystemEnvironment systemEnvironment;
    private GoPluginBundleDescriptorBuilder goPluginBundleDescriptorBuilder;

    @Before
    public void setUp() throws Exception {
        bundleDir = temporaryFolder.newFolder("bundleDir");
        pluginDir = temporaryFolder.newFolder("pluginDir");

        registry = mock(DefaultPluginRegistry.class);
        osgiManifestGenerator = mock(GoPluginOSGiManifest.DefaultGoPluginOSGiManifestCreator.class);
        pluginLoader = mock(PluginLoader.class);
        goPluginBundleDescriptorBuilder = mock(GoPluginBundleDescriptorBuilder.class);
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_ACTIVATOR_JAR_PATH)).thenReturn("defaultFiles/go-plugin-activator.jar");
        when(systemEnvironment.get(PLUGIN_BUNDLE_PATH)).thenReturn(bundleDir.getAbsolutePath());
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Linux");

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
    }

    @Test
    public void shouldCopyPluginToBundlePathAndInformRegistryAndUpdateTheOSGiManifestWhenAPluginIsAdded() throws Exception {
        String pluginId = "testplugin.descriptorValidator";
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId(pluginId, pluginJarFile.getAbsolutePath(), expectedBundleDirectory, true));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        when(registry.getPluginBundleByIdOrFileName(pluginId, PLUGIN_JAR_FILE_NAME)).thenReturn(null);
        doNothing().when(registry).loadPlugin(descriptor);

        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(expectedBundleDirectory.exists(), is(true));
        verify(registry).getPluginBundleByIdOrFileName(pluginId, PLUGIN_JAR_FILE_NAME);
        verify(registry).loadPlugin(descriptor);
        verify(osgiManifestGenerator).updateManifestOf(descriptor);
        verify(pluginLoader).loadPlugin(descriptor);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(registry);
        assertThat(new File(expectedBundleDirectory, "lib/go-plugin-activator.jar").exists(), is(true));
    }

    @Test
    public void shouldOverwriteAFileCalledGoPluginActivatorInLibWithOurOwnGoPluginActivatorEvenIfItExists() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);
        File activatorFileLocation = new File(expectedBundleDirectory, "lib/go-plugin-activator.jar");
        FileUtils.writeStringToFile(activatorFileLocation, "SOME-DATA", UTF_8);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginJarFile.getAbsolutePath(), expectedBundleDirectory, true));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        doNothing().when(registry).loadPlugin(descriptor);

        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(new File(expectedBundleDirectory, "lib/go-plugin-activator.jar").exists(), is(true));
        assertThat(FileUtils.readFileToString(activatorFileLocation, UTF_8), is(not("SOME-DATA")));
    }

    @Test
    public void shouldCopyPluginToBundlePathAndInformRegistryAndUpdateTheOSGiManifestWhenAPluginIsUpdated() throws Exception {
        DefaultPluginJarChangeListener spy = spy(listener);
        String pluginId = "plugin-id";
        String pluginJarFileName = "jarName";
        File pluginJarFile = mock(File.class);
        File oldPluginBundleDirectory = temporaryFolder.newFolder("bundleDir", "old-bundle");

        final File explodedDirectory = mock(File.class);
        doNothing().when(spy).explodePluginJarToBundleDir(pluginJarFile, explodedDirectory);
        doNothing().when(spy).installActivatorJarToBundleDir(explodedDirectory);

        GoPluginBundleDescriptor oldDescriptor = mock(GoPluginBundleDescriptor.class);
        Bundle oldBundle = mock(Bundle.class);
        when(oldDescriptor.bundle()).thenReturn(oldBundle);
        when(oldDescriptor.fileName()).thenReturn(pluginJarFileName);
        when(oldDescriptor.bundleLocation()).thenReturn(oldPluginBundleDirectory);

        GoPluginBundleDescriptor newDescriptor = mock(GoPluginBundleDescriptor.class);
        when(newDescriptor.id()).thenReturn(pluginId);
        when(newDescriptor.isInvalid()).thenReturn(false);
        when(newDescriptor.bundleLocation()).thenReturn(explodedDirectory);
        when(newDescriptor.fileName()).thenReturn(pluginJarFileName);
        when(newDescriptor.isCurrentOSValidForThisPlugin(systemEnvironment.getOperatingSystemFamilyName())).thenReturn(true);
        when(newDescriptor.isCurrentGocdVersionValidForThisPlugin()).thenReturn(true);
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(newDescriptor);

        when(registry.getPluginBundleByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldDescriptor);
        when(registry.unloadPlugin(newDescriptor)).thenReturn(oldDescriptor);
        doNothing().when(registry).loadPlugin(newDescriptor);

        spy.pluginJarUpdated(new PluginFileDetails(pluginJarFile, true));

        assertThat(oldPluginBundleDirectory.exists(), is(false));

        verify(registry).getPluginBundleByIdOrFileName(pluginId, pluginJarFileName);
        verify(registry).unloadPlugin(newDescriptor);
        verify(registry).loadPlugin(newDescriptor);
        verify(osgiManifestGenerator).updateManifestOf(newDescriptor);
        verify(pluginLoader).unloadPlugin(oldDescriptor);
        verify(pluginLoader).loadPlugin(newDescriptor);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(registry);
    }

    @Test
    public void shouldRemovePluginFromBundlePathAndInformRegistryWhenAPluginIsRemoved() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File removedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        Bundle bundle = mock(Bundle.class);
        GoPluginBundleDescriptor descriptorOfThePluginWhichWillBeRemoved = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginJarFile.getAbsolutePath(), removedBundleDirectory, true));
        descriptorOfThePluginWhichWillBeRemoved.setBundle(bundle);

        when(registry.getPluginBundleByIdOrFileName(null, descriptorOfThePluginWhichWillBeRemoved.fileName())).thenReturn(descriptorOfThePluginWhichWillBeRemoved);
        when(registry.unloadPlugin(descriptorOfThePluginWhichWillBeRemoved)).thenReturn(descriptorOfThePluginWhichWillBeRemoved);

        copyPluginToTheDirectory(bundleDir, PLUGIN_JAR_FILE_NAME);

        listener.pluginJarRemoved(new PluginFileDetails(pluginJarFile, true));

        verify(registry).unloadPlugin(descriptorOfThePluginWhichWillBeRemoved);
        verify(pluginLoader).unloadPlugin(descriptorOfThePluginWhichWillBeRemoved);
        assertThat(removedBundleDirectory.exists(), is(false));
    }

    @Test
    public void shouldNotTryAndUpdateManifestOfAnAddedInvalidPlugin() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        GoPluginBundleDescriptor descriptorForInvalidPlugin = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginJarFile.getAbsolutePath(), expectedBundleDirectory, true).markAsInvalid(
                asList("For a test"), null));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptorForInvalidPlugin);
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
        File pluginFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectoryForInvalidPlugin = new File(bundleDir, PLUGIN_JAR_FILE_NAME);
        File bundleDirectoryForOldPlugin = new File(bundleDir, "descriptor-aware-test-plugin-old.jar");
        FileUtils.forceMkdir(bundleDirectoryForOldPlugin);

        GoPluginBundleDescriptor descriptorForInvalidPlugin = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("testplugin.descriptorValidator", pluginFile.getAbsolutePath(), expectedBundleDirectoryForInvalidPlugin,
                true).markAsInvalid(
                asList("For a test"), null));

        Bundle oldBundle = mock(Bundle.class);
        GoPluginBundleDescriptor oldPluginDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("some.old.id", "some/path/to/plugin.jar", bundleDirectoryForOldPlugin, true).setBundle(oldBundle));

        when(goPluginBundleDescriptorBuilder.build(pluginFile, true)).thenReturn(descriptorForInvalidPlugin);
        when(registry.getPlugin(pluginId)).thenReturn(oldPluginDescriptor.descriptor());
        when(registry.unloadPlugin(descriptorForInvalidPlugin)).thenReturn(oldPluginDescriptor);
        doNothing().when(registry).loadPlugin(descriptorForInvalidPlugin);


        spy.pluginJarUpdated(new PluginFileDetails(pluginFile, true));

        assertThat(expectedBundleDirectoryForInvalidPlugin.exists(), is(true));
        assertThat(bundleDirectoryForOldPlugin.exists(), is(false));
        verify(registry).unloadPlugin(descriptorForInvalidPlugin);
        verify(pluginLoader).unloadPlugin(oldPluginDescriptor);
        verify(registry).loadPlugin(descriptorForInvalidPlugin);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(pluginLoader);
    }

    @Test(expected = RuntimeException.class)
    public void shouldFailToLoadAPluginWhenActivatorJarIsNotAvailable() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_ACTIVATOR_JAR_PATH)).thenReturn("some-path-which-does-not-exist.jar");


        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File bundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("some.old.id", pluginJarFile.getAbsolutePath(), bundleDirectory, true);
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(new GoPluginBundleDescriptor(descriptor));

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));
    }

    @Test
    public void shouldNotReplaceBundledPluginWhenExternalPluginIsAdded() {
        String pluginId = "external";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginBundleDescriptor externalPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginId, "1.0", null, pluginJarFile.getAbsolutePath(), new File(pluginJarFileName), false));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(externalPluginDescriptor);

        GoPluginBundleDescriptor bundledPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor("bundled", "1.0", null, null, null, true));
        when(registry.getPluginBundleByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(bundledPluginDescriptor);

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
    public void shouldNotUpdatePluginWhenThereIsExistingPluginWithSameId() {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginBundleDescriptor newPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginId, "1.0", null, pluginJarFile.getAbsolutePath(), new File(pluginJarFileName), true));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        GoPluginBundleDescriptor oldPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginId, "1.0", null, "location-old", new File("location-old"), true));
        when(registry.getPluginBundleByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldPluginDescriptor);

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
    public void shouldNotUpdateBundledPluginWithExternalPlugin() {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginBundleDescriptor newPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginId, "1.0", null, null, new File(pluginJarFileName), false));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        GoPluginBundleDescriptor oldPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginId, "1.0", null, null, null, true));
        when(registry.getPluginBundleByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldPluginDescriptor);

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
    public void shouldNotRemoveBundledPluginExternalPluginJarRemovedWithSameId() {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginBundleDescriptor oldPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginId, "1.0", null, null, null, true));
        when(registry.getPluginBundleByIdOrFileName(null, pluginJarFileName)).thenReturn(oldPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        spy.pluginJarRemoved(new PluginFileDetails(pluginJarFile, false));
        verify(registry, never()).unloadPlugin(oldPluginDescriptor);
        verify(pluginLoader, never()).unloadPlugin(oldPluginDescriptor);
    }

    @Test
    public void shouldNotLoadAPluginWhenCurrentOSIsNotAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXML() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Linux", "Mac OS X")), null,
                new File(PLUGIN_JAR_FILE_NAME), false));
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verifyZeroInteractions(pluginLoader);

        assertThat(descriptor.getStatus().getMessages().size(), is(1));
        assertThat(descriptor.getStatus().getMessages().get(0),
                is("Plugin with ID (some.old.id) is not valid: Incompatible with current operating system 'Windows'. Valid operating systems are: [Linux, Mac OS X]."));
    }

    @Test
    public void shouldNotLoadAPluginWhenCurrentOSIsNotAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXMLForUpdatePath() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);

        String pluginID = "some.id";
        GoPluginBundleDescriptor newPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginID, "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Mac OS X")), null,
                new File(PLUGIN_JAR_FILE_NAME), true));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        GoPluginBundleDescriptor oldPluginDescriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor(pluginID, "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Linux", "Mac OS X")), null, new File(PLUGIN_JAR_FILE_NAME), true));
        when(registry.getPluginBundleByIdOrFileName(pluginID, PLUGIN_JAR_FILE_NAME)).thenReturn(oldPluginDescriptor);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Linux");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(newPluginDescriptor);
        when(registry.unloadPlugin(newPluginDescriptor)).thenReturn(oldPluginDescriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(newPluginDescriptor);

        assertThat(newPluginDescriptor.getStatus().getMessages().size(), is(1));
        assertThat(newPluginDescriptor.getStatus().getMessages().get(0),
                is("Plugin with ID (some.id) is not valid: Incompatible with current operating system 'Linux'. Valid operating systems are: [Mac OS X]."));
    }

    @Test
    public void shouldLoadAPluginWhenCurrentOSIsAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXML() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Windows", "Linux")), null,
                new File(PLUGIN_JAR_FILE_NAME), false));
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(pluginLoader, times(1)).loadPlugin(descriptor);
    }

    @Test
    public void shouldLoadAPluginWhenAListOfTargetOSesIsNotDeclaredByThePluginInItsXML() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, null, null, null, null), null, new File(PLUGIN_JAR_FILE_NAME), false));
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(pluginLoader, times(1)).loadPlugin(descriptor);
    }

    @Test
    public void shouldNotLoadAPluginWhenTargetedGocdVersionIsGreaterThanCurrentGocdVersion() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, "9999.0.0", null, null, asList("Linux", "Mac OS X")), null,
                new File(PLUGIN_JAR_FILE_NAME), false));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verifyZeroInteractions(pluginLoader);

        assertThat(descriptor.getStatus().getMessages().size(), is(1));
        assertThat(descriptor.getStatus().getMessages().get(0),
                is("Plugin with ID (some.old.id) is not valid: Incompatible with GoCD version '" + CurrentGoCDVersion.getInstance().goVersion() + "'. Compatible version is: 9999.0.0."));
    }

    @Test
    public void shouldNotLoadAPluginWhenTargetedGocdVersionIsIncorrect() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, "9999.0.0.1.2", null, null, asList("Linux", "Mac OS X")), null,
                new File(PLUGIN_JAR_FILE_NAME), false));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verifyZeroInteractions(pluginLoader);

        assertThat(descriptor.getStatus().getMessages().size(), is(1));
        assertThat(descriptor.getStatus().getMessages().get(0),
                is("Plugin with ID (some.old.id) is not valid: Incorrect target gocd version(9999.0.0.1.2) specified."));
    }

    @Test
    public void shouldNotLoadAPluginWhenProvidedExtensionVersionByThePluginIsNotSupportedByCurrentGoCDVersion() throws IOException {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginDescriptor descriptor = new GoPluginDescriptor("some.old.id", "1.0", new GoPluginDescriptor.About(null, null, null, null, null, asList("Windows", "Linux")), null,
                new File(PLUGIN_JAR_FILE_NAME), false);
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(new GoPluginBundleDescriptor(descriptor));
    }

    private void copyPluginToTheDirectory(File destinationDir, String destinationFilenameOfPlugin) throws IOException {
        FileUtils.copyFile(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.jar"), new File(destinationDir, destinationFilenameOfPlugin));
    }

    private File pathOfFileInDefaultFiles(String filePath) {
        return new File(getClass().getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

}
