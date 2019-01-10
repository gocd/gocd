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

package com.thoughtworks.go.plugin.infra.monitor;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.File;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(JunitExtRunner.class)
@RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON,  OSChecker.WINDOWS})
public class DefaultExternalPluginJarLocationMonitorTest extends AbstractDefaultPluginJarLocationMonitorTest {

    private File pluginBundledDir;
    private File pluginExternalDir;

    private DefaultPluginJarLocationMonitor monitor;
    private PluginJarChangeListener changeListener;
    private SystemEnvironment systemEnvironment;

    @Before
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

    @After
    public void tearDown() throws Exception {
        monitor.stop();
        super.tearDown();
        temporaryFolder.delete();
    }

    @Test
    public void shouldCreateExternalPluginDirectoryIfItDoesNotExist() throws Exception {
        pluginExternalDir.delete();
        new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
        assertThat(pluginExternalDir.exists(), is(true));
    }

    @Test
    public void shouldThrowUpWhenExternalPluginDirectoryCreationFails() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn("/xyz");
        try {
            new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
            fail("should have failed for missing external plugin folder");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(),is("Failed to create external plugins folder in location /xyz"));
        }
    }

    @Test
    public void shouldDetectNewlyAddedPluginJar() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-plugin-2.jar");
        waitUntilNextRun(monitor);

        verify(changeListener).pluginJarAdded(pluginFileDetails(pluginExternalDir, "descriptor-aware-test-plugin-2.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-plugin.something-other-than-jar.zip");
        waitUntilNextRun(monitor);

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldDetectRemovedPluginJar() throws Exception {
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
    public void shouldNotifyListenerOfMultiplePluginFilesAdded() throws Exception {
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
    public void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
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
    public void shouldNotifyRemoveEventBeforeAddEventInCaseOfFileRename() throws Exception {
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
    public void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
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
    public void shouldAlwaysHandleBundledPluginsAheadOfExternalPlugins() throws Exception {
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
    public void shouldSpecifyIfPluginIsBundledOrExternalWhenAdded() throws Exception {
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(pluginBundledDir, "descriptor-aware-test-bundled-plugin-1.jar");
        copyPluginToThePluginDirectory(pluginExternalDir, "descriptor-aware-test-external-plugin-1.jar");
        ArgumentCaptor<PluginFileDetails> pluginFileDetailsArgumentCaptor = ArgumentCaptor.forClass(PluginFileDetails.class);
        waitUntilNextRun(monitor);
        verify(changeListener, times(2)).pluginJarAdded(pluginFileDetailsArgumentCaptor.capture());
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(0).isBundledPlugin(), is(true));
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(1).isBundledPlugin(), is(false));
        verifyNoMoreInteractions(changeListener);

    }
}
