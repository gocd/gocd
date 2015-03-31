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

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.commons.PluginUploadResponse;
import com.thoughtworks.go.plugin.infra.listeners.DefaultPluginJarChangeListener;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.plugin.infra.plugininfo.DefaultPluginRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

import static com.thoughtworks.go.util.SystemEnvironment.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class DefaultPluginManagerTest {
    private static final String TEST_PLUGIN_BUNDLE_PATH = "test-bundles-dir";
    private File BUNDLE_DIR;
    private File PLUGIN_EXTERNAL_DIR;
    private DefaultPluginJarLocationMonitor monitor;
    private DefaultPluginRegistry registry;
    private GoPluginOSGiFramework goPluginOSGiFramework;
    private DefaultPluginJarChangeListener jarChangeListener;
    private SystemEnvironment systemEnvironment;
    private GoApplicationAccessor applicationAccessor;
    private PluginWriter pluginWriter;
    private PluginValidator pluginValidator;
    private static final File NON_JAR_FILE = new File("ice-cream-photo.jpg");
    public static final File NEW_JAR_FILE = new File("a-fancy-hipster-plugin.jar");

    @Before
    public void setUp() throws Exception {
        BUNDLE_DIR = new File(TEST_PLUGIN_BUNDLE_PATH);

        String pluginExternalDirName = "./tmp-external-DPJLMT" + new Random().nextInt();
        PLUGIN_EXTERNAL_DIR = new File(pluginExternalDirName);
        PLUGIN_EXTERNAL_DIR.mkdirs();

        monitor = mock(DefaultPluginJarLocationMonitor.class);
        registry = mock(DefaultPluginRegistry.class);
        goPluginOSGiFramework = mock(GoPluginOSGiFramework.class);
        jarChangeListener = mock(DefaultPluginJarChangeListener.class);
        systemEnvironment = mock(SystemEnvironment.class);
        applicationAccessor = mock(GoApplicationAccessor.class);
        pluginWriter = mock(PluginWriter.class);
        pluginValidator = mock(PluginValidator.class);

        when(systemEnvironment.get(PLUGIN_BUNDLE_PATH)).thenReturn(TEST_PLUGIN_BUNDLE_PATH);
        when(systemEnvironment.get(PLUGIN_FRAMEWORK_ENABLED)).thenReturn(Boolean.TRUE);
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(pluginExternalDirName);
    }

    @After
    public void clean() {
        FileUtils.deleteQuietly(PLUGIN_EXTERNAL_DIR);
        FileUtils.deleteQuietly(NEW_JAR_FILE);
        FileUtils.deleteQuietly(NON_JAR_FILE);
    }

    @Test
    public void shouldProceedToPluginWriterWithValidJarFile() throws Exception {
        NEW_JAR_FILE.createNewFile();
        DefaultPluginManager defaultPluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);
        when(pluginValidator.namecheckForJar(NEW_JAR_FILE.getName())).thenReturn(true);

        defaultPluginManager.addPlugin(NEW_JAR_FILE, NEW_JAR_FILE.getName());

        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(pluginWriter).addPlugin(fileArgumentCaptor.capture(), filenameArgumentCaptor.capture());

        assertThat(fileArgumentCaptor.getValue(), is(NEW_JAR_FILE));
        assertThat(filenameArgumentCaptor.getValue(), is(NEW_JAR_FILE.getName()));
    }

    @Test
    public void shouldReturnTheResponseReturnedByPluginWriterWithValidJarFile() throws Exception {
        NEW_JAR_FILE.createNewFile();
        DefaultPluginManager defaultPluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);
        when(pluginValidator.namecheckForJar(NEW_JAR_FILE.getName())).thenReturn(true);
        PluginUploadResponse expectedResponse = PluginUploadResponse.create(true, "successful!", null);
        when(pluginWriter.addPlugin(NEW_JAR_FILE, NEW_JAR_FILE.getName())).thenReturn(expectedResponse);

        PluginUploadResponse response = defaultPluginManager.addPlugin(NEW_JAR_FILE, NEW_JAR_FILE.getName());

        assertThat(response, is(expectedResponse));
    }

    @Test
    public void shouldReturnResponseWithErrorsWithInvalidJarFile() throws Exception {
        NON_JAR_FILE.createNewFile();
        DefaultPluginManager defaultPluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);
        when(pluginValidator.namecheckForJar(NON_JAR_FILE.getName())).thenReturn(false);

        PluginUploadResponse response = defaultPluginManager.addPlugin(NON_JAR_FILE, "not a jar");

        assertThat(response.success(), isEmptyString());
        assertFalse(response.isSuccess());
        assertTrue(response.errors().containsKey(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE));
        assertThat(response.errors().get(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE), is("Please upload a jar."));
    }

    @Test
    public void shouldCleanTheBundleDirectoryAtStart() throws Exception {
        String pluginJarFile = "descriptor-aware-test-plugin.should.be.deleted.jar";
        copyPluginToTheDirectory(BUNDLE_DIR, pluginJarFile);

        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment).startPluginInfrastructure();

        assertThat(BUNDLE_DIR.exists(), is(false));
    }

    @Test
    public void shouldStartOSGiFrameworkBeforeStartingMonitor() throws Exception {
        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment).startPluginInfrastructure();
        InOrder inOrder = inOrder(goPluginOSGiFramework, monitor);

        inOrder.verify(goPluginOSGiFramework).start();
        inOrder.verify(monitor).start();
    }

    @Test
    public void shouldNotStartPluginFrameworkIfPluginsAreDisabled() throws Exception {
        when(systemEnvironment.get(PLUGIN_FRAMEWORK_ENABLED)).thenReturn(Boolean.FALSE);

        new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);

        verifyZeroInteractions(goPluginOSGiFramework);
        verifyZeroInteractions(monitor);
    }

    @Test
    public void shouldAllowRunningAnActionOnAllRegisteredImplementations() throws Exception {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);

        Action action = mock(Action.class);
        pluginManager.doOnAll(SomeInterface.class, action);

        verify(goPluginOSGiFramework).doOnAll(SomeInterface.class, action);
    }

    @Test
    public void shouldAllowRunningAnActionOnASpecificPlugin() throws Exception {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);

        Action action = mock(Action.class);
        String pluginId = "test-plugin-id";
        pluginManager.doOn(SomeInterface.class, pluginId, action);

        verify(goPluginOSGiFramework).doOn(SomeInterface.class, pluginId, action);
    }

    @Test
    public void shouldAllowRegistrationOfPluginChangeListenersForGivenServiceReferences() throws Exception {
        GoPlugginOSGiFrameworkStub frameworkStub = new GoPlugginOSGiFrameworkStub();
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, frameworkStub, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);

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
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);

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
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);

        Action action = mock(Action.class);
        ExceptionHandler exceptionHandler = mock(ExceptionHandler.class);
        pluginManager.doOnAll(SomeInterface.class, action, exceptionHandler);

        verify(goPluginOSGiFramework).doOnAllWithExceptionHandling(SomeInterface.class, action, exceptionHandler);
    }

    @Test
    public void shouldAllowRunningAnActionOnRegisteredImplementationOfSpecifiedPlugin() {
        PluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);
        ActionWithReturn action = mock(ActionWithReturn.class);
        pluginManager.doOn(SomeInterface.class, "plugin-id", action);

        verify(goPluginOSGiFramework).doOn(SomeInterface.class, "plugin-id", action);
    }

    @Test
    public void shouldGetPluginDescriptorForGivenPluginIdCorrectly() throws Exception {
        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, null, pluginWriter, pluginValidator, systemEnvironment);
        GoPluginDescriptor pluginDescriptorForP1 = new GoPluginDescriptor("p1", "1.0", null, null, null, true);
        when(registry.getPlugin("valid-plugin")).thenReturn(pluginDescriptorForP1);
        when(registry.getPlugin("invalid-plugin")).thenReturn(null);
        MatcherAssert.assertThat(pluginManager.getPluginDescriptorFor("valid-plugin"), is(pluginDescriptorForP1));
        MatcherAssert.assertThat(pluginManager.getPluginDescriptorFor("invalid-plugin"), is(nullValue()));
    }

    @Test
    public void shouldSubmitPluginApiRequestToGivenPlugin() throws Exception {
        GoPluginApiRequest request = mock(GoPluginApiRequest.class);
        GoPluginApiResponse expectedResponse = mock(GoPluginApiResponse.class);
        final GoPlugin goPlugin = mock(GoPlugin.class);
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);

        when(goPlugin.handle(request)).thenReturn(expectedResponse);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ActionWithReturn<GoPlugin, GoPluginApiResponse> action = (ActionWithReturn<GoPlugin, GoPluginApiResponse>) invocationOnMock.getArguments()[2];
                return action.execute(goPlugin, descriptor);
            }
        }).when(goPluginOSGiFramework).doOn(eq(GoPlugin.class), eq("plugin-id"), any(ActionWithReturn.class));

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        GoPluginApiResponse actualResponse = pluginManager.submitTo("plugin-id", request);

        assertThat(actualResponse, is(expectedResponse));
        verify(goPlugin).initializeGoApplicationAccessor(applicationAccessor);
    }

    @Test
    public void shouldGetAllPluginsOfGivenExtension() throws Exception {
        GoPluginIdentifier pluginIdentifier = new GoPluginIdentifier("extension-type", asList("1.0"));
        GoPluginIdentifier anotherPluginIdentifier = new GoPluginIdentifier("another-extension-type", asList("1.0"));
        final GoPlugin goPlugin = mock(GoPlugin.class);
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Action<GoPlugin> action = (Action<GoPlugin>) invocationOnMock.getArguments()[1];
                action.execute(goPlugin, descriptor);
                action.execute(goPlugin, descriptor);
                return null;
            }
        }).when(goPluginOSGiFramework).doOnAll(eq(GoPlugin.class), any(Action.class));
        when(goPlugin.pluginIdentifier()).thenReturn(pluginIdentifier).thenReturn(anotherPluginIdentifier);


        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        List<GoPluginIdentifier> pluginIdentifiers = pluginManager.allPluginsOfType("extension-type");
        assertThat(pluginIdentifiers.size(), is(1));
        assertThat(pluginIdentifiers.get(0), is(pluginIdentifier));
    }

    @Test
    public void shouldCheckIfReferenceCanBeFoundForServiceClassAndPluginId() throws Exception {
        String pluginId = "plugin-id";
        when(goPluginOSGiFramework.hasReferenceFor(GoPlugin.class, pluginId)).thenReturn(true);
        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        assertThat(pluginManager.hasReferenceFor(GoPlugin.class, pluginId), is(true));
    }

    @Test
    public void shouldReturnTrueIfPluginIsOfGivenExtensionWhenReferenceFoundAndExtensionMatch() throws Exception {
        String pluginId = "plugin-id";
        GoPluginIdentifier pluginIdentifier = new GoPluginIdentifier("sample-extension", asList("1.0"));
        final GoPlugin goPlugin = mock(GoPlugin.class);
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        when(goPluginOSGiFramework.hasReferenceFor(GoPlugin.class, pluginId)).thenReturn(true);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ActionWithReturn<GoPlugin, GoPluginApiResponse> action = (ActionWithReturn<GoPlugin, GoPluginApiResponse>) invocationOnMock.getArguments()[2];
                return action.execute(goPlugin, descriptor);
            }
        }).when(goPluginOSGiFramework).doOn(eq(GoPlugin.class), eq(pluginId), any(ActionWithReturn.class));
        when(goPlugin.pluginIdentifier()).thenReturn(pluginIdentifier);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        assertTrue(pluginManager.isPluginOfType("sample-extension", pluginId));
    }

    @Test
    public void shouldNotFindPluginIsOfGivenExtensionWhenReferenceNotFound() throws Exception {
        final String pluginThatDoesNotImplement = "plugin-that-does-not-implement";
        when(goPluginOSGiFramework.hasReferenceFor(GoPlugin.class, pluginThatDoesNotImplement)).thenReturn(false);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        assertFalse(pluginManager.isPluginOfType("extension-type", pluginThatDoesNotImplement));
        verify(goPluginOSGiFramework).hasReferenceFor(GoPlugin.class, pluginThatDoesNotImplement);
        verify(goPluginOSGiFramework, never()).doOn(eq(GoPlugin.class), eq(pluginThatDoesNotImplement), any(ActionWithReturn.class));
    }

    @Test
    public void shouldNotFindPluginIsOfGivenExtensionWhenReferenceNotFoundAndExtensionDoNotMatch() throws Exception {
        final String pluginThatDoesNotImplement = "plugin-that-does-not-implement";
        GoPluginIdentifier pluginIdentifier = new GoPluginIdentifier("another-extension-type", asList("1.0"));
        final GoPlugin goPlugin = mock(GoPlugin.class);
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);

        when(goPluginOSGiFramework.hasReferenceFor(GoPlugin.class, pluginThatDoesNotImplement)).thenReturn(true);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ActionWithReturn<GoPlugin, GoPluginApiResponse> action = (ActionWithReturn<GoPlugin, GoPluginApiResponse>) invocationOnMock.getArguments()[2];
                return action.execute(goPlugin, descriptor);
            }
        }).when(goPluginOSGiFramework).doOn(eq(GoPlugin.class), eq(pluginThatDoesNotImplement), any(ActionWithReturn.class));
        when(goPlugin.pluginIdentifier()).thenReturn(pluginIdentifier);

        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        assertFalse(pluginManager.isPluginOfType("extension-type", pluginThatDoesNotImplement));
        verify(goPluginOSGiFramework).doOn(eq(GoPlugin.class), eq(pluginThatDoesNotImplement), any(ActionWithReturn.class));
    }

    @Test
    public void shouldResolveToCorrectExtensionVersion() throws Exception {
        String pluginId = "plugin-id";
        GoPlugin goPlugin = mock(GoPlugin.class);
        GoPlugginOSGiFrameworkStub osGiFrameworkStub = new GoPlugginOSGiFrameworkStub(goPlugin);
        osGiFrameworkStub.addHasReferenceFor(GoPlugin.class, pluginId, true);
        when(goPlugin.pluginIdentifier()).thenReturn(new GoPluginIdentifier("sample-extension", asList("1.0", "2.0")));
        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, osGiFrameworkStub, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        assertThat(pluginManager.resolveExtensionVersion(pluginId, asList("1.0", "2.0", "3.0")), is("2.0"));

    }

    @Test
    public void shouldThrowExceptionIfMatchingExtensionVersionNotFound() throws Exception {
        String pluginId = "plugin-id";
        GoPlugin goPlugin = mock(GoPlugin.class);
        GoPlugginOSGiFrameworkStub osGiFrameworkStub = new GoPlugginOSGiFrameworkStub(goPlugin);
        osGiFrameworkStub.addHasReferenceFor(GoPlugin.class, pluginId, true);
        when(goPlugin.pluginIdentifier()).thenReturn(new GoPluginIdentifier("sample-extension", asList("1.0", "2.0")));
        DefaultPluginManager pluginManager = new DefaultPluginManager(monitor, registry, osGiFrameworkStub, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment);
        try {
            pluginManager.resolveExtensionVersion(pluginId, asList("3.0", "4.0"));
            fail("should have thrown exception for not finding matching extension version");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Could not find matching extension version between Plugin[plugin-id] and Go"));
        }
    }

    @Test
    public void shouldSubmitPluginRequestToLoadSettings() throws Exception {
        DefaultPluginManager pluginManager = spy(new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment));

        final DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.success("<div></div");
        final String pluginId = "plugin-id";
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(((String) invocationOnMock.getArguments()[0]), is(pluginId));
                assertThat(((GoPluginApiRequest) invocationOnMock.getArguments()[1]).extension(), is(""));
                assertThat(((GoPluginApiRequest) invocationOnMock.getArguments()[1]).extensionVersion(), is(""));
                assertThat(((GoPluginApiRequest) invocationOnMock.getArguments()[1]).requestName(), is("load-settings"));
                return response;
            }
        }).when(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));

        String settingsTemplate = pluginManager.loadPluginSettings(pluginId);
        assertThat(settingsTemplate, is(response.responseBody()));
    }

    @Test
    public void shouldThrowExceptionIfPluginLoadingSettingsFail() throws Exception {
        try {
            DefaultPluginManager pluginManager = spy(new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment));
            doThrow(new GoPluginFrameworkException("Failed")).when(pluginManager).submitTo(eq("plugin-id"), any(GoPluginApiRequest.class));
            pluginManager.loadPluginSettings("plugin-id");
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Could not load settings for Plugin plugin-id. Failed"));
        }
    }

    @Test
    public void shouldThrowExceptionIfPluginReturnsUnsuccessfulResponseLoadingSettings() throws Exception {
        try {
            DefaultPluginManager pluginManager = spy(new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment));
            DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.error("error");
            doReturn(response).when(pluginManager).submitTo(eq("plugin-id"), any(GoPluginApiRequest.class));
            pluginManager.loadPluginSettings("plugin-id");
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Error while loading settings for plugin plugin-id. [error]"));
        }
    }

    @Test
    public void shouldSavePluginSettings() throws Exception {
        DefaultPluginManager pluginManager = spy(new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment));

        final DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.success("");
        final String pluginId = "plugin-id";
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(((String) invocationOnMock.getArguments()[0]), is(pluginId));
                assertThat(((GoPluginApiRequest) invocationOnMock.getArguments()[1]).extension(), is(""));
                assertThat(((GoPluginApiRequest) invocationOnMock.getArguments()[1]).extensionVersion(), is(""));
                assertThat(((GoPluginApiRequest) invocationOnMock.getArguments()[1]).requestName(), is("save-settings"));
                assertThat(((GoPluginApiRequest) invocationOnMock.getArguments()[1]).requestBody(), is("{}"));
                return response;
            }
        }).when(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));
        pluginManager.savePluginSettings(pluginId, "{}");
    }

    @Test
    public void shouldThrowExceptionIfPluginSaveSettingsFails() throws Exception {
        try {
            DefaultPluginManager pluginManager = spy(new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment));
            doThrow(new GoPluginFrameworkException("Failed")).when(pluginManager).submitTo(eq("plugin-id"), any(GoPluginApiRequest.class));
            pluginManager.savePluginSettings("plugin-id", "{}");
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Could not save settings for Plugin plugin-id. Failed"));
        }
    }

    @Test
    public void shouldThrowExceptionIfPluginReturnsUnsuccessfulResponseSavingSettings() throws Exception {
        try {
            DefaultPluginManager pluginManager = spy(new DefaultPluginManager(monitor, registry, goPluginOSGiFramework, jarChangeListener, applicationAccessor, pluginWriter, pluginValidator, systemEnvironment));
            DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.error("error");
            doReturn(response).when(pluginManager).submitTo(eq("plugin-id"), any(GoPluginApiRequest.class));
            pluginManager.savePluginSettings("plugin-id", "{}");
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Error while saving settings for plugin plugin-id. [error]"));
        }
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
        private GoPluginOSGiFramework goPluginOSGiFramework = mock(GoPluginOSGiFramework.class);
        private Object serviceReferenceInstance;


        GoPlugginOSGiFrameworkStub() {
        }

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
            return action.execute((T) serviceReferenceInstance, mock(GoPluginDescriptor.class));
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
