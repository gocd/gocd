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
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.Duration;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.*;


@DisabledOnOs(OS.WINDOWS)
@ExtendWith(MockitoExtension.class)
class DefaultPluginJarLocationMonitorTest extends AbstractDefaultPluginJarLocationMonitorTest {

    private DefaultPluginJarLocationMonitor monitor;
    private File bundledPluginDir;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private SystemEnvironment systemEnvironment;

    @Mock
    PluginJarChangeListener changeListener;

    @Override
    @BeforeEach
    void setUp(@TempDir File tempFolder) throws Exception {
        super.setUp(tempFolder);

        bundledPluginDir = this.tempFolder.newFolder("bundled-plugins");

        when(systemEnvironment.getPluginLocationMonitorIntervalInMillis()).thenReturn(TEST_MONITOR_INTERVAL_MILLIS);
        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(this.tempFolder.newFolder("external-plugins").getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_WORK_DIR)).thenReturn(pluginWorkDir.getAbsolutePath());

        monitor = new DefaultPluginJarLocationMonitor(systemEnvironment);
        monitor.initialize();
    }

    @AfterEach
    void tearDown() {
        monitor.stop();
    }

    @Test
    void shouldCreatePluginDirectoryIfItDoesNotExist() {
        bundledPluginDir.delete();
        new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
        assertThat(bundledPluginDir).exists();
    }

    @Test
    void shouldNotFailIfNoListenerIsPresentWhenAPluginJarIsAdded() throws Exception {
        addPlugin(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));
        monitor.start();

        assertTimeout(Duration.ofMillis(TEST_TIMEOUT), () -> monitor.hasRunAtLeastOnce());

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectNewlyAddedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);
        addPlugin(plugin);

        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        addPlugin(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.something-other-than-jar.zip", true));

        assertTimeout(Duration.ofMillis(TEST_TIMEOUT), () -> monitor.hasRunAtLeastOnce());

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldRunOnlyOnceWhenIntervalIsLessThanZero() throws Exception {
        when(systemEnvironment.getPluginLocationMonitorIntervalInMillis()).thenReturn(-1L);

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        monitor.addPluginJarChangeListener(changeListener);
        addPlugin(plugin);
        monitor.start();
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin);

        deletePlugin(plugin);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectRemovedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        addPlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin);

        deletePlugin(plugin);

        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(plugin);
        verify(changeListener, never()).pluginJarUpdated(any());
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true);

        addPlugin(plugin1);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin1);

        addPlugin(plugin2);
        addPlugin(plugin3);

        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin3);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesAddedEvenIfOneListenerThrowsAnException() throws Exception {
        PluginJarChangeListener exceptionRaisingListener = mock(PluginJarChangeListener.class);
        doThrow(new RuntimeException("Dummy Listener Exception")).when(exceptionRaisingListener).pluginJarAdded(any(BundleOrPluginFileDetails.class));
        monitor.addPluginJarChangeListener(exceptionRaisingListener);
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true);

        addPlugin(plugin1);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin1);
        verify(exceptionRaisingListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin1);

        addPlugin(plugin2);
        addPlugin(plugin3);

        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin2);
        verify(exceptionRaisingListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin3);
        verify(exceptionRaisingListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin3);

        verifyNoMoreInteractions(changeListener);
        verifyNoMoreInteractions(exceptionRaisingListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true);

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

        BundleOrPluginFileDetails orgFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails newFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1-new.jar", true);

        addPlugin(orgFile);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(orgFile);

        FileUtils.moveFile(orgFile.file(), newFile.file());

        InOrder inOrder = inOrder(changeListener);
        inOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(orgFile);
        inOrder.verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(newFile);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        addPlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin);

        updatePlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarUpdated(plugin);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldCreatePluginZipIfPluginJarIsAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        addPlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin);
    }

    @Test
    void shouldCreatePluginZipIfPluginJarIsRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        addPlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin);

        deletePlugin(plugin);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(plugin);
    }

    @Test
    void shouldCreatePluginZipIfPluginJarIsUpdated() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails orgFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails newFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1-new.jar", true);

        addPlugin(orgFile);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(orgFile);

        FileUtils.moveFile(orgFile.file(), newFile.file());
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarRemoved(orgFile);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(newFile);
    }

    @Test
    void shouldNotCreatePluginZipIfPluginJarIsNeitherUpdatedNorAddedOrRemoved() {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        assertTimeout(Duration.ofMillis(TEST_TIMEOUT), () -> monitor.hasRunAtLeastOnce());
    }

    @Test
    void shouldNotSendAnyNotificationsToAListenerWhichHasBeenRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true);

        addPlugin(plugin1);
        verify(changeListener, timeout(TEST_TIMEOUT)).pluginJarAdded(plugin1);

        monitor.removePluginJarChangeListener(changeListener);
        addPlugin(plugin2);
        long removed = System.currentTimeMillis();
        assertTimeout(Duration.ofMillis(TEST_TIMEOUT), () -> monitor.hasRunSince(removed));

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotAllowMonitorToBeStartedMultipleTimes() {
        monitor.start();
        assertThatThrownBy(() -> monitor.start()).isExactlyInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot start the monitor multiple times.");
    }
}
