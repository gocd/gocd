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
package com.thoughtworks.go.plugin.infra.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class PluginChangeNotifierTest {
    private PluginChangeNotifier pluginChangeNotifier;

    @BeforeEach
    void setUp() {
        pluginChangeNotifier = new PluginChangeNotifier();
    }

    @Test
    void shouldNotifyWhenNewPluginIsAdded() {
        final PluginJarChangeListener listener = mock(PluginJarChangeListener.class);
        List<BundleOrPluginFileDetails> knownPlugins = Collections.emptyList();
        BundleOrPluginFileDetails pluginOne = mock(BundleOrPluginFileDetails.class);
        BundleOrPluginFileDetails pluginTwo = mock(BundleOrPluginFileDetails.class);
        BundleOrPluginFileDetails pluginThree = mock(BundleOrPluginFileDetails.class);
        List<BundleOrPluginFileDetails> newPlugins = List.of(pluginOne, pluginTwo, pluginThree);

        pluginChangeNotifier.notify(listener, knownPlugins, newPlugins);

        verify(listener).pluginJarAdded(pluginOne);
        verify(listener, never()).pluginJarRemoved(any());
        verify(listener, never()).pluginJarUpdated(any());
    }

    @Test
    void shouldNotifyWhenPluginIsUpdated() {
        final PluginJarChangeListener listener = mock(PluginJarChangeListener.class);
        BundleOrPluginFileDetails pluginOne = mock(BundleOrPluginFileDetails.class);
        BundleOrPluginFileDetails pluginTwo = mock(BundleOrPluginFileDetails.class);
        BundleOrPluginFileDetails pluginThree = mock(BundleOrPluginFileDetails.class);

        List<BundleOrPluginFileDetails> knownPlugins = List.of(pluginOne, pluginTwo, pluginThree);
        List<BundleOrPluginFileDetails> newPlugins = List.of(pluginOne, pluginTwo, pluginThree);
        when(pluginOne.doesTimeStampDiffer(pluginOne)).thenReturn(true);

        pluginChangeNotifier.notify(listener, knownPlugins, newPlugins);

        verify(listener).pluginJarUpdated(pluginOne);
        verify(listener, never()).pluginJarAdded(any());
        verify(listener, never()).pluginJarRemoved(any());
    }

    @Test
    void shouldNotifyWhenPluginIsRemoved() {
        final PluginJarChangeListener listener = mock(PluginJarChangeListener.class);
        BundleOrPluginFileDetails pluginOne = mock(BundleOrPluginFileDetails.class);
        BundleOrPluginFileDetails pluginTwo = mock(BundleOrPluginFileDetails.class);
        BundleOrPluginFileDetails pluginThree = mock(BundleOrPluginFileDetails.class);

        List<BundleOrPluginFileDetails> knownPlugins = List.of(pluginOne, pluginTwo, pluginThree);
        List<BundleOrPluginFileDetails> newPlugins = List.of(pluginOne, pluginTwo);

        pluginChangeNotifier.notify(listener, knownPlugins, newPlugins);

        verify(listener).pluginJarRemoved(pluginThree);
        verify(listener, never()).pluginJarAdded(any());
        verify(listener, never()).pluginJarUpdated(any());
    }

    @Test
    void shouldNotifyRemovedBeforeAddWhenPluginJarIsRenamed() {
        final PluginJarChangeListener listener = mock(PluginJarChangeListener.class);
        File pluginJarOne = mock(File.class);
        File pluginJarTwo = mock(File.class);
        File pluginWorkDir = mock(File.class);
        BundleOrPluginFileDetails pluginOne = new BundleOrPluginFileDetails(pluginJarOne, false, pluginWorkDir);
        BundleOrPluginFileDetails pluginTwo = new BundleOrPluginFileDetails(pluginJarTwo, false, pluginWorkDir);

        List<BundleOrPluginFileDetails> knownPlugins = List.of(pluginOne);
        List<BundleOrPluginFileDetails> newPlugins = List.of(pluginTwo);

        when(pluginJarOne.getName()).thenReturn("plugin-1.0.0.jar");
        when(pluginJarTwo.getName()).thenReturn("plugin-2.0.0.jar");

        pluginChangeNotifier.notify(listener, knownPlugins, newPlugins);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).pluginJarRemoved(pluginOne);
        inOrder.verify(listener).pluginJarAdded(pluginTwo);
        verify(listener, never()).pluginJarUpdated(any());
    }
}
