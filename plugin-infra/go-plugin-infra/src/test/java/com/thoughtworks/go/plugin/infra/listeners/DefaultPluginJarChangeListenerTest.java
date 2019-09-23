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
import com.thoughtworks.go.plugin.FileHelper;
import com.thoughtworks.go.plugin.infra.PluginLoader;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.plugininfo.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_ACTIVATOR_JAR_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_WORK_DIR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultPluginJarChangeListenerTest {
    private static final String PLUGIN_JAR_FILE_NAME = "descriptor-aware-test-plugin.jar";
    private File pluginDir;
    private File bundleDir;
    private DefaultPluginRegistry registry;
    private GoPluginOSGiManifestGenerator osgiManifestGenerator;
    private DefaultPluginJarChangeListener listener;
    private PluginLoader pluginLoader;
    private SystemEnvironment systemEnvironment;
    private GoPluginBundleDescriptorBuilder goPluginBundleDescriptorBuilder;
    private FileHelper temporaryFolder;

    @BeforeEach
    void setUp(@TempDir File rootDir) {
        temporaryFolder = new FileHelper(rootDir);
        bundleDir = temporaryFolder.newFolder("bundleDir");
        pluginDir = temporaryFolder.newFolder("pluginDir");

        registry = mock(DefaultPluginRegistry.class);
        osgiManifestGenerator = mock(GoPluginOSGiManifest.DefaultGoPluginOSGiManifestCreator.class);
        pluginLoader = mock(PluginLoader.class);
        goPluginBundleDescriptorBuilder = mock(GoPluginBundleDescriptorBuilder.class);
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_ACTIVATOR_JAR_PATH)).thenReturn("defaultFiles/go-plugin-activator.jar");
        when(systemEnvironment.get(PLUGIN_WORK_DIR)).thenReturn(bundleDir.getAbsolutePath());
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Linux");

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
    }

    @Test
    void shouldCopyPluginToBundlePathAndInformRegistryAndUpdateTheOSGiManifestWhenAPluginIsAdded() throws Exception {
        String pluginId = "testplugin.descriptorValidator";
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder()
                .id(pluginId)
                .bundleLocation(expectedBundleDirectory)
                .pluginJarFileLocation(pluginJarFileLocation)
                .isBundledPlugin(true)
                .build());
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        when(registry.getPluginByIdOrFileName(pluginId, PLUGIN_JAR_FILE_NAME)).thenReturn(null);
        doNothing().when(registry).loadPlugin(descriptor);

        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(expectedBundleDirectory.exists()).isTrue();
        verify(registry).getPluginByIdOrFileName(pluginId, PLUGIN_JAR_FILE_NAME);
        verify(registry).loadPlugin(descriptor);
        verify(osgiManifestGenerator).updateManifestOf(descriptor);
        verify(pluginLoader).loadPlugin(descriptor);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(registry);
        assertThat(new File(expectedBundleDirectory, "lib/go-plugin-activator.jar").exists()).isTrue();
    }

    @Test
    void shouldOverwriteAFileCalledGoPluginActivatorInLibWithOurOwnGoPluginActivatorEvenIfItExists() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);
        File activatorFileLocation = new File(expectedBundleDirectory, "lib/go-plugin-activator.jar");
        FileUtils.writeStringToFile(activatorFileLocation, "SOME-DATA", UTF_8);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder()
                .id("testplugin.descriptorValidator")
                .bundleLocation(expectedBundleDirectory)
                .pluginJarFileLocation(pluginJarFileLocation)
                .isBundledPlugin(true)
                .build());
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);
        doNothing().when(registry).loadPlugin(descriptor);

        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(new File(expectedBundleDirectory, "lib/go-plugin-activator.jar").exists()).isTrue();
        assertThat(FileUtils.readFileToString(activatorFileLocation, UTF_8)).isNotEqualTo("SOME-DATA");
    }

    @Test
    void shouldCopyPluginToBundlePathAndInformRegistryAndUpdateTheOSGiManifestWhenAPluginIsUpdated() {
        String pluginId = "plugin-id";
        String pluginJarFileName = "jar-name.jar";
        File oldBundleDir = temporaryFolder.newFolder("old-bundle-dir");
        File newBundleDir = temporaryFolder.newFolder("new-bundle-dir");
        File newBundleJarFile = new File(newBundleDir, pluginJarFileName);
        DefaultPluginJarChangeListener spy = spy(listener);

        String pluginJarFileLocation1 = newBundleJarFile.getAbsolutePath();
        GoPluginDescriptor oldDescriptor = GoPluginDescriptor.builder()
                .id("testplugin.descriptorValidator")
                .bundleLocation(oldBundleDir)
                .pluginJarFileLocation(pluginJarFileLocation1)
                .pluginJarFileLocation(pluginJarFileLocation1)
                .isBundledPlugin(true)
                .build();
        GoPluginBundleDescriptor oldBundleDescriptor = new GoPluginBundleDescriptor(oldDescriptor);

        String pluginJarFileLocation = newBundleJarFile.getAbsolutePath();
        GoPluginDescriptor newDescriptor = GoPluginDescriptor.builder()
                .id(pluginId)
                .bundleLocation(newBundleDir)
                .pluginJarFileLocation(pluginJarFileLocation)
                .isBundledPlugin(true)
                .build();
        GoPluginBundleDescriptor newBundleDescriptor = new GoPluginBundleDescriptor(newDescriptor);

        doNothing().when(spy).explodePluginJarToBundleDir(newBundleJarFile, newBundleDir);
        doNothing().when(spy).installActivatorJarToBundleDir(newBundleJarFile);
        when(goPluginBundleDescriptorBuilder.build(newBundleJarFile, true)).thenReturn(newBundleDescriptor);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldDescriptor);
        when(registry.unloadPlugin(newBundleDescriptor)).thenReturn(oldBundleDescriptor);

        spy.pluginJarUpdated(new PluginFileDetails(newBundleJarFile, true));

        assertThat(oldBundleDir.exists()).isFalse();
        verify(registry, atLeastOnce()).getPluginByIdOrFileName(pluginId, pluginJarFileName);
        verify(registry).unloadPlugin(newBundleDescriptor);
        verify(registry).loadPlugin(newBundleDescriptor);
        verify(osgiManifestGenerator).updateManifestOf(newBundleDescriptor);
        verify(pluginLoader).unloadPlugin(oldBundleDescriptor);
        verify(pluginLoader).loadPlugin(newBundleDescriptor);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(registry);
    }

    @Test
    void shouldRemovePluginFromBundlePathAndInformRegistryWhenAPluginIsRemoved() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File removedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
        final GoPluginDescriptor descriptorOfThePluginWhichWillBeRemoved = GoPluginDescriptor.builder()
                .id("testplugin.descriptorValidator")
                .bundleLocation(removedBundleDirectory)
                .pluginJarFileLocation(pluginJarFileLocation)
                .isBundledPlugin(true)
                .build();
        GoPluginBundleDescriptor descriptorOfThePluginBundleWhichWillBeRemoved = new GoPluginBundleDescriptor(descriptorOfThePluginWhichWillBeRemoved);

        when(registry.getPluginByIdOrFileName(null, descriptorOfThePluginWhichWillBeRemoved.fileName())).thenReturn(descriptorOfThePluginWhichWillBeRemoved);
        when(registry.unloadPlugin(descriptorOfThePluginBundleWhichWillBeRemoved)).thenReturn(descriptorOfThePluginBundleWhichWillBeRemoved);

        copyPluginToTheDirectory(bundleDir, PLUGIN_JAR_FILE_NAME);

        listener.pluginJarRemoved(new PluginFileDetails(pluginJarFile, true));

        verify(registry).unloadPlugin(descriptorOfThePluginBundleWhichWillBeRemoved);
        verify(pluginLoader).unloadPlugin(descriptorOfThePluginBundleWhichWillBeRemoved);
        assertThat(removedBundleDirectory.exists()).isFalse();
    }

    @Test
    void shouldNotTryAndUpdateManifestOfAnAddedInvalidPlugin() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
        GoPluginBundleDescriptor descriptorForInvalidPlugin = new GoPluginBundleDescriptor(
                GoPluginDescriptor.builder()
                        .id("testplugin.descriptorValidator")
                        .bundleLocation(expectedBundleDirectory)
                        .pluginJarFileLocation(pluginJarFileLocation)
                        .pluginJarFileLocation(pluginJarFileLocation)
                        .isBundledPlugin(true)
                        .build())
                .markAsInvalid(singletonList("For a test"), null);

        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptorForInvalidPlugin);
        doNothing().when(registry).loadPlugin(descriptorForInvalidPlugin);


        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        assertThat(expectedBundleDirectory.exists()).isTrue();
        verify(registry).loadPlugin(descriptorForInvalidPlugin);
        verifyNoMoreInteractions(osgiManifestGenerator);
    }

    @Test
    void shouldNotTryAndUpdateManifestOfAnUpdatedInvalidPlugin() throws Exception {
        DefaultPluginJarChangeListener spy = spy(listener);
        String pluginId = "plugin-id";
        File pluginFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        File expectedBundleDirectoryForInvalidPlugin = new File(bundleDir, PLUGIN_JAR_FILE_NAME);
        File bundleDirectoryForOldPlugin = new File(bundleDir, "descriptor-aware-test-plugin-old.jar");
        FileUtils.forceMkdir(bundleDirectoryForOldPlugin);

        String pluginJarFileLocation = pluginFile.getAbsolutePath();
        GoPluginBundleDescriptor descriptorForInvalidPlugin = new GoPluginBundleDescriptor(
                GoPluginDescriptor.builder()
                        .id("testplugin.descriptorValidator")
                        .bundleLocation(expectedBundleDirectoryForInvalidPlugin)
                        .pluginJarFileLocation(pluginJarFileLocation)
                        .pluginJarFileLocation(pluginJarFileLocation)
                        .isBundledPlugin(true)
                        .build())
                .markAsInvalid(singletonList("For a test"), null);

        Bundle oldBundle = mock(Bundle.class);
        final GoPluginDescriptor oldPluginDescriptor = GoPluginDescriptor.builder()
                .id("some.old.id")
                .bundleLocation(bundleDirectoryForOldPlugin)
                .pluginJarFileLocation("some/path/to/plugin.jar")
                .isBundledPlugin(true)
                .build();
        GoPluginBundleDescriptor oldBundleDescriptor = new GoPluginBundleDescriptor(oldPluginDescriptor).setBundle(oldBundle);

        when(goPluginBundleDescriptorBuilder.build(pluginFile, true)).thenReturn(descriptorForInvalidPlugin);
        when(registry.getPlugin(pluginId)).thenReturn(oldPluginDescriptor);
        when(registry.unloadPlugin(descriptorForInvalidPlugin)).thenReturn(oldBundleDescriptor);
        doNothing().when(registry).loadPlugin(descriptorForInvalidPlugin);


        spy.pluginJarUpdated(new PluginFileDetails(pluginFile, true));

        assertThat(expectedBundleDirectoryForInvalidPlugin.exists()).isTrue();
        assertThat(bundleDirectoryForOldPlugin.exists()).isFalse();
        verify(registry).unloadPlugin(descriptorForInvalidPlugin);
        verify(pluginLoader).unloadPlugin(oldBundleDescriptor);
        verify(registry).loadPlugin(descriptorForInvalidPlugin);
        verifyNoMoreInteractions(osgiManifestGenerator);
        verifyNoMoreInteractions(pluginLoader);
    }

    @Test
    void shouldFailToLoadAPluginWhenActivatorJarIsNotAvailable() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_ACTIVATOR_JAR_PATH)).thenReturn("some-path-which-does-not-exist.jar");


        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        File bundleDirectory = new File(bundleDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        String pluginJarFileLocation = pluginJarFile.getAbsolutePath();
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder()
                .id("some.old.id")
                .bundleLocation(bundleDirectory)
                .pluginJarFileLocation(pluginJarFileLocation)
                .isBundledPlugin(true)
                .build();
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(new GoPluginBundleDescriptor(descriptor));

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        assertThatCode(() -> listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldNotReplaceBundledPluginWhenExternalPluginIsAdded() {
        String pluginId = "external";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        GoPluginBundleDescriptor externalPluginDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(pluginId, "1.0", pluginJarFileName, pluginJarFile, false, null));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(externalPluginDescriptor);

        GoPluginDescriptor bundledPluginDescriptor = getPluginDescriptor("bundled", "1.0", "1.0", null, true, null);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(bundledPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Found bundled plugin with ID: [bundled], external plugin could not be loaded");
        }
        verify(spy, never()).explodePluginJarToBundleDir(pluginJarFile, externalPluginDescriptor.bundleLocation());
    }

    private GoPluginDescriptor getPluginDescriptor(String pluginId, String version, String pluginJarLocation, File bundleLocation, boolean isBundledPlugin, String targetGoVersion, String... operatingSystems) {
        return GoPluginDescriptor.builder()
                .id(pluginId)
                .version("1")
                .pluginJarFileLocation(pluginJarLocation)
                .bundleLocation(bundleLocation)
                .isBundledPlugin(isBundledPlugin)
                .about(GoPluginDescriptor.About.builder()
                        .version(version)
                        .targetGoVersion(targetGoVersion)
                        .targetOperatingSystems(List.of(operatingSystems))
                        .build())
                .build();
    }

    @Test
    void shouldFailIfAtleastOnePluginFromExternalPluginBundleTriesToReplaceGoCDInternalBundledPlugins_WhenAdding() {
        final String filename = "plugin-file-name.jar";
        File newPluginJarFile = new File("/path/to/" + filename);
        final File bundleLocation = new File("/some/path/" + filename);

        final GoPluginDescriptor newPluginDescriptor1 = getPluginDescriptor("external.1", "1.0", newPluginJarFile.getAbsolutePath(), bundleLocation, false, null);
        final GoPluginDescriptor newPluginDescriptor2 = getPluginDescriptor("bundled", "1.0", newPluginJarFile.getAbsolutePath(), bundleLocation, false, null);
        GoPluginBundleDescriptor newExternalPluginBundleDescriptor = new GoPluginBundleDescriptor(newPluginDescriptor1, newPluginDescriptor2);
        when(goPluginBundleDescriptorBuilder.build(newPluginJarFile, false)).thenReturn(newExternalPluginBundleDescriptor);

        final GoPluginDescriptor existingPluginDescriptor1 = getPluginDescriptor("bundled", "1.0", "/some/path/file.jar", new File("/some/file.jar"), true, null);
        when(registry.getPluginByIdOrFileName("external.1", filename)).thenReturn(null);
        when(registry.getPluginByIdOrFileName("bundled", filename)).thenReturn(existingPluginDescriptor1);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            spy.pluginJarAdded(new PluginFileDetails(newPluginJarFile, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Found bundled plugin with ID: [bundled], external plugin could not be loaded");
        }
        verify(spy, never()).explodePluginJarToBundleDir(newPluginJarFile, newExternalPluginBundleDescriptor.bundleLocation());
    }

    @Test
    void shouldNotUpdatePluginWhenThereIsExistingPluginWithSameId() {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        final GoPluginDescriptor pluginDescriptor1 = getPluginDescriptor("some-new-plugin-id", "1.0", pluginJarFileName, pluginJarFile, true, null);
        final GoPluginDescriptor pluginDescriptor2 = getPluginDescriptor(pluginId, "1.0", pluginJarFileName, pluginJarFile, true, null);
        GoPluginBundleDescriptor newPluginDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        final GoPluginDescriptor oldPluginDescriptor = getPluginDescriptor(pluginId, "1.0", "location-old", new File("location-old"), true, null);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            spy.pluginJarUpdated(new PluginFileDetails(pluginJarFile, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Found another plugin with ID: plugin-id");
        }
        verify(spy, never()).explodePluginJarToBundleDir(pluginJarFile, newPluginDescriptor.bundleLocation());
    }

    @Test
    void shouldNotUpdateBundledPluginWithExternalPlugin() {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name.jar";
        File pluginJarFile = new File("/some/path/" + pluginJarFileName);

        GoPluginBundleDescriptor newPluginDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(pluginId, "1.0", pluginJarFileName, null, false, null));
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newPluginDescriptor);

        GoPluginDescriptor oldPluginDescriptor = getPluginDescriptor(pluginId, "1.0", null, null, true, null);
        when(registry.getPluginByIdOrFileName(pluginId, pluginJarFileName)).thenReturn(oldPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            spy.pluginJarUpdated(new PluginFileDetails(pluginJarFile, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Found bundled plugin with ID: [plugin-id], external plugin could not be loaded");
        }
        verify(spy, never()).explodePluginJarToBundleDir(pluginJarFile, newPluginDescriptor.bundleLocation());
    }

    @Test
    void shouldFailIfAtleastOnePluginFromExternalPluginBundleTriesToReplaceGoCDInternalBundledPlugins_WhenUpdating() {
        final String filename = "plugin-file-name.jar";
        File updatedPluginJarLocation = new File("/path/to/" + filename);
        File bundleLocation = new File("/some/path/" + filename);

        final GoPluginDescriptor newPluginDescriptor1 = getPluginDescriptor("external.1", "1.0", updatedPluginJarLocation.getAbsolutePath(), bundleLocation, false, null);
        final GoPluginDescriptor newPluginDescriptor2 = getPluginDescriptor("bundled", "1.0", updatedPluginJarLocation.getAbsolutePath(), bundleLocation, false, null);
        GoPluginBundleDescriptor newExternalPluginBundleDescriptor = new GoPluginBundleDescriptor(newPluginDescriptor1, newPluginDescriptor2);
        when(goPluginBundleDescriptorBuilder.build(updatedPluginJarLocation, false)).thenReturn(newExternalPluginBundleDescriptor);

        final GoPluginDescriptor existingPluginDescriptor1 = getPluginDescriptor("bundled", "1.0", updatedPluginJarLocation.getAbsolutePath(), bundleLocation, true, null);
        when(registry.getPluginByIdOrFileName("external.1", filename)).thenReturn(null);
        when(registry.getPluginByIdOrFileName("bundled", filename)).thenReturn(existingPluginDescriptor1);

        DefaultPluginJarChangeListener spy = spy(listener);
        try {
            spy.pluginJarUpdated(new PluginFileDetails(updatedPluginJarLocation, false));
            fail("should have failed as external plugin cannot replace bundled plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Found bundled plugin with ID: [bundled], external plugin could not be loaded");
        }
        verify(spy, never()).explodePluginJarToBundleDir(updatedPluginJarLocation, newExternalPluginBundleDescriptor.bundleLocation());
    }

    @Test
    void shouldNotRemoveBundledPluginExternalPluginJarRemovedWithSameId() {
        String pluginId = "plugin-id";
        String pluginJarFileName = "plugin-file-name";
        File pluginJarFile = mock(File.class);
        when(pluginJarFile.getName()).thenReturn(pluginJarFileName);

        final GoPluginDescriptor oldPluginDescriptor = getPluginDescriptor(pluginId, "1.0", null, null, true, null);
        GoPluginBundleDescriptor oldPluginBundleDescriptor = new GoPluginBundleDescriptor(oldPluginDescriptor);
        when(registry.getPluginByIdOrFileName(null, pluginJarFileName)).thenReturn(oldPluginDescriptor);

        DefaultPluginJarChangeListener spy = spy(listener);
        spy.pluginJarRemoved(new PluginFileDetails(pluginJarFile, false));
        verify(registry, never()).unloadPlugin(oldPluginBundleDescriptor);
        verify(pluginLoader, never()).unloadPlugin(oldPluginBundleDescriptor);
    }

    @Test
    void shouldNotLoadAPluginWhenCurrentOSIsNotAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXML() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);

        final GoPluginDescriptor pluginDescriptor1 = getPluginDescriptor("some.old.id.1", "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), false, null, "Windows");
        final GoPluginDescriptor pluginDescriptor2 = getPluginDescriptor("some.old.id.2", "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), false, null, "Linux", "Mac OS X");
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(bundleDescriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(bundleDescriptor);
        verifyZeroInteractions(pluginLoader);

        assertThat(pluginDescriptor1.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor1.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incompatible with current operating system 'Windows'. Valid operating systems are: [Linux, Mac OS X].");

        assertThat(pluginDescriptor2.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor2.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incompatible with current operating system 'Windows'. Valid operating systems are: [Linux, Mac OS X].");
    }

    @Test
    void shouldNotLoadAPluginWhenCurrentOSIsNotAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXMLForUpdatePath() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);
        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);

        String pluginID = "some.id";

        final GoPluginDescriptor pluginDescriptor1 = getPluginDescriptor("some.old.id.1", "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), false, null, "Linux");
        final GoPluginDescriptor pluginDescriptor2 = getPluginDescriptor("some.old.id.2", "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), false, null, "Windows", "Mac OS X");
        GoPluginBundleDescriptor newBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, false)).thenReturn(newBundleDescriptor);

        GoPluginBundleDescriptor oldPluginDescriptor = new GoPluginBundleDescriptor(getPluginDescriptor(pluginID, "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), true, null, "Linux", "Mac OS X"));
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Linux");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(newBundleDescriptor);
        when(registry.unloadPlugin(newBundleDescriptor)).thenReturn(oldPluginDescriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(newBundleDescriptor);

        assertThat(pluginDescriptor1.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor1.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incompatible with current operating system 'Linux'. Valid operating systems are: [Windows, Mac OS X].");

        assertThat(pluginDescriptor2.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor2.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incompatible with current operating system 'Linux'. Valid operating systems are: [Windows, Mac OS X].");
    }

    @Test
    void shouldLoadAPluginWhenCurrentOSIsAmongTheListOfTargetOSesAsDeclaredByThePluginInItsXML() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(getPluginDescriptor("some.old.id", "1.0", pluginJarFile.getAbsolutePath(),
                new File(PLUGIN_JAR_FILE_NAME), false, null, "Windows", "Linux"));
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(pluginLoader, times(1)).loadPlugin(descriptor);
    }

    @Test
    void shouldLoadAPluginWhenAListOfTargetOSesIsNotDeclaredByThePluginInItsXML() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(getPluginDescriptor("some.old.id", "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), false, null));
        when(systemEnvironment.getOperatingSystemFamilyName()).thenReturn("Windows");
        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(descriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(descriptor);
        verify(pluginLoader, times(1)).loadPlugin(descriptor);
    }

    @Test
    void shouldNotLoadAPluginWhenTargetedGocdVersionIsGreaterThanCurrentGocdVersion() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);

        final GoPluginDescriptor pluginDescriptor1 = getPluginDescriptor("some.old.id.1", "1.0", pluginJarFile.getAbsolutePath(),
                new File(PLUGIN_JAR_FILE_NAME), false, "17.5.0", "Linux", "Mac OS X");
        final GoPluginDescriptor pluginDescriptor2 = getPluginDescriptor("some.old.id.2", "1.0", pluginJarFile.getAbsolutePath(),
                new File(PLUGIN_JAR_FILE_NAME), false, "9999.0.0", "Linux", "Mac OS X");
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(bundleDescriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(bundleDescriptor);
        verifyZeroInteractions(pluginLoader);

        assertThat(pluginDescriptor1.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor1.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incompatible with GoCD version '" + CurrentGoCDVersion.getInstance().goVersion() + "'. Compatible version is: 9999.0.0.");

        assertThat(pluginDescriptor2.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor2.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incompatible with GoCD version '" + CurrentGoCDVersion.getInstance().goVersion() + "'. Compatible version is: 9999.0.0.");
    }

    @Test
    void shouldNotLoadAPluginWhenTargetedGocdVersionIsIncorrect() throws Exception {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);

        final GoPluginDescriptor pluginDescriptor1 = getPluginDescriptor("some.old.id.1", "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), false, "17.5.0", "Linux", "Mac OS X");
        final GoPluginDescriptor pluginDescriptor2 = getPluginDescriptor("some.old.id.2", "1.0", pluginJarFile.getAbsolutePath(), new File(PLUGIN_JAR_FILE_NAME), false, "9999.0.0.1.2", "Linux", "Mac OS X");
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        when(goPluginBundleDescriptorBuilder.build(pluginJarFile, true)).thenReturn(bundleDescriptor);

        listener = new DefaultPluginJarChangeListener(registry, osgiManifestGenerator, pluginLoader, goPluginBundleDescriptorBuilder, systemEnvironment);
        listener.pluginJarAdded(new PluginFileDetails(pluginJarFile, true));

        verify(registry, times(1)).loadPlugin(bundleDescriptor);
        verifyZeroInteractions(pluginLoader);

        assertThat(pluginDescriptor1.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor1.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incorrect target GoCD version (17.5.0 & 9999.0.0.1.2) specified.");

        assertThat(pluginDescriptor2.getStatus().getMessages().size()).isEqualTo(1);
        assertThat(pluginDescriptor2.getStatus().getMessages().get(0)).isEqualTo("Plugins with IDs ([some.old.id.1, some.old.id.2]) are not valid: Incorrect target GoCD version (17.5.0 & 9999.0.0.1.2) specified.");
    }

    @Test
    void shouldNotLoadAPluginWhenProvidedExtensionVersionByThePluginIsNotSupportedByCurrentGoCDVersion() throws IOException {
        File pluginJarFile = new File(pluginDir, PLUGIN_JAR_FILE_NAME);

        copyPluginToTheDirectory(pluginDir, PLUGIN_JAR_FILE_NAME);
        GoPluginDescriptor descriptor = getPluginDescriptor("some.old.id", "1.0", null,
                new File(PLUGIN_JAR_FILE_NAME), false, null, "Windows", "Linux");
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
