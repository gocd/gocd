/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.FileHelper;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_WORK_DIR;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;;

@ExtendWith(MockitoExtension.class)
class DefaultPluginManagerTest {
    @Mock
    private DefaultPluginJarLocationMonitor monitor;
    @Mock
    private DefaultPluginRegistry registry;
    @Mock
    private GoPluginOSGiFramework goPluginOSGiFramework;
    @Mock
    private DefaultPluginJarChangeListener jarChangeListener;
    @Mock(lenient = true)
    private SystemEnvironment systemEnvironment;
    @Mock
    private PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    @Mock
    private PluginLoader pluginLoader;
    private File bundleDir;

    @BeforeEach
    void setUp(@TempDir File rootDir) {
        FileHelper temporaryFolder = new FileHelper(rootDir);

        bundleDir = temporaryFolder.newFolder("bundleDir");
        File pluginExternalDir = temporaryFolder.newFolder("externalDir");

        when(systemEnvironment.get(PLUGIN_WORK_DIR)).thenReturn(bundleDir.getAbsolutePath());
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(pluginExternalDir.getAbsolutePath());
    }

    @Test
    void shouldCleanTheBundleDirectoryAtStart() throws Exception {
        String pluginJarFile = "descriptor-aware-test-plugin.should.be.deleted.jar";
        copyPluginToTheDirectory(bundleDir, pluginJarFile);

        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, systemEnvironment, pluginLoader).startInfrastructure(true);

