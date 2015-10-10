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

import java.io.File;
import java.util.Random;

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static com.thoughtworks.go.util.FileUtil.recreateDirectory;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_GO_PROVIDED_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultExternalPluginJarLocationMonitorTest extends AbstractDefaultPluginJarLocationMonitorTest {
    private static OSChecker WINDOWS = new OSChecker(OSChecker.WINDOWS);
    private static final Random RANDOM = new Random();
    private File PLUGIN_BUNDLED_DIR;
    private File PLUGIN_EXTERNAL_DIR;

    private DefaultPluginJarLocationMonitor monitor;
    private PluginJarChangeListener changeListener;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        String pluginBundledDirName = "./tmp-bundled-DEPJLMT" + RANDOM.nextInt();
        PLUGIN_BUNDLED_DIR = new File(pluginBundledDirName);
        recreateDirectory(PLUGIN_BUNDLED_DIR);

        String pluginExternalDirName = "./tmp-external-DEPJLMT" + RANDOM.nextInt();
        PLUGIN_EXTERNAL_DIR = new File(pluginExternalDirName);
        recreateDirectory(PLUGIN_EXTERNAL_DIR);


        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS)).thenReturn(1);
        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(pluginBundledDirName);
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(pluginExternalDirName);

        changeListener = mock(PluginJarChangeListener.class);
        monitor = new DefaultPluginJarLocationMonitor(systemEnvironment);
        monitor.initialize();
    }

    @After
    public void tearDown() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.stop();
        FileUtils.deleteQuietly(PLUGIN_BUNDLED_DIR);
        FileUtils.deleteQuietly(PLUGIN_EXTERNAL_DIR);
    }

    @Test
    public void shouldCreateExternalPluginDirectoryIfItDoesNotExist() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        PLUGIN_EXTERNAL_DIR.delete();
        new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
        assertThat(PLUGIN_EXTERNAL_DIR.exists(), is(true));
    }

    @Test
    public void shouldThrowUpWhenExternalPluginDirectoryCreationFails() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
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
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-plugin-2.jar");
        waitAMoment();

        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-plugin-2.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-plugin.something-other-than-jar.zip");
        waitAMoment();

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldDetectRemovedPluginJar() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-plugin-2.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-plugin-2.jar", false));

        FileUtils.deleteQuietly(new File(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-plugin-2.jar"));
        waitAMoment();

        verify(changeListener).pluginJarRemoved(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-plugin-2.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyListenerOfMultiplePluginFilesAdded() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar", false));

        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-2.jar");
        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-3.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-2.jar", false));
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-3.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar");
        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-2.jar");
        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-3.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar", false));
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-2.jar", false));
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-3.jar", false));

        FileUtils.deleteQuietly(new File(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar"));
        FileUtils.deleteQuietly(new File(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-2.jar"));
        waitAMoment();
        verify(changeListener).pluginJarRemoved(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar", false));
        verify(changeListener).pluginJarRemoved(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-2.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyRemoveEventBeforeAddEventInCaseOfFileRename() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar");
        waitAMoment();
        PluginFileDetails orgExternalFile = pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar", false);
        verify(changeListener).pluginJarAdded(orgExternalFile);

        PluginFileDetails newExternalFile = pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1-new.jar", false);
        FileUtils.moveFile(orgExternalFile.file(), newExternalFile.file());

        waitAMoment();

        InOrder inOrder = inOrder(changeListener);
        inOrder.verify(changeListener).pluginJarRemoved(orgExternalFile);
        inOrder.verify(changeListener).pluginJarAdded(newExternalFile);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin.jar", false));

        updateFileContents(new File(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin.jar"));
        waitAMoment();

        verify(changeListener).pluginJarUpdated(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin.jar", false));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldAlwaysHandleBundledPluginsAheadOfExternalPlugins() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(PLUGIN_BUNDLED_DIR, "descriptor-aware-test-bundled-plugin-1.jar");
        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar");
        waitAMoment();
        InOrder jarAddedOrder = inOrder(changeListener);
        jarAddedOrder.verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_BUNDLED_DIR, "descriptor-aware-test-bundled-plugin-1.jar", true));
        jarAddedOrder.verify(changeListener).pluginJarAdded(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar", false));


        updateFileContents(new File(PLUGIN_BUNDLED_DIR, "descriptor-aware-test-bundled-plugin-1.jar"));
        updateFileContents(new File(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar"));
        waitAMoment();
        InOrder jarUpdatedOrder = inOrder(changeListener);
        jarUpdatedOrder.verify(changeListener).pluginJarUpdated(pluginFileDetails(PLUGIN_BUNDLED_DIR, "descriptor-aware-test-bundled-plugin-1.jar", true));
        jarUpdatedOrder.verify(changeListener).pluginJarUpdated(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar", false));

        FileUtils.deleteQuietly(new File(PLUGIN_BUNDLED_DIR, "descriptor-aware-test-bundled-plugin-1.jar"));
        FileUtils.deleteQuietly(new File(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar"));
        waitAMoment();
        InOrder jarRemovedOrder = inOrder(changeListener);
        jarRemovedOrder.verify(changeListener).pluginJarRemoved(pluginFileDetails(PLUGIN_BUNDLED_DIR, "descriptor-aware-test-bundled-plugin-1.jar", true));
        jarRemovedOrder.verify(changeListener).pluginJarRemoved(pluginFileDetails(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar", false));

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldSpecifyIfPluginIsBundledOrExternalWhenAdded() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(PLUGIN_BUNDLED_DIR, "descriptor-aware-test-bundled-plugin-1.jar");
        copyPluginToThePluginDirectory(PLUGIN_EXTERNAL_DIR, "descriptor-aware-test-external-plugin-1.jar");
        ArgumentCaptor<PluginFileDetails> pluginFileDetailsArgumentCaptor = ArgumentCaptor.forClass(PluginFileDetails.class);
        waitAMoment();
        verify(changeListener, times(2)).pluginJarAdded(pluginFileDetailsArgumentCaptor.capture());
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(0).isBundledPlugin(), is(true));
        assertThat(pluginFileDetailsArgumentCaptor.getAllValues().get(1).isBundledPlugin(), is(false));
        verifyNoMoreInteractions(changeListener);

    }
}
