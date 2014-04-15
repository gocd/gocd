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

package com.thoughtworks.go.plugin.infra;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.Bundle;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_BUNDLE_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DefaultPluginManagerTest {
    private static final String TEST_PLUGIN_BUNDLE_PATH = "test-bundles-dir";
    private File BUNDLE_DIR;
    private DefaultPluginJarLocationMonitor monitor;
    private DefaultPluginRegistry registry;
    private GoPluginOSGiFramework goPluginOSGiFramework;
    private DefaultPluginJarChangeListener jarChangeListener;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        BUNDLE_DIR = new File(TEST_PLUGIN_BUNDLE_PATH);

        monitor = mock(DefaultPluginJarLocationMonitor.class);
        registry = mock(DefaultPluginRegistry.class);
        goPluginOSGiFramework = mock(GoPluginOSGiFramework.class);
        jarChangeListener = mock(DefaultPluginJarChangeListener.class);
        systemEnvironment = mock(SystemEnvironment.class);

        when(systemEnvironment.get(PLUGIN_BUNDLE_PATH)).thenReturn(TEST_PLUGIN_BUNDLE_PATH);
        when(systemEnvironment.get(PLUGIN_FRAMEWORK_ENABLED)).thenReturn(Boolean.TRUE);
    }

    @Test
    public void shouldCleanTheBundleDirectoryAtStart() throws Exception {
        String pluginJarFile = "descriptor-aware-test-plugin.should.be.deleted.jar";
        copyPluginToTheDirectory(BUNDLE_DIR, pluginJarFile);

        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment).startPluginInfrastructure();

        assertThat(BUNDLE_DIR.exists(), is(false));
    }

    @Test
    public void shouldStartOSGiFrameworkBeforeStartingMonitor() throws Exception {
        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment).startPluginInfrastructure();
        InOrder inOrder = inOrder(goPluginOSGiFramework, monitor);

        inOrder.verify(goPluginOSGiFramework).start();
        inOrder.verify(monitor).start();
    }

    @Test
    public void shouldNotStartPluginFrameworkIfPluginsAreDisabled() throws Exception {
        when(systemEnvironment.get(PLUGIN_FRAMEWORK_ENABLED)).thenReturn(Boolean.FALSE);

        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment);

        verifyZeroInteractions(goPluginOSGiFramework);
        verifyZeroInteractions(monitor);
    }

    @Test
    public void shouldAllowRunningAnActionOnAllRegisteredImplementations() throws Exception {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment);

        Action action = mock(Action.class);
        pluginManager.doOnAll(SomeInterface.class, action);

        verify(goPluginOSGiFramework).doOnAll(SomeInterface.class, action);
    }

    @Test
    public void shouldAllowRunningAnActionOnASpecificPlugin() throws Exception {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment);

        Action action = mock(Action.class);
        String pluginId = "test-plugin-id";
        pluginManager.doOn(SomeInterface.class, pluginId, action);

        verify(goPluginOSGiFramework).doOn(SomeInterface.class, pluginId, action);
    }

    @Test
    public void shouldAllowRegistrationOfPluginChangeListenersForGivenServiceReferences() throws Exception {
        GoPlugginOSGiFrameworkStub frameworkStub = new GoPlugginOSGiFrameworkStub();
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, frameworkStub, jarChangeListener, systemEnvironment);

        Action action = mock(Action.class);
        String pluginId1 = "test-plugin-id-1";
        String pluginId2 = "test-plugin-id-2";
        GoPluginDescriptor descriptor1 = mock(GoPluginDescriptor.class);
        when(descriptor1.id()).thenReturn(pluginId1);
        GoPluginDescriptor descriptor2 = mock(GoPluginDescriptor.class);
        when(descriptor2.id()).thenReturn(pluginId2);

        final int[] pluginLoaded = new int[]{0};
        final int[] pluginUnloaded = new int[]{0};
        PluginChangeListener someInterfaceListener = new PluginChangeListener() {
            @Override
            public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
                pluginLoaded[0]++;
            }

            @Override
            public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
                pluginUnloaded[0]++;
            }
        };
        pluginManager.addPluginChangeListener(someInterfaceListener, SomeInterface.class, SomeOtherInterface.class);
        frameworkStub.addHasReferenceFor(SomeInterface.class, pluginId1, true);
        frameworkStub.addHasReferenceFor(SomeOtherInterface.class, pluginId1, false);
        frameworkStub.addHasReferenceFor(SomeInterface.class, pluginId2, false);
        frameworkStub.addHasReferenceFor(SomeOtherInterface.class, pluginId2, true);
        assertThat(frameworkStub.pluginChangeListener == null, is(false));
        frameworkStub.pluginChangeListener.pluginLoaded(descriptor1);
        frameworkStub.pluginChangeListener.pluginLoaded(descriptor2);
        frameworkStub.pluginChangeListener.pluginUnLoaded(descriptor1);
        frameworkStub.pluginChangeListener.pluginUnLoaded(descriptor2);
        assertThat(pluginLoaded[0], is(2));
        assertThat(pluginUnloaded[0], is(2));
    }

    @Test
    public void shouldAllowRunningAnActionOnASpecificPluginIfReferenceExists() throws Exception {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment);

        Action action = mock(Action.class);
        String pluginId1 = "test-plugin-id-1";
        String pluginId2 = "test-plugin-id-2";
        when(goPluginOSGiFramework.hasReferenceFor(SomeInterface.class, pluginId1)).thenReturn(true);
        when(goPluginOSGiFramework.hasReferenceFor(SomeInterface.class, pluginId2)).thenReturn(false);
        pluginManager.doOnIfHasReference(SomeInterface.class, pluginId1, action);
        pluginManager.doOnIfHasReference(SomeInterface.class, pluginId2, action);

        verify(goPluginOSGiFramework).hasReferenceFor(SomeInterface.class, pluginId1);
        verify(goPluginOSGiFramework).hasReferenceFor(SomeInterface.class, pluginId2);
        verify(goPluginOSGiFramework).doOn(SomeInterface.class, pluginId1, action);

    }

    @Test
    public void shouldAllowRunningAnActionOnAllRegisteredImplementationsWithExceptionHandling() throws Exception {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment);

        Action action = mock(Action.class);
        ExceptionHandler exceptionHandler = mock(ExceptionHandler.class);
        pluginManager.doOnAll(SomeInterface.class, action, exceptionHandler);

        verify(goPluginOSGiFramework).doOnAllWithExceptionHandling(SomeInterface.class, action, exceptionHandler);
    }

    @Test
    public void shouldAllowRunningAnActionOnRegisteredImplementationOfSpecifiedPlugin() {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment);
        ActionWithReturn action = mock(ActionWithReturn.class);
        pluginManager.doOn(SomeInterface.class, "plugin-id", action);

        verify(goPluginOSGiFramework).doOn(SomeInterface.class, "plugin-id", action);
    }

    @Test
    public void shouldGetPluginDescriptorForGivenPluginIdCorrectly() throws Exception {
        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, systemEnvironment);
        GoPluginDescriptor pluginDescriptorForP1 = new GoPluginDescriptor("p1", "1.0", null, null, null, true);
        when(registry.getPlugin("valid-plugin")).thenReturn(pluginDescriptorForP1);
        when(registry.getPlugin("invalid-plugin")).thenReturn(null);
        MatcherAssert.assertThat(pluginManager.getPluginDescriptorFor("valid-plugin"), is(pluginDescriptorForP1));
        MatcherAssert.assertThat(pluginManager.getPluginDescriptorFor("invalid-plugin"), is(nullValue()));
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(BUNDLE_DIR);
    }

    private void copyPluginToTheDirectory(File destinationDir, String destinationFilenameOfPlugin) throws IOException, URISyntaxException {
        FileUtils.copyFile(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.jar"), new File(destinationDir, destinationFilenameOfPlugin));
    }

    private File pathOfFileInDefaultFiles(String filePath) {
        return new File(getClass().getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

    private static interface SomeInterface {
    }

    private static interface SomeOtherInterface {
    }

    private static class GoPlugginOSGiFrameworkStub implements GoPluginOSGiFramework {

        public PluginChangeListener pluginChangeListener;
        private GoPluginOSGiFramework goPluginOSGiFramework=mock(GoPluginOSGiFramework.class);

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public Bundle loadPlugin(GoPluginDescriptor pluginDescriptor) {
            return null;
        }

        @Override
        public void unloadPlugin(GoPluginDescriptor pluginDescriptor) {

        }

        @Override
        public void addPluginChangeListener(PluginChangeListener pluginChangeListener) {
            this.pluginChangeListener = pluginChangeListener;
        }

        @Override
        public <T> void doOnAll(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches) {

        }

        @Override
        public <T> void doOnAllWithExceptionHandling(Class<T> serviceReferenceClass, Action<T> actionToDoOnEachRegisteredServiceWhichMatches, ExceptionHandler<T> handler) {

        }

        @Override
        public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, ActionWithReturn<T, R> action) {
            return null;
        }

        @Override
        public <T> void doOn(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {

        }

        @Override
        public <T> void doOnWithExceptionHandling(Class<T> serviceReferenceClass, String pluginId, Action<T> action, ExceptionHandler<T> handler) {

        }

        @Override
        public <T> void doOnAllForPlugin(Class<T> serviceReferenceClass, String pluginId, Action<T> action) {

        }

        @Override
        public <T> void doOnAllWithExceptionHandlingForPlugin(Class<T> serviceReferenceClass, String pluginId, Action<T> action, ExceptionHandler<T> handler) {

        }

        @Override
        public <T> boolean hasReferenceFor(Class<T> serviceReferenceClass, String pluginId) {
            return goPluginOSGiFramework.hasReferenceFor(serviceReferenceClass, pluginId);
        }

        public void addHasReferenceFor(Class<?> serviceRef, String pluginId, boolean hasReference) {
            when(goPluginOSGiFramework.hasReferenceFor(serviceRef, pluginId)).thenReturn(hasReference);
        }
    }

}
