/*
 * Copyright 2024 Thoughtworks, Inc.
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

    @Mock
    PluginJarChangeListener changeListener;

    @BeforeEach
    @Override
    void setUp(@TempDir File tempFolder) throws Exception {
        super.setUp(tempFolder);
        pluginBundledDir = this.tempFolder.newFolder("bundledDir");
        pluginExternalDir = this.tempFolder.newFolder("externalDir");

        when(systemEnvironment.getPluginLocationMonitorIntervalInMillis()).thenReturn(TEST_MONITOR_INTERVAL_MILLIS);
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
        assertThat(pluginExternalDir).exists();
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

        addPlugin(plugin2);

        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin2);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin.something-other-than-jar.zip", false);
        
        addPlugin(plugin);
        assertTimeout(Duration.ofMillis(TEST_TIMEOUT), () -> monitor.hasRunAtLeastOnce());

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectRemovedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin2 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false);

        addPlugin(plugin2);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin2);

        deletePlugin(plugin2);

        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(plugin2);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-1.jar", false);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-3.jar", false);

        addPlugin(plugin1);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin1);

        addPlugin(plugin2);
        addPlugin(plugin3);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin3);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-1.jar", false);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-3.jar", false);

        addPlugin(plugin1);
        addPlugin(plugin2);
        addPlugin(plugin3);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin1);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin3);

        deletePlugin(plugin1);
        deletePlugin(plugin2);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(plugin1);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(plugin2);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyRemoveEventBeforeAddEventInCaseOfFileRename() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails orgExternalFile = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false);
        BundleOrPluginFileDetails newExternalFile = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1-new.jar", false);

        addPlugin(orgExternalFile);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(orgExternalFile);

        renamePlugin(orgExternalFile, newExternalFile);

        InOrder inOrder = inOrder(changeListener);
        inOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(orgExternalFile);
        inOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(newExternalFile);
        verifyNoMoreInteractions(changeListener);
    }



    @Test
    void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin.jar", false);

        addPlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin);

        updatePlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarUpdated(plugin);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldAlwaysHandleBundledPluginsAheadOfExternalPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails bundledPlugin1 = pluginFileDetails(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar", true);
        BundleOrPluginFileDetails externalPlugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false);

        addPlugin(bundledPlugin1);
        addPlugin(externalPlugin1);
        InOrder jarAddedOrder = inOrder(changeListener);
        jarAddedOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(bundledPlugin1);
        jarAddedOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(externalPlugin1);

        verifyNoMoreInteractions(changeListener);

        updatePlugin(bundledPlugin1);
        updatePlugin(externalPlugin1);
        InOrder jarUpdatedOrder = inOrder(changeListener);
        jarUpdatedOrder.verify(changeListener, timeout(TEST_TIMEOUT).atLeastOnce()).pluginJarUpdated(bundledPlugin1);
        jarUpdatedOrder.verify(changeListener, timeout(TEST_TIMEOUT).atLeastOnce()).pluginJarUpdated(externalPlugin1);

        verifyNoMoreInteractions(changeListener);

        deletePlugin(bundledPlugin1);
        deletePlugin(externalPlugin1);
        InOrder jarRemovedOrder = inOrder(changeListener);
        jarRemovedOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(bundledPlugin1);
        jarRemovedOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(externalPlugin1);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldSpecifyIfPluginIsBundledOrExternalWhenAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails bundledPlugin1 = pluginFileDetails(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar", true);
        BundleOrPluginFileDetails externalPlugin1 = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false);

        addPlugin(bundledPlugin1);
        addPlugin(externalPlugin1);
        ArgumentCaptor<BundleOrPluginFileDetails> pluginFileDetailsArgumentCaptor = ArgumentCaptor.forClass(BundleOrPluginFileDetails.class);
        verify(changeListener, timeout(TEST_TIMEOUT).times(2)).pluginJarAdded(pluginFileDetailsArgumentCaptor.capture());
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(0).isBundledPlugin()).isTrue();
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(1).isBundledPlugin()).isFalse();

        verifyNoMoreInteractions(changeListener);
    }
}
