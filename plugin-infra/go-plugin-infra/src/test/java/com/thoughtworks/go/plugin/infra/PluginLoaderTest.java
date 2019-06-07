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

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.osgi.framework.Bundle;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
    void shouldMarkPluginDescriptorInvalidAndNotNotifyPluginChangeListenersWhenPostLoadHookFails() {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.usingId("some.id.1", null, null, false);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);

        final PluginPostLoadHook postLoadHook = mock(PluginPostLoadHook.class);
        when(postLoadHook.run(eq(pluginDescriptor), any())).thenReturn(new PluginPostLoadHook.Result(true, "Something went wrong"));

        PluginChangeListener listener = mock(PluginChangeListener.class);

        pluginLoader.addPluginChangeListener(listener);
        pluginLoader.addPluginPostLoadHook(postLoadHook);

        pluginLoader.loadPlugin(pluginBundleDescriptor);

        verifyZeroInteractions(listener);

        assertThat(pluginDescriptor.isInvalid(), is(true));
        assertThat(pluginDescriptor.getStatus().getMessages(), is(singletonList("Something went wrong")));
        verify(postLoadHook, times(1)).run(eq(pluginDescriptor), any());
        verify(pluginOSGiFramework, times(0)).unloadPlugin(pluginBundleDescriptor);
    }

    @Test
    void shouldRunPostLoadHooksInOrderOfRegistration() {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.usingId("some.id.1", null, null, false);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);

        final PluginPostLoadHook postLoadHook1 = mock(PluginPostLoadHook.class);
        when(postLoadHook1.run(eq(pluginDescriptor), anyMap())).thenReturn(new PluginPostLoadHook.Result(false, null));

        final PluginPostLoadHook postLoadHook2 = mock(PluginPostLoadHook.class);
        when(postLoadHook2.run(eq(pluginDescriptor), anyMap())).thenReturn(new PluginPostLoadHook.Result(true, "Something went wrong"));

        PluginChangeListener listener1 = mock(PluginChangeListener.class);

        pluginLoader.addPluginChangeListener(listener1);
        pluginLoader.addPluginPostLoadHook(postLoadHook1);
        pluginLoader.addPluginPostLoadHook(postLoadHook2);

        pluginLoader.loadPlugin(pluginBundleDescriptor);

        verifyZeroInteractions(listener1);

        assertThat(pluginDescriptor.isInvalid(), is(true));
        assertThat(pluginDescriptor.getStatus().getMessages(), is(singletonList("Something went wrong")));

        final InOrder inOrder = inOrder(postLoadHook1, postLoadHook2);
        inOrder.verify(postLoadHook1).run(eq(pluginDescriptor), anyMap());
        inOrder.verify(postLoadHook2).run(eq(pluginDescriptor), anyMap());
    }

    @Test
    void shouldSendExtensionsInfoToPostLoadHooksDuringLoad() {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);
        when(pluginDescriptor.id()).thenReturn("some-id");

        final Map<String, List<String>> extensionsInfo = singletonMap("elastic-agent", singletonList("1.0"));
        when(pluginOSGiFramework.getExtensionsInfoFromThePlugin("some-id")).thenReturn(extensionsInfo);


        final PluginPostLoadHook postLoadHook = mock(PluginPostLoadHook.class);
        when(postLoadHook.run(pluginDescriptor, extensionsInfo)).thenReturn(new PluginPostLoadHook.Result(false, null));

        pluginLoader.addPluginPostLoadHook(postLoadHook);
        pluginLoader.loadPlugin(pluginBundleDescriptor);


        verify(postLoadHook).run(pluginDescriptor, extensionsInfo);
    }

    @Test
    void shouldNotifyAllPluginChangeListenerOncePluginIsLoaded() {
        GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);

        PluginChangeListener listener1 = mock(PluginChangeListener.class);
        PluginChangeListener listener2 = mock(PluginChangeListener.class);
        PluginChangeListener listener3 = mock(PluginChangeListener.class);

        pluginLoader.addPluginChangeListener(listener1);
        pluginLoader.addPluginChangeListener(listener2);
        pluginLoader.addPluginChangeListener(listener3);

        pluginLoader.loadPlugin(pluginBundleDescriptor);

        final InOrder inOrder = inOrder(listener1, listener2, listener3);
        inOrder.verify(listener1, times(1)).pluginLoaded(pluginDescriptor);
        inOrder.verify(listener2, times(1)).pluginLoaded(pluginDescriptor);
        inOrder.verify(listener3, times(1)).pluginLoaded(pluginDescriptor);
    }

    @Test
    void shouldNotifyPluginPostLoadHooksAndChangeListenersForEachPluginInBundleOncePluginIsLoaded() {
        PluginChangeListener listener = mock(PluginChangeListener.class);
        PluginPostLoadHook postLoadHook = mock(PluginPostLoadHook.class);

        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        when(postLoadHook.run(eq(pluginDescriptor1), anyMap())).thenReturn(new PluginPostLoadHook.Result(false, null));
        when(postLoadHook.run(eq(pluginDescriptor2), anyMap())).thenReturn(new PluginPostLoadHook.Result(false, null));

        pluginLoader.addPluginChangeListener(listener);
        pluginLoader.addPluginPostLoadHook(postLoadHook);
        pluginLoader.loadPlugin(pluginBundleDescriptor);

        final InOrder inOrder = inOrder(listener, postLoadHook);
        inOrder.verify(postLoadHook, times(1)).run(eq(pluginDescriptor1), anyMap());
        inOrder.verify(postLoadHook, times(1)).run(eq(pluginDescriptor2), anyMap());
        inOrder.verify(listener, times(1)).pluginLoaded(pluginDescriptor1);
        inOrder.verify(listener, times(1)).pluginLoaded(pluginDescriptor2);
    }

    @Test
    void shouldMarkThePluginAsInvalidAndUnloadItIfAnyPluginChangeListenerThrowsAnExceptionDuringLoad() {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.usingId("some.id.1", null, null, false);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);
        pluginBundleDescriptor.setBundle(mock(Bundle.class));

        final PluginChangeListener changeListener = mock(PluginChangeListener.class);
        doThrow(new RuntimeException("some error")).when(changeListener).pluginLoaded(pluginDescriptor);

        try {
            pluginLoader.addPluginChangeListener(changeListener);
            pluginLoader.loadPlugin(pluginBundleDescriptor);
            fail("should have thrown an exception");
        } catch (Exception e) {
            assertThat(pluginDescriptor.isInvalid(), is(true));
            assertThat(pluginDescriptor.getStatus().getMessages(), is(singletonList("some error")));

            assertThat(e.getMessage(), containsString("Failed to load plugin:"));
            verify(pluginOSGiFramework, times(1)).unloadPlugin(pluginBundleDescriptor);
        }
    }

    @Test
    void shouldNotCallPostLoadHooksAndListenersIfBundleFailsToLoad() {
        PluginChangeListener changeListener = mock(PluginChangeListener.class);
        PluginPostLoadHook postLoadHook = mock(PluginPostLoadHook.class);

        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.usingId("plugin1", null, null, false);
        GoPluginBundleDescriptor goPluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);

        when(pluginOSGiFramework.loadPlugin(goPluginBundleDescriptor)).then(invocation -> {
            goPluginBundleDescriptor.markAsInvalid(singletonList("Ouch!"), null);
            goPluginBundleDescriptor.setBundle(mock(Bundle.class));
            return goPluginBundleDescriptor.bundle();
        });

        pluginLoader.addPluginChangeListener(changeListener);
        pluginLoader.addPluginPostLoadHook(postLoadHook);

        pluginLoader.loadPlugin(goPluginBundleDescriptor);

        verifyZeroInteractions(postLoadHook);
        verify(changeListener, times(0)).pluginLoaded(pluginDescriptor);
        verify(changeListener, times(1)).pluginUnLoaded(pluginDescriptor);
    }

    @Test
    void shouldRunOtherUnloadListenersAndUnloadPluginBundleEvenIfAListenerFails() {
        final GoPluginDescriptor pluginDescriptor = mock(GoPluginDescriptor.class);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);
        pluginBundleDescriptor.setBundle(mock(Bundle.class));

        PluginChangeListener listenerWhichWorks1 = mock(PluginChangeListener.class, "Listener Which Works: 1");
        PluginChangeListener listenerWhichWorks2 = mock(PluginChangeListener.class, "Listener Which Works: 2");
        PluginChangeListener listenerWhichThrowsWhenUnloading = mock(PluginChangeListener.class, "Listener Which Throws");
        doThrow(new RuntimeException("Fail!")).when(listenerWhichThrowsWhenUnloading).pluginUnLoaded(pluginDescriptor);

        pluginLoader.addPluginChangeListener(listenerWhichWorks1);
        pluginLoader.addPluginChangeListener(listenerWhichThrowsWhenUnloading);
        pluginLoader.addPluginChangeListener(listenerWhichWorks2);

        pluginLoader.unloadPlugin(pluginBundleDescriptor);

        verify(listenerWhichWorks1, times(1)).pluginUnLoaded(pluginDescriptor);
        verify(listenerWhichThrowsWhenUnloading, times(1)).pluginUnLoaded(pluginDescriptor);
        verify(listenerWhichWorks2, times(1)).pluginUnLoaded(pluginDescriptor);

        verify(pluginOSGiFramework, times(1)).unloadPlugin(pluginBundleDescriptor);
    }

    @Test
    void shouldCallUnloadListenersForEveryPluginInBundle() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);
        pluginBundleDescriptor.setBundle(mock(Bundle.class));

        PluginChangeListener pluginChangeListener = mock(PluginChangeListener.class, "Listener Which Works: 1");

        pluginLoader.addPluginChangeListener(pluginChangeListener);
        pluginLoader.unloadPlugin(pluginBundleDescriptor);

        verify(pluginChangeListener, times(1)).pluginUnLoaded(pluginDescriptor1);
        verify(pluginChangeListener, times(1)).pluginUnLoaded(pluginDescriptor2);
        verify(pluginOSGiFramework, times(1)).unloadPlugin(pluginBundleDescriptor);
    }

    @Test
    void shouldNotUnloadAPluginIfItsBundleIsNull() {
        GoPluginBundleDescriptor pluginDescriptor = mock(GoPluginBundleDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(null);

        pluginLoader.unloadPlugin(pluginDescriptor);

        verifyZeroInteractions(pluginOSGiFramework);
    }

    @Test
    void shouldUnloadPluginIfBundleFailsToLoad() {
        GoPluginBundleDescriptor goPluginBundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("plugin1", null, null, false));

        when(pluginOSGiFramework.loadPlugin(goPluginBundleDescriptor)).then(invocation -> {
            goPluginBundleDescriptor.markAsInvalid(singletonList("Ouch!"), null);
            goPluginBundleDescriptor.setBundle(mock(Bundle.class));
            return goPluginBundleDescriptor.bundle();
        });

        pluginLoader.loadPlugin(goPluginBundleDescriptor);

        verify(pluginOSGiFramework, times(1)).unloadPlugin(goPluginBundleDescriptor);
    }

    @Test
    void shouldUnloadPluginIfBundleThrowsExceptionDuringLoad() {
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.usingId("plugin1", null, null, false);
        GoPluginBundleDescriptor pluginBundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);

        when(pluginOSGiFramework.loadPlugin(pluginBundleDescriptor)).then(invocation -> {
            pluginBundleDescriptor.setBundle(mock(Bundle.class));
            throw new UnsupportedOperationException("Ouch!");
        });

        try {
            pluginLoader.loadPlugin(pluginBundleDescriptor);
            fail("Should have failed to load the plugin");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), containsString("Failed to load plugin"));
        }

        assertThat(pluginDescriptor.isInvalid(), is(true));
        assertThat(pluginDescriptor.getStatus().getMessages(), is(singletonList("Ouch!")));
        verify(pluginOSGiFramework, times(1)).unloadPlugin(pluginBundleDescriptor);
    }
}