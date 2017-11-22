/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.io.File;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class DefaultPluginJarLocationMonitorTest extends AbstractDefaultPluginJarLocationMonitorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static OSChecker WINDOWS = new OSChecker(OSChecker.WINDOWS);
    private DefaultPluginJarLocationMonitor monitor;
    private File bundledPluginDir;
    private File pluginExternalDir;

    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private PluginJarChangeListener changeListener;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        initMocks(this);
        if (WINDOWS.satisfy()) {
            return;
        }

        bundledPluginDir = temporaryFolder.newFolder("bundled-plugins");

        pluginExternalDir = temporaryFolder.newFolder("external-plugins");

        when(systemEnvironment.get(PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS)).thenReturn(1);
        when(systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH)).thenReturn(bundledPluginDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(pluginExternalDir.getAbsolutePath());

        monitor = new DefaultPluginJarLocationMonitor(systemEnvironment);
        monitor.initialize();
    }

    @After
    public void tearDown() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.stop();
        super.tearDown();
    }

    @Test
    public void shouldCreatePluginDirectoryIfItDoesNotExist() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        bundledPluginDir.delete();
        new DefaultPluginJarLocationMonitor(systemEnvironment).initialize();
        assertThat(bundledPluginDir.exists(), is(true));
    }

    @Test
    public void shouldNotFailIfNoListenerIsPresentWhenAPluginJarIsAdded() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin.jar");
        monitor.start();

        waitAMoment();

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldDetectNewlyAddedPluginJar() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin.jar");
        waitAMoment();

        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldDetectOnlyJarsAsNewPlugins() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin.something-other-than-jar.zip");
        waitAMoment();

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldRunOnlyOnceWhenIntervalIsLessThanZero() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        when(systemEnvironment.get(PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS)).thenReturn(-1);

        monitor.addPluginJarChangeListener(changeListener);
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin.jar");
        monitor.start();

        waitAMoment();
        FileUtils.deleteQuietly(new File(bundledPluginDir, "descriptor-aware-test-plugin.jar"));
        waitAMoment();

        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldDetectRemovedPluginJar() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));

        FileUtils.deleteQuietly(new File(bundledPluginDir, "descriptor-aware-test-plugin.jar"));
        waitAMoment();

        verify(changeListener).pluginJarRemoved(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyListenerOfMultiplePluginFilesAdded() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-1.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true));

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-2.jar");
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-3.jar");
        waitAMoment();

        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true));
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyListenerOfMultiplePluginFilesAddedEvenIfOneListenerThrowsAnException() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        PluginJarChangeListener exceptionRasingListener = mock(PluginJarChangeListener.class);
        doThrow(new RuntimeException("Dummy Listener Exception")).when(exceptionRasingListener).pluginJarAdded(Matchers.<PluginFileDetails>anyObject());
        monitor.addPluginJarChangeListener(exceptionRasingListener);
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-1.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true));
        verify(exceptionRasingListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true));

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-2.jar");
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-3.jar");
        waitAMoment();

        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true));
        verify(exceptionRasingListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true));
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true));
        verify(exceptionRasingListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true));
        verifyNoMoreInteractions(changeListener);
        verifyNoMoreInteractions(exceptionRasingListener);
    }

    @Test
    public void shouldNotifyListenerOfMultiplePluginFilesRemoved() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-1.jar");
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-2.jar");
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-3.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true));
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true));
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-3.jar", true));

        FileUtils.deleteQuietly(new File(bundledPluginDir, "descriptor-aware-test-plugin-1.jar"));
        FileUtils.deleteQuietly(new File(bundledPluginDir, "descriptor-aware-test-plugin-2.jar"));
        waitAMoment();

        verify(changeListener).pluginJarRemoved(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true));
        verify(changeListener).pluginJarRemoved(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-2.jar", true));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyRemoveEventBeforeAddEventInCaseOfFileRename() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-1.jar");
        waitAMoment();
        PluginFileDetails orgFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        verify(changeListener).pluginJarAdded(orgFile);

        PluginFileDetails newFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1-new.jar", true);
        FileUtils.moveFile(orgFile.file(), newFile.file());

        waitAMoment();

        InOrder inOrder = inOrder(changeListener);
        inOrder.verify(changeListener).pluginJarRemoved(orgFile);
        inOrder.verify(changeListener).pluginJarAdded(newFile);
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotifyListenersOfUpdatesToPluginJars() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));

        updateFileContents(new File(bundledPluginDir, "descriptor-aware-test-plugin.jar"));
        waitAMoment();

        verify(changeListener).pluginJarUpdated(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));
        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldCreatePluginZipIfPluginJarIsAdded() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin.jar", true));
    }

    @Test
    public void shouldCreatePluginZipIfPluginJarIsRemoved() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        String pluginJar = "descriptor-aware-test-plugin.jar";
        copyPluginToThePluginDirectory(bundledPluginDir, pluginJar);
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, pluginJar, true));

        FileUtils.deleteQuietly(new File(bundledPluginDir, pluginJar));
        waitAMoment();
        verify(changeListener).pluginJarRemoved(pluginFileDetails(bundledPluginDir, pluginJar, true));
    }

    @Test
    public void shouldCreatePluginZipIfPluginJarIsUpdated() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();

        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-1.jar");
        waitAMoment();
        PluginFileDetails orgFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true);
        verify(changeListener).pluginJarAdded(orgFile);

        PluginFileDetails newFile = pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1-new.jar", true);
        FileUtils.moveFile(orgFile.file(), newFile.file());
        waitAMoment();
    }

    @Test
    public void shouldNotCreatePluginZipIfPluginJarIsNeitherUpdatedNorAddedOrRemoved() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        waitAMoment();
    }

    @Test
    public void shouldNotSendAnyNotificationsToAListenerWhichHasBeenRemoved() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        monitor.addPluginJarChangeListener(changeListener);
        monitor.start();
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-1.jar");
        waitAMoment();
        verify(changeListener).pluginJarAdded(pluginFileDetails(bundledPluginDir, "descriptor-aware-test-plugin-1.jar", true));

        monitor.removePluginJarChangeListener(changeListener);
        copyPluginToThePluginDirectory(bundledPluginDir, "descriptor-aware-test-plugin-2.jar");
        waitAMoment();

        verifyNoMoreInteractions(changeListener);
    }

    @Test
    public void shouldNotAllowMonitorToBeStartedMultipleTimes() throws Exception {
        if (WINDOWS.satisfy()) {
            return;
        }
        try {
            monitor.start();
            monitor.start();
            fail("Expected an IllegalStateException.");
        } catch (IllegalStateException e) {
            assertEquals("Cannot start the monitor multiple times.", e.getMessage());
        }
    }
}