        assertThat(bundleDir.exists()).isFalse();
    }

    @Test
    void shouldStartOSGiFrameworkBeforeStartingMonitor() {
        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, systemEnvironment, pluginLoader).startInfrastructure(true);
        InOrder inOrder = inOrder(goPluginOSGiFramework, monitor);

        inOrder.verify(goPluginOSGiFramework).start();
        inOrder.verify(monitor).start();
    }

    @Test
    void shouldAllowRegistrationOfPluginChangeListeners() {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, systemEnvironment, pluginLoader);

        final PluginChangeListener pluginChangeListener = mock(PluginChangeListener.class);
        pluginManager.addPluginChangeListener(pluginChangeListener);

        verify(pluginLoader).addPluginChangeListener(pluginChangeListener);
    }

    @Test
    void shouldAllowRegistrationOfPluginPostLoadHooks() {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, systemEnvironment, pluginLoader);

        final PluginPostLoadHook pluginPostLoadHook = mock(PluginPostLoadHook.class);
        pluginManager.addPluginPostLoadHook(pluginPostLoadHook);

        verify(pluginLoader).addPluginPostLoadHook(pluginPostLoadHook);
    }

    @Test
    void shouldGetPluginDescriptorForGivenPluginIdCorrectly() {
        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, systemEnvironment, pluginLoader);
        GoPluginDescriptor pluginDescriptorForP1 = GoPluginDescriptor.builder().id("p1").version("1.0").build();
        when(registry.getPlugin("valid-plugin")).thenReturn(pluginDescriptorForP1);
        when(registry.getPlugin("invalid-plugin")).thenReturn(null);
        assertThat(pluginManager.getPluginDescriptorFor("valid-plugin")).isEqualTo(pluginDescriptorForP1);
        assertThat(pluginManager.getPluginDescriptorFor("invalid-plugin")).isNull();
    }

    @Test
    void shouldSubmitPluginApiRequestToGivenPlugin() throws Exception {
        String extensionType = "sample-extension";
        GoPluginApiRequest request = mock(GoPluginApiRequest.class);
        GoPluginApiResponse expectedResponse = mock(GoPluginApiResponse.class);
        final GoPlugin goPlugin = mock(GoPlugin.class);
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);

        when(goPlugin.handle(request)).thenReturn(expectedResponse);
        ArgumentCaptor<PluginAwareDefaultGoApplicationAccessor> captor = ArgumentCaptor.forClass(PluginAwareDefaultGoApplicationAccessor.class);
        doNothing().when(goPlugin).initializeGoApplicationAccessor(captor.capture());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                ActionWithReturn<GoPlugin, GoPluginApiResponse> action = (ActionWithReturn<GoPlugin, GoPluginApiResponse>) invocationOnMock.getArguments()[3];
                return action.execute(goPlugin, descriptor);
            }
        }).when(goPluginOSGiFramework).doOn(eq(GoPlugin.class), eq("plugin-id"), eq(extensionType), any(ActionWithReturn.class));

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);
        GoPluginApiResponse actualResponse = pluginManager.submitTo("plugin-id", extensionType, request);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        PluginAwareDefaultGoApplicationAccessor accessor = captor.getValue();
        assertThat(accessor.pluginDescriptor()).isEqualTo(descriptor);
    }

    @Test
    void shouldSayPluginIsOfGivenExtensionTypeWhenReferenceIsFound() {
        String pluginId = "plugin-id";
        String extensionType = "sample-extension";
        GoPluginIdentifier pluginIdentifier = new GoPluginIdentifier(extensionType, asList("1.0"));
        final GoPlugin goPlugin = mock(GoPlugin.class);
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        when(goPluginOSGiFramework.hasReferenceFor(GoPlugin.class, pluginId, extensionType)).thenReturn(true);

        lenient().doAnswer(invocationOnMock -> {
            ActionWithReturn<GoPlugin, GoPluginApiResponse> action = (ActionWithReturn<GoPlugin, GoPluginApiResponse>) invocationOnMock.getArguments()[2];
            return action.execute(goPlugin, descriptor);
        }).when(goPluginOSGiFramework).doOn(eq(GoPlugin.class), eq(pluginId), eq(extensionType), any(ActionWithReturn.class));
        lenient().when(goPlugin.pluginIdentifier()).thenReturn(pluginIdentifier);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);
        assertThat(pluginManager.isPluginOfType(extensionType, pluginId)).isTrue();
    }

    @Test
    void shouldSayAPluginIsNotOfAnExtensionTypeWhenReferenceIsNotFound() {
        final String pluginThatDoesNotImplement = "plugin-that-does-not-implement";
        String extensionType = "extension-type";
        when(goPluginOSGiFramework.hasReferenceFor(GoPlugin.class, pluginThatDoesNotImplement, extensionType)).thenReturn(false);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);
        boolean pluginIsOfExtensionType = pluginManager.isPluginOfType(extensionType, pluginThatDoesNotImplement);

        assertThat(pluginIsOfExtensionType).isFalse();
        verify(goPluginOSGiFramework).hasReferenceFor(GoPlugin.class, pluginThatDoesNotImplement, extensionType);
        verify(goPluginOSGiFramework, never()).doOn(eq(GoPlugin.class), eq(pluginThatDoesNotImplement), eq(extensionType), any(ActionWithReturn.class));
    }

    @Test
    void shouldResolveToCorrectExtensionVersion() {
        String pluginId = "plugin-id";
        String extensionType = "sample-extension";
        GoPlugin goPlugin = mock(GoPlugin.class);
        GoPlugginOSGiFrameworkStub osGiFrameworkStub = new GoPlugginOSGiFrameworkStub(goPlugin);
        osGiFrameworkStub.addHasReferenceFor(GoPlugin.class, pluginId, extensionType, true);
        when(goPlugin.pluginIdentifier()).thenReturn(new GoPluginIdentifier(extensionType, asList("1.0", "2.0")));

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, osGiFrameworkStub, jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);
        assertThat(pluginManager.resolveExtensionVersion(pluginId, extensionType, asList("1.0", "2.0", "3.0"))).isEqualTo("2.0");
    }

    @Test
    void shouldThrowExceptionIfMatchingExtensionVersionNotFound() {
        String pluginId = "plugin-id";
        String extensionType = "sample-extension";
        GoPlugin goPlugin = mock(GoPlugin.class);
        GoPlugginOSGiFrameworkStub osGiFrameworkStub = new GoPlugginOSGiFrameworkStub(goPlugin);
        osGiFrameworkStub.addHasReferenceFor(GoPlugin.class, pluginId, extensionType, true);
        when(goPlugin.pluginIdentifier()).thenReturn(new GoPluginIdentifier(extensionType, asList("1.0", "2.0")));

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, osGiFrameworkStub, jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);
        try {
            pluginManager.resolveExtensionVersion(pluginId, extensionType, asList("3.0", "4.0"));
            fail("should have thrown exception for not finding matching extension version");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Could not find matching extension version between Plugin[plugin-id] and Go");
        }
    }

    @Test
    void shouldAddPluginChangeListener() {
        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, mock(GoPluginOSGiFramework.class), jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);
        pluginManager.startInfrastructure(true);

        InOrder inOrder = inOrder(monitor);

        inOrder.verify(monitor).addPluginJarChangeListener(jarChangeListener);
    }

    @Test
    void isPluginLoaded_shouldReturnTrueWhenPluginIsLoaded() {
        final GoPluginDescriptor dockerPluginDescriptor = mock(GoPluginDescriptor.class);

        when(dockerPluginDescriptor.isInvalid()).thenReturn(false);
        when(registry.getPlugin("cd.go.elastic-agent.docker")).thenReturn(dockerPluginDescriptor);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, mock(GoPluginOSGiFramework.class), jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);

        assertThat(pluginManager.isPluginLoaded("cd.go.elastic-agent.docker")).isTrue();
    }

    @Test
    void isPluginLoaded_shouldReturnFalseWhenPluginIsLoadedButIsInInvalidState() {
        final GoPluginDescriptor dockerPluginDescriptor = mock(GoPluginDescriptor.class);

        when(dockerPluginDescriptor.isInvalid()).thenReturn(true);
        when(registry.getPlugin("cd.go.elastic-agent.docker")).thenReturn(dockerPluginDescriptor);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, mock(GoPluginOSGiFramework.class), jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);

        assertThat(pluginManager.isPluginLoaded("cd.go.elastic-agent.docker")).isFalse();
    }

    @Test
    void isPluginLoaded_shouldReturnFalseWhenPluginIsNotLoaded() {
        when(registry.getPlugin("cd.go.elastic-agent.docker")).thenReturn(null);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, mock(GoPluginOSGiFramework.class), jarChangeListener, pluginRequestProcessorRegistry, systemEnvironment, pluginLoader);

        assertThat(pluginManager.isPluginLoaded("cd.go.elastic-agent.docker")).isFalse();
    }

    private void copyPluginToTheDirectory(File destinationDir, String destinationFilenameOfPlugin) throws IOException {
        FileUtils.copyFile(pathOfFileInDefaultFiles("descriptor-aware-test-plugin.jar"), new File(destinationDir, destinationFilenameOfPlugin));
    }

    private File pathOfFileInDefaultFiles(String filePath) {
        return new File(getClass().getClassLoader().getResource("defaultFiles/" + filePath).getFile());
    }

    private static class GoPlugginOSGiFrameworkStub implements GoPluginOSGiFramework {
        private GoPluginOSGiFramework goPluginOSGiFramework = mock(GoPluginOSGiFramework.class);
        private Object serviceReferenceInstance;

        GoPlugginOSGiFrameworkStub(Object serviceReferenceInstance) {
            this.serviceReferenceInstance = serviceReferenceInstance;
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public Bundle loadPlugin(GoPluginBundleDescriptor pluginBundleDescriptor) {
            return null;
        }

        @Override
        public void unloadPlugin(GoPluginBundleDescriptor pluginDescriptor) {
        }

        @Override
        public <T, R> R doOn(Class<T> serviceReferenceClass, String pluginId, String extensionType, ActionWithReturn<T, R> action) {
            return action.execute((T) serviceReferenceInstance, mock(GoPluginDescriptor.class));
        }

        @Override
        public <T> boolean hasReferenceFor(Class<T> serviceReferenceClass, String pluginId, String extensionType) {
            return goPluginOSGiFramework.hasReferenceFor(serviceReferenceClass, pluginId, extensionType);
        }

        @Override
        public Map<String, List<String>> getExtensionsInfoFromThePlugin(String pluginId) {
            return new HashMap<>();
        }

        void addHasReferenceFor(Class<?> serviceRef, String pluginId, String extensionType, boolean hasReference) {
            lenient().when(goPluginOSGiFramework.hasReferenceFor(serviceRef, pluginId, extensionType)).thenReturn(hasReference);
        }
    }

}
