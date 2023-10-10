/*
 * Copyright 2023 Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.infra.monitor;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.Duration;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.*;

@DisabledOnOs(OS.WINDOWS)
@ExtendWith(MockitoExtension.class)
class DefaultExternalPluginJarLocationMonitorTest extends AbstractDefaultPluginJarLocationMonitorTest {

    private File pluginBundledDir;
    private File pluginExternalDir;

    private DefaultPluginJarLocationMonitor monitor;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    @Override
    void setUp(@TempDir File tempFolder) throws Exception {
        super.setUp(tempFolder);
        pluginBundledDir = this.tempFolder.newFolder("bundledDir");
        pluginExternalDir = this.tempFolder.newFolder("externalDir");

        when(systemEnvironment.getPluginLocationMonitorIntervalInMillis()).thenReturn(100L);
        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(pluginBundledDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(pluginExternalDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_WORK_DIR)).thenReturn(pluginWorkDir.getAbsolutePath());

        monitor = new DefaultPluginJarLocationMonitor(systemEnvironment);
        monitor.initialize();
    }

    @AfterEach
    void tearDown() {
        monitor.stop();
    }

    @Test
    void shouldCreateExternalPluginDirectoryIfItDoesNotExist() {
        pluginExternalDir.delete();
        new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
        assertThat(pluginExternalDir.exists()).isTrue();
    }

    @Test
    void shouldThrowUpWhenExternalPluginDirectoryCreationFails() {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn("/xyz");
        assertThatCode(() -> new DefaultPluginJarLocationMonitor(systemEnvironment).initialize())
                .hasMessage("Failed to create external plugins folder in location /xyz");
    }

    @Test
    void shouldDetectNewlyAddedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin2 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false);

        copyPluginToThePluginDirectory(plugin2);

        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin2);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin.something-other-than-jar.zip", false);
        
        copyPluginToThePluginDirectory(plugin);
        assertTimeout(Duration.ofMillis(MONITOR_WAIT_MILLIS), () -> monitor.hasRunAtLeastOnce());

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectRemovedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin2 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false);

        copyPluginToThePluginDirectory(plugin2);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin2);

        FileUtils.deleteQuietly(plugin2.file());

        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(plugin2);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-1.jar", false);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-3.jar", false);

        copyPluginToThePluginDirectory(plugin1);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin1);

        copyPluginToThePluginDirectory(plugin2);
        copyPluginToThePluginDirectory(plugin3);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin3);

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(plugin1, plugin2, plugin3);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-1.jar", false);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-3.jar", false);

        copyPluginToThePluginDirectory(plugin1);
        copyPluginToThePluginDirectory(plugin2);
        copyPluginToThePluginDirectory(plugin3);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin1);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin3);

        FileUtils.deleteQuietly(plugin1.file());
        FileUtils.deleteQuietly(plugin2.file());
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(plugin1);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(plugin2);

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(plugin1, plugin2, plugin3);
    }

    @Test
    void shouldNotifyRemoveEventBeforeAddEventInCaseOfFileRename() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails orgExternalFile = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false);
        BundleOrPluginFileDetails newExternalFile = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1-new.jar", false);

        copyPluginToThePluginDirectory(orgExternalFile);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(orgExternalFile);

        FileUtils.moveFile(orgExternalFile.file(), newExternalFile.file());

        InOrder inOrder = inOrder(changeListener);
        inOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(orgExternalFile);
        inOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(newExternalFile);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin.jar", false);

        copyPluginToThePluginDirectory(plugin);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin);

        updateFileContents(plugin.file());
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarUpdated(plugin);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldAlwaysHandleBundledPluginsAheadOfExternalPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails bundledPlugin1 = pluginFileDetails(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar", true);
        BundleOrPluginFileDetails externalPlugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false);

        copyPluginToThePluginDirectory(bundledPlugin1);
        copyPluginToThePluginDirectory(externalPlugin1);
        InOrder jarAddedOrder = inOrder(changeListener);
        jarAddedOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(bundledPlugin1);
        jarAddedOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(externalPlugin1);

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(bundledPlugin1, externalPlugin1);

        updateFileContents(bundledPlugin1.file());
        updateFileContents(externalPlugin1.file());
        InOrder jarUpdatedOrder = inOrder(changeListener);
        jarUpdatedOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS).atLeastOnce()).pluginJarUpdated(bundledPlugin1);
        jarUpdatedOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS).atLeastOnce()).pluginJarUpdated(externalPlugin1);

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(bundledPlugin1, externalPlugin1);

        FileUtils.deleteQuietly(bundledPlugin1.file());
        FileUtils.deleteQuietly(externalPlugin1.file());
        InOrder jarRemovedOrder = inOrder(changeListener);
        jarRemovedOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(bundledPlugin1);
        jarRemovedOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(externalPlugin1);

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(bundledPlugin1, externalPlugin1);
    }

    @Test
    void shouldSpecifyIfPluginIsBundledOrExternalWhenAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails bundledPlugin1 = pluginFileDetails(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar", true);
        BundleOrPluginFileDetails externalPlugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false);

        copyPluginToThePluginDirectory(bundledPlugin1);
        copyPluginToThePluginDirectory(externalPlugin1);
        ArgumentCaptor<BundleOrPluginFileDetails> pluginFileDetailsArgumentCaptor = ArgumentCaptor.forClass(BundleOrPluginFileDetails.class);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS).times(2)).pluginJarAdded(pluginFileDetailsArgumentCaptor.capture());
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(0).isBundledPlugin()).isTrue();
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(1).isBundledPlugin()).isFalse();

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(bundledPlugin1, externalPlugin1);
    }
}
