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

package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PluginLoaderTest {
    private PluginLoader pluginLoader;
    private GoPluginOSGiFramework pluginOSGiFramework;

    @BeforeEach
    void setUp() {
        pluginOSGiFramework = mock(GoPluginOSGiFramework.class);
        pluginLoader = new PluginLoader(pluginOSGiFramework);
    }

    @Test
    void shouldMarkPluginDescriptorInvalidAndNotNotifyPluginChangeListenersWhenPostLoadHookFails() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.isInvalid()).thenReturn(false);

        final PluginPostLoadHook postLoadHook = mock(PluginPostLoadHook.class);
        when(postLoadHook.run(eq(pluginDescriptor), any())).thenReturn(new PluginPostLoadHook.Result(true, "Something went wrong"));

        PluginChangeListener listener = mock(PluginChangeListener.class);

        pluginLoader.addPluginChangeListener(listener);
        pluginLoader.addPluginPostLoadHook(postLoadHook);

        pluginLoader.loadPlugin(pluginDescriptor);

        verifyZeroInteractions(listener);

        verify(pluginDescriptor).markAsInvalid(eq(singletonList("Something went wrong")), eq(null));
        verify(postLoadHook, times(1)).run(eq(pluginDescriptor), any());
        verify(pluginOSGiFramework, times(0)).unloadPlugin(pluginDescriptor);
    }

    @Test
    void shouldRunPostLoadHooksInOrderOfRegistration() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.isInvalid()).thenReturn(false);

        final PluginPostLoadHook postLoadHook1 = mock(PluginPostLoadHook.class);
        when(postLoadHook1.run(eq(pluginDescriptor), anyMap())).thenReturn(new PluginPostLoadHook.Result(false, null));

        final PluginPostLoadHook postLoadHook2 = mock(PluginPostLoadHook.class);
        when(postLoadHook2.run(eq(pluginDescriptor), anyMap())).thenReturn(new PluginPostLoadHook.Result(true, "Something went wrong"));

        PluginChangeListener listener1 = mock(PluginChangeListener.class);

        pluginLoader.addPluginChangeListener(listener1);
        pluginLoader.addPluginPostLoadHook(postLoadHook1);
        pluginLoader.addPluginPostLoadHook(postLoadHook2);

        pluginLoader.loadPlugin(pluginDescriptor);

        verifyZeroInteractions(listener1);

        verify(pluginDescriptor).markAsInvalid(eq(singletonList("Something went wrong")), eq(null));

        final InOrder inOrder = inOrder(postLoadHook1, postLoadHook2);
        inOrder.verify(postLoadHook1).run(eq(pluginDescriptor), anyMap());
        inOrder.verify(postLoadHook2).run(eq(pluginDescriptor), anyMap());
    }

    @Test
    void shouldSendExtensionsInfoToPostLoadHooksDuringLoad() {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.isInvalid()).thenReturn(false);
        when(pluginDescriptor.id()).thenReturn("some-id");

        final Map<String, List<String>> extensionsInfo = singletonMap("elastic-agent", singletonList("1.0"));
        when(pluginOSGiFramework.getExtensionsInfoFromThePlugin("some-id")).thenReturn(extensionsInfo);


        final PluginPostLoadHook postLoadHook = mock(PluginPostLoadHook.class);
        when(postLoadHook.run(pluginDescriptor, extensionsInfo)).thenReturn(new PluginPostLoadHook.Result(false, null));

        pluginLoader.addPluginPostLoadHook(postLoadHook);
        pluginLoader.loadPlugin(pluginDescriptor);


        verify(postLoadHook).run(pluginDescriptor, extensionsInfo);
    }

    @Test
    void shouldNotifyAllPluginChangeListenerOncePluginIsLoaded() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.isInvalid()).thenReturn(false);

        PluginChangeListener listener1 = mock(PluginChangeListener.class);
        PluginChangeListener listener2 = mock(PluginChangeListener.class);
        PluginChangeListener listener3 = mock(PluginChangeListener.class);

        pluginLoader.addPluginChangeListener(listener1);
        pluginLoader.addPluginChangeListener(listener2);
        pluginLoader.addPluginChangeListener(listener3);

        pluginLoader.loadPlugin(pluginDescriptor);

        final InOrder inOrder = inOrder(listener1, listener2, listener3);
        inOrder.verify(listener1, times(1)).pluginLoaded(pluginDescriptor);
        inOrder.verify(listener2, times(1)).pluginLoaded(pluginDescriptor);
        inOrder.verify(listener3, times(1)).pluginLoaded(pluginDescriptor);
    }

    @Test
    void shouldMarkThePluginAsInvalidAndUnloadItIfAnyPluginChangeListenerThrowsAnExceptionDuringLoad() throws BundleException {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(mock(Bundle.class));
        when(pluginDescriptor.isInvalid()).thenReturn(false);
        when(pluginDescriptor.getStatus()).thenReturn(new PluginStatus(PluginStatus.State.INVALID).setMessages(singletonList("error"), new RuntimeException("root cause")));

        final PluginChangeListener changeListener = mock(PluginChangeListener.class);
        doThrow(new RuntimeException("some error")).when(changeListener).pluginLoaded(pluginDescriptor);

        try {
            pluginLoader.addPluginChangeListener(changeListener);
            pluginLoader.loadPlugin(pluginDescriptor);
            fail("should have thrown an exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to load plugin:"));
            verify(pluginDescriptor, times(1)).markAsInvalid(eq(singletonList("some error")), any());
            verify(pluginOSGiFramework, times(1)).unloadPlugin(pluginDescriptor);
        }
    }

    @Test
    void shouldNotCallPostLoadHooksAndListenersIfBundleFailsToLoad() {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        PluginChangeListener changeListener = mock(PluginChangeListener.class);
        PluginPostLoadHook postLoadHook = mock(PluginPostLoadHook.class);

        when(pluginOSGiFramework.loadPlugin(pluginDescriptor)).thenReturn(mock(Bundle.class));
        when(pluginDescriptor.isInvalid()).thenReturn(true);

        pluginLoader.addPluginChangeListener(changeListener);
        pluginLoader.addPluginPostLoadHook(postLoadHook);

        pluginLoader.loadPlugin(pluginDescriptor);

        verifyZeroInteractions(changeListener, postLoadHook);
    }

    @Test
    void shouldRunOtherUnloadListenersAndUnloadPluginBundleEvenIfAListenerFails() {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(mock(Bundle.class));

        PluginChangeListener listenerWhichWorks1 = mock(PluginChangeListener.class, "Listener Which Works: 1");
        PluginChangeListener listenerWhichWorks2 = mock(PluginChangeListener.class, "Listener Which Works: 2");
        PluginChangeListener listenerWhichThrowsWhenUnloading = mock(PluginChangeListener.class, "Listener Which Throws");
        doThrow(new RuntimeException("Fail!")).when(listenerWhichThrowsWhenUnloading).pluginUnLoaded(pluginDescriptor);

        pluginLoader.addPluginChangeListener(listenerWhichWorks1);
        pluginLoader.addPluginChangeListener(listenerWhichThrowsWhenUnloading);
        pluginLoader.addPluginChangeListener(listenerWhichWorks2);

        pluginLoader.unloadPlugin(pluginDescriptor);

        verify(listenerWhichWorks1, times(1)).pluginUnLoaded(pluginDescriptor);
        verify(listenerWhichThrowsWhenUnloading, times(1)).pluginUnLoaded(pluginDescriptor);
        verify(listenerWhichWorks2, times(1)).pluginUnLoaded(pluginDescriptor);

        verify(pluginOSGiFramework, times(1)).unloadPlugin(pluginDescriptor);
    }

    @Test
    void shouldNotUnloadAPluginIfItsBundleIsNull() {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(null);

        pluginLoader.unloadPlugin(pluginDescriptor);

        verifyZeroInteractions(pluginOSGiFramework);
    }
}