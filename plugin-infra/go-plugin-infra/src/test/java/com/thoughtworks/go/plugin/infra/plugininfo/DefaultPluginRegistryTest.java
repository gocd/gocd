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
package com.thoughtworks.go.plugin.infra.plugininfo;

import java.io.File;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class DefaultPluginRegistryTest {

    private DefaultPluginRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = new DefaultPluginRegistry();
    }

    @Test
    public void shouldMarkPluginAsInvalidWithMessage() throws Exception {
        String pluginId = "plugin-id";
        File pluginFile = mock(File.class);
        String message = "random failure";
        DefaultPluginRegistry spy = spy(registry);
        GoPluginDescriptor descriptor = new GoPluginDescriptor(pluginId, "1.0", null, null, pluginFile, true);
        spy.loadPlugin(descriptor);

        spy.markPluginInvalid(pluginId, Arrays.asList(message));

        GoPluginDescriptor loadedDescriptor = spy.plugins().get(0);
        assertThat(loadedDescriptor.isInvalid(), is(true));
        assertThat(loadedDescriptor.getStatus().getMessages(), hasItem(message));
    }

    @Test
    public void testThrowExceptionWhenPluginNotFound() throws Exception {
        try {
            registry.markPluginInvalid("invalid-plugin-id", Arrays.asList("some message"));
            fail("should have thrown exception for plugin not found ");
        } catch (Exception e) {
            assertThat(e instanceof RuntimeException, is(true));
            assertThat(e.getMessage(), is("Invalid plugin identifier 'invalid-plugin-id'"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenPluginIdIsNull() throws Exception {
        try {
            registry.markPluginInvalid(null, Arrays.asList("some message"));
            fail("should have thrown exception for plugin not found ");
        } catch (Exception e) {
            assertThat(e instanceof RuntimeException, is(true));
            assertThat(e.getMessage(), is("Invalid plugin identifier 'null'"));
        }

    }

    @Test
    public void shouldListAllLoadedPlugins() throws Exception {
        GoPluginDescriptor descriptor1 = GoPluginDescriptor.usingId("id1", null, null, true);
        registry.loadPlugin(descriptor1);

        GoPluginDescriptor descriptor2 = GoPluginDescriptor.usingId("id2", null, null, true);
        registry.loadPlugin(descriptor2);

        assertThat(registry.plugins().size(), is(2));
        assertThat(registry.plugins(), hasItems(descriptor1, descriptor2));
    }

    @Test
    public void shouldReturnThePluginWithGivenId() throws Exception {
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("id", null, null, true);
        registry.loadPlugin(descriptor);
        assertThat(registry.getPlugin("id"), is(descriptor));
    }

    @Test
    public void shouldUnloadPluginFromRegistry() throws Exception {
        GoPluginDescriptor descriptor1 = GoPluginDescriptor.usingId("id1", "location-one.jar", new File("location-one"), true);
        registry.loadPlugin(descriptor1);

        GoPluginDescriptor descriptor2 = GoPluginDescriptor.usingId("id2", "location-two.jar", new File("location-two"), true);
        registry.loadPlugin(descriptor2);

        assertThat(registry.plugins().size(), is(2));
        assertThat(registry.plugins(), hasItems(descriptor1, descriptor2));

        registry.unloadPlugin(descriptor2);

        assertThat(registry.plugins().size(), is(1));
        assertThat(registry.plugins(), hasItems(descriptor1));
    }

    @Test
    public void shouldBeAbleToUnloadThePluginBasedOnFileNameEvenIfTheIDHasBeenChanged() throws Exception {
        File bundleLocation = mock(File.class);
        when(bundleLocation.getName()).thenReturn("plugin-id");
        GoPluginDescriptor descriptor1 = GoPluginDescriptor.usingId("id", "some-plugin.jar", bundleLocation, true);
        registry.loadPlugin(descriptor1);
        assertThat(descriptor1.id(), is("id"));


        GoPluginDescriptor descriptorOfPluginToBeUnloaded = GoPluginDescriptor.usingId("plugin-id", "some-plugin.jar", bundleLocation, true);
        GoPluginDescriptor descriptorOfUnloadedPlugin = registry.unloadPlugin(descriptorOfPluginToBeUnloaded);

        assertThat(descriptorOfUnloadedPlugin.id(), is("id"));
        assertThat(registry.plugins().size(), is(0));
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotUnloadAPluginIfItWasNotLoadedBefore() throws Exception {
        registry.unloadPlugin(GoPluginDescriptor.usingId("id1", null, null, true));
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotLoadPluginIfThereIsOneMorePluginWithTheSameID() throws Exception {
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("id1", null, null, true);
        registry.loadPlugin(descriptor);

        GoPluginDescriptor secondPluginBundleLocation = GoPluginDescriptor.usingId("id1", null, null, true);
        registry.loadPlugin(secondPluginBundleLocation);
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotLoadPluginIfThereIsOneMorePluginWithTheSameIDAndDifferentCase() throws Exception {
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("id1", null, null, true);
        registry.loadPlugin(descriptor);

        GoPluginDescriptor secondPluginBundleLocation = GoPluginDescriptor.usingId("iD1", null, null, true);
        registry.loadPlugin(secondPluginBundleLocation);
    }

}
