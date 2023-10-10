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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.Duration;
import java.util.List;

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

    @Override
    @BeforeEach
    void setUp(@TempDir File tempFolder) throws Exception {
        super.setUp(tempFolder);

        bundledPluginDir = this.tempFolder.newFolder("bundled-plugins");

        when(systemEnvironment.getPluginLocationMonitorIntervalInMillis()).thenReturn(100L);
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
        assertThat(bundledPluginDir.exists()).isTrue();
    }

    @Test
    void shouldNotFailIfNoListenerIsPresentWhenAPluginJarIsAdded() throws Exception {
        copyPluginToThePluginDirectory(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));
        monitor.start();

        assertTimeout(Duration.ofMillis(MONITOR_WAIT_MILLIS), () -> monitor.hasRunAtLeastOnce());

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectNewlyAddedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);
        copyPluginToThePluginDirectory(plugin);

        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.something-other-than-jar.zip", true));

        assertTimeout(Duration.ofMillis(MONITOR_WAIT_MILLIS), () -> monitor.hasRunAtLeastOnce());

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldRunOnlyOnceWhenIntervalIsLessThanZero() throws Exception {
        when(systemEnvironment.getPluginLocationMonitorIntervalInMillis()).thenReturn(-1L);

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        monitor.addPluginJarChangeListener(changeListener);
        copyPluginToThePluginDirectory(plugin);
        monitor.start();
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin);

        FileUtils.deleteQuietly(plugin.file());

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectRemovedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        copyPluginToThePluginDirectory(plugin);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin);

        FileUtils.deleteQuietly(plugin.file());

        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(plugin);
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

        copyPluginToThePluginDirectory(plugin1);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin1);

        copyPluginToThePluginDirectory(plugin2);
        copyPluginToThePluginDirectory(plugin3);

        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin3);

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(plugin1, plugin2, plugin3);
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

        copyPluginToThePluginDirectory(plugin1);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin1);
        verify(exceptionRaisingListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin1);

        copyPluginToThePluginDirectory(plugin2);
        copyPluginToThePluginDirectory(plugin3);

        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin2);
        verify(exceptionRaisingListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin2);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin3);
        verify(exceptionRaisingListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin3);

        verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(plugin1, plugin2, plugin3);

        for (var plugin : List.of(plugin1, plugin2, plugin3)) {
            verify(exceptionRaisingListener, atMostOnce()).pluginJarUpdated(plugin);
        }
        verifyNoMoreInteractions(exceptionRaisingListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true);
        BundleOrPluginFileDetails plugin3 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true);

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

        BundleOrPluginFileDetails orgFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails newFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1-new.jar", true);

        copyPluginToThePluginDirectory(orgFile);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(orgFile);

        FileUtils.moveFile(orgFile.file(), newFile.file());

        InOrder inOrder = inOrder(changeListener);
        inOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(orgFile);
        inOrder.verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(newFile);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        copyPluginToThePluginDirectory(plugin);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin);

        updateFileContents(plugin.file());
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarUpdated(plugin);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldCreatePluginZipIfPluginJarIsAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        copyPluginToThePluginDirectory(plugin);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin);
    }

    @Test
    void shouldCreatePluginZipIfPluginJarIsRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        BundleOrPluginFileDetails plugin = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true);

        copyPluginToThePluginDirectory(plugin);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin);

        FileUtils.deleteQuietly(plugin.file());
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(plugin);
    }

    @Test
    void shouldCreatePluginZipIfPluginJarIsUpdated() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails orgFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails newFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1-new.jar", true);

        copyPluginToThePluginDirectory(orgFile);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(orgFile);

        FileUtils.moveFile(orgFile.file(), newFile.file());
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarRemoved(orgFile);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(newFile);
    }

    @Test
    void shouldNotCreatePluginZipIfPluginJarIsNeitherUpdatedNorAddedOrRemoved() {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        assertTimeout(Duration.ofMillis(MONITOR_WAIT_MILLIS), () -> monitor.hasRunAtLeastOnce());
    }

    @Test
    void shouldNotSendAnyNotificationsToAListenerWhichHasBeenRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        BundleOrPluginFileDetails plugin1 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        BundleOrPluginFileDetails plugin2 = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true);

        copyPluginToThePluginDirectory(plugin1);
        verify(changeListener, timeout(MONITOR_WAIT_MILLIS)).pluginJarAdded(plugin1);

        monitor.removePluginJarChangeListener(changeListener);
        copyPluginToThePluginDirectory(plugin2);
        long removed = System.currentTimeMillis();
        assertTimeout(Duration.ofMillis(MONITOR_WAIT_MILLIS), () -> monitor.hasRunSince(removed));

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotAllowMonitorToBeStartedMultipleTimes() {
        monitor.start();
        assertThatThrownBy(() -> monitor.start()).isExactlyInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot start the monitor multiple times.");
    }
}
