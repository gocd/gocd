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
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singletonList;
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
    public void setUp() {
        registry = new DefaultPluginRegistry();
    }

    @Test
    public void shouldMarkAllPluginsInBundleAsInvalidWithMessage() {
        File pluginFile = mock(File.class);
        String message = "random failure";

        final GoPluginDescriptor pluginDescriptor1 = new GoPluginDescriptor("plugin-id_1", "1.0", null, null, pluginFile, true);
        final GoPluginDescriptor pluginDescriptor2 = new GoPluginDescriptor("plugin-id_2", "1.0", null, null, pluginFile, true);
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);
        registry.loadPlugin(descriptor);

        registry.markPluginInvalid("plugin-id_2", singletonList(message));

        GoPluginDescriptor loadedDescriptor1 = registry.plugins().get(0);
        assertThat(loadedDescriptor1.isInvalid(), is(true));
        assertThat(loadedDescriptor1.getStatus().getMessages(), hasItem(message));

        GoPluginDescriptor loadedDescriptor2 = registry.plugins().get(1);
        assertThat(loadedDescriptor2.isInvalid(), is(true));
        assertThat(loadedDescriptor2.getStatus().getMessages(), hasItem(message));
    }

    @Test
    public void testThrowExceptionWhenPluginNotFound() {
        try {
            registry.markPluginInvalid("invalid-plugin-id", singletonList("some message"));
            fail("should have thrown exception for plugin not found ");
        } catch (Exception e) {
            assertThat(e instanceof RuntimeException, is(true));
            assertThat(e.getMessage(), is("Invalid plugin identifier 'invalid-plugin-id'"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenPluginIdIsNull() {
        try {
            registry.markPluginInvalid(null, singletonList("some message"));
            fail("should have thrown exception for plugin not found ");
        } catch (Exception e) {
            assertThat(e instanceof RuntimeException, is(true));
            assertThat(e.getMessage(), is("Invalid plugin identifier 'null'"));
        }

    }

    @Test
    public void shouldListAllLoadedPlugins() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("id1", null, null, true);
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor1));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("id2", null, null, true);
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor2));

        assertThat(registry.plugins().size(), is(2));
        assertThat(registry.plugins(), hasItems(pluginDescriptor1, pluginDescriptor2));
    }

    @Test
    public void shouldRegisterAllPluginsInABundle() {
        GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("id1", null, null, true);
        GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("id2", null, null, true);

        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2));

        assertThat(registry.plugins().size(), is(2));
        assertThat(registry.plugins(), hasItems(pluginDescriptor1, pluginDescriptor2));
    }

    @Test
    public void shouldReturnThePluginWithGivenId() {
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("id", null, null, true));
        registry.loadPlugin(descriptor);

        assertThat(registry.getPlugin("id"), is(descriptor.descriptor()));
        assertThat(registry.getPlugin("ID"), is(descriptor.descriptor()));
        assertThat(registry.getPlugin("Id"), is(descriptor.descriptor()));
    }

    @Test
    public void shouldUnloadPluginFromRegistry() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("id1", "location-one.jar", new File("location-one"), true);
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor1));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("id2", "location-two.jar", new File("location-two"), true);
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor2));

        assertThat(registry.plugins().size(), is(2));
        assertThat(registry.plugins(), hasItems(pluginDescriptor1, pluginDescriptor2));

        registry.unloadPlugin(new GoPluginBundleDescriptor(pluginDescriptor2));

        assertThat(registry.plugins().size(), is(1));
        assertThat(registry.plugins(), hasItems(pluginDescriptor1));
    }

    @Test
    public void shouldUnloadAllPluginsInABundleFromRegistry() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("id1", "location-one.jar", new File("location-one"), true);
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("id2", "location-two.jar", new File("location-two"), true);
        final GoPluginDescriptor pluginDescriptor3 = GoPluginDescriptor.usingId("id3", "location-two.jar", new File("location-two"), true);

        final GoPluginBundleDescriptor bundle1 = new GoPluginBundleDescriptor(pluginDescriptor1);
        final GoPluginBundleDescriptor bundle2 = new GoPluginBundleDescriptor(pluginDescriptor2, pluginDescriptor3);

        registry.loadPlugin(bundle1);
        registry.loadPlugin(bundle2);

        assertThat(registry.plugins().size(), is(3));
        assertThat(registry.plugins(), hasItems(pluginDescriptor1, pluginDescriptor2, pluginDescriptor3));

        registry.unloadPlugin(bundle2);

        assertThat(registry.plugins().size(), is(1));
        assertThat(registry.plugins(), hasItems(pluginDescriptor1));
    }

    @Test
    public void shouldBeAbleToUnloadThePluginBasedOnFileNameEvenIfTheIDHasBeenChanged() {
        File bundleLocation = mock(File.class);
        when(bundleLocation.getName()).thenReturn("plugin-id");
        GoPluginBundleDescriptor descriptor1 = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("id", "some-plugin.jar", bundleLocation, true));
        registry.loadPlugin(descriptor1);


        GoPluginBundleDescriptor descriptorOfPluginToBeUnloaded = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("plugin-id", "some-plugin.jar", bundleLocation, true));
        GoPluginBundleDescriptor descriptorOfUnloadedPlugin = registry.unloadPlugin(descriptorOfPluginToBeUnloaded);

        assertThat(descriptorOfUnloadedPlugin.id(), is("id"));
        assertThat(registry.plugins().size(), is(0));
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotUnloadAPluginIfItWasNotLoadedBefore() {
        registry.unloadPlugin(new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("id1", null, null, true)));
    }

    @Test
    public void shouldNotLoadAnyPluginsInBundleIfThereIsOneMorePluginWithTheSameIDAlreadyInTheRegistry() {
        final GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor("id_Z", "1", new GoPluginDescriptor.About("name1", "1.0", "19.5", null, null, null), "/tmp/path/1", null, true);
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);
        registry.loadPlugin(bundleDescriptor);

        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("id_Y", "/tmp/path/2", null, true);
        final GoPluginDescriptor pluginDescriptor2 = new GoPluginDescriptor("id_Z", "1", new GoPluginDescriptor.About("name2", "2.0", "19.5", null, null, null), "/tmp/path/1", null, true);
        GoPluginBundleDescriptor newPluginBundle = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        try {
            registry.loadPlugin(newPluginBundle);
        } catch (RuntimeException e) {
            assertThat(registry.plugins().size(), is(1));
            assertThat(registry.plugins().get(0), is(pluginDescriptor));
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotLoadPluginIfThereIsOneMorePluginWithTheSameIDAndDifferentCase() {
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("id1", null, null, true));
        registry.loadPlugin(descriptor);

        GoPluginBundleDescriptor secondPluginBundleLocation = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("iD1", null, null, true));
        registry.loadPlugin(secondPluginBundleLocation);
    }

}
