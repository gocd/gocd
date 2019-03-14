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

package com.thoughtworks.go.plugin.infra.monitor;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.File;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@DisabledOnOs(OS.WINDOWS)
public class DefaultExternalPluginJarLocationMonitorTest extends AbstractDefaultPluginJarLocationMonitorTest {

    private File pluginBundledDir;
    private File pluginExternalDir;

    private DefaultPluginJarLocationMonitor monitor;
    private PluginJarChangeListener changeListener;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        temporaryFolder.create();
        pluginBundledDir = temporaryFolder.newFolder("bundledDir");
        pluginExternalDir = temporaryFolder.newFolder("externalDir");

        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS)).thenReturn(1);
        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(pluginBundledDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(pluginExternalDir.getAbsolutePath());

        changeListener = mock(PluginJarChangeListener.class);
        monitor = new DefaultPluginJarLocationMonitor(systemEnvironment);
        monitor.initialize();
    }

    @AfterEach
    public void tearDown() throws Exception {
        monitor.stop();
        super.tearDown();
        temporaryFolder.delete();
    }

    @Test
    void shouldCreateExternalPluginDirectoryIfItDoesNotExist() {
        pluginExternalDir.delete();
        new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
        assertThat(pluginExternalDir.exists()).isTrue();
    }

    @Test
    void shouldThrowUpWhenExternalPluginDirectoryCreationFails() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn("/xyz");
        try {
            new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
            fail("should have failed for missing external plugin folder");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Failed to create external plugins folder in location /xyz");
        }
    }

    @Test
    void shouldDetectNewlyAddedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-plugin-2.jar");
        waitUntilNextRun(monitor);

        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-plugin.something-other-than-jar.zip");
        waitUntilNextRun(monitor);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldDetectRemovedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-plugin-2.jar");
        waitUntilNextRun(monitor);
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false));

        FileUtils.deleteQuietly(new File(pluginExternalDir, "descriptor-aware-test-plugin-2.jar"));
        waitUntilNextRun(monitor);

        verify(changeListener).pluginJarRemoved(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar");
        waitUntilNextRun(monitor);
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false));

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-2.jar");
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-3.jar");
        waitUntilNextRun(monitor);
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-2.jar", false));
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-3.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar");
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-2.jar");
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-3.jar");
        waitUntilNextRun(monitor);
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false));
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-2.jar", false));
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-3.jar", false));

        FileUtils.deleteQuietly(new File(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar"));
        FileUtils.deleteQuietly(new File(pluginExternalDir, "descriptor-aware-test-external-plugin-2.jar"));
        waitUntilNextRun(monitor);
        verify(changeListener).pluginJarRemoved(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false));
        verify(changeListener).pluginJarRemoved(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-2.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyRemoveEventBeforeAddEventInCaseOfFileRename() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar");
        waitUntilNextRun(monitor);
        PluginFileDetails orgExternalFile = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false);
        verify(changeListener).pluginJarAdded(orgExternalFile);

        PluginFileDetails newExternalFile = pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1-new.jar", false);
        FileUtils.moveFile(orgExternalFile.file(), newExternalFile.file());

        waitUntilNextRun(monitor);

        InOrder inOrder = inOrder(changeListener);
        inOrder.verify(changeListener).pluginJarRemoved(orgExternalFile);
        inOrder.verify(changeListener).pluginJarAdded(newExternalFile);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin.jar");
        waitUntilNextRun(monitor);
        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin.jar", false));

        updateFileContents(new File(pluginExternalDir, "descriptor-aware-test-external-plugin.jar"));
        waitUntilNextRun(monitor);

        verify(changeListener).pluginJarUpdated(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldAlwaysHandleBundledPluginsAheadOfExternalPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar");
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar");
        waitUntilNextRun(monitor);
        InOrder jarAddedOrder = inOrder(changeListener);
        jarAddedOrder.verify(changeListener).pluginJarAdded(pluginFileDetails(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar", true));
        jarAddedOrder.verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false));


        updateFileContents(new File(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar"));
        updateFileContents(new File(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar"));
        waitUntilNextRun(monitor);
        InOrder jarUpdatedOrder = inOrder(changeListener);
        jarUpdatedOrder.verify(changeListener).pluginJarUpdated(pluginFileDetails(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar", true));
        jarUpdatedOrder.verify(changeListener).pluginJarUpdated(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false));

        FileUtils.deleteQuietly(new File(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar"));
        FileUtils.deleteQuietly(new File(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar"));
        waitUntilNextRun(monitor);
        InOrder jarRemovedOrder = inOrder(changeListener);
        jarRemovedOrder.verify(changeListener).pluginJarRemoved(pluginFileDetails(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar", true));
        jarRemovedOrder.verify(changeListener).pluginJarRemoved(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar", false));

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    void shouldSpecifyIfPluginIsBundledOrExternalWhenAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar");
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar");
        ArgumentCaptor<PluginFileDetails> pluginFileDetailsArgumentCaptor = ArgumentCaptor.forClass(PluginFileDetails.class);
        waitUntilNextRun(monitor);
        verify(changeListener, times(2)).pluginJarAdded(pluginFileDetailsArgumentCaptor.capture());
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(0).isBundledPlugin()).isTrue();
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(1).isBundledPlugin()).isFalse();
        verifyNoMoreInteractions(changeListener);

    }
}
