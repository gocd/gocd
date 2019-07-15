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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
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

        registry.markPluginInvalid(descriptor.bundleSymbolicName(), singletonList(message));

        GoPluginDescriptor loadedDescriptor1 = registry.plugins().get(0);
        assertThat(loadedDescriptor1.isInvalid(), is(true));
        assertThat(loadedDescriptor1.getStatus().getMessages(), hasItem(message));

        GoPluginDescriptor loadedDescriptor2 = registry.plugins().get(1);
        assertThat(loadedDescriptor2.isInvalid(), is(true));
        assertThat(loadedDescriptor2.getStatus().getMessages(), hasItem(message));
    }

    @Test
    public void testThrowExceptionWhenBundleSymbolicNameNotFound() {
        try {
            registry.markPluginInvalid("invalid-bundle-symbolic-name", singletonList("some message"));
            fail("should have thrown exception for plugin not found ");
        } catch (Exception e) {
            assertThat(e instanceof RuntimeException, is(true));
            assertThat(e.getMessage(), is("Invalid bundle symbolic name 'invalid-bundle-symbolic-name'"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenBundleSymbolicNameIsNull() {
        try {
            registry.markPluginInvalid(null, singletonList("some message"));
            fail("should have thrown exception for plugin not found ");
        } catch (Exception e) {
            assertThat(e instanceof RuntimeException, is(true));
            assertThat(e.getMessage(), is("Invalid bundle symbolic name 'null'"));
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
        final GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.usingId("id", null, null, true);
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor));

        assertThat(registry.getPlugin("id"), is(pluginDescriptor));
        assertThat(registry.getPlugin("ID"), is(pluginDescriptor));
        assertThat(registry.getPlugin("Id"), is(pluginDescriptor));
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
        GoPluginBundleDescriptor oldBundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("old-plugin-id", "some-plugin.jar", bundleLocation, true));
        registry.loadPlugin(oldBundleDescriptor);


        GoPluginBundleDescriptor descriptorOfPluginToBeUnloaded = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("new-plugin-id", "some-plugin.jar", bundleLocation, true));
        GoPluginBundleDescriptor descriptorOfUnloadedPlugin = registry.unloadPlugin(descriptorOfPluginToBeUnloaded);

        assertThat(descriptorOfUnloadedPlugin, is(oldBundleDescriptor));
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

        GoPluginBundleDescriptor secondPluginBundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("iD1", null, null, true));
        registry.loadPlugin(secondPluginBundleDescriptor);
    }

    @Test
    public void shouldFindABundleBySymbolicName() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);
        final GoPluginBundleDescriptor bundleDescriptor1 = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        final GoPluginDescriptor pluginDescriptor3 = GoPluginDescriptor.usingId("plugin.3", null, null, false);
        final GoPluginDescriptor pluginDescriptor4 = GoPluginDescriptor.usingId("plugin.4", null, null, false);
        final GoPluginBundleDescriptor bundleDescriptor2 = new GoPluginBundleDescriptor(pluginDescriptor3, pluginDescriptor4);

        registry.loadPlugin(bundleDescriptor1);
        registry.loadPlugin(bundleDescriptor2);

        assertThat(registry.getBundleDescriptor(bundleDescriptor1.bundleSymbolicName()), is(bundleDescriptor1));
        assertThat(registry.getBundleDescriptor(bundleDescriptor2.bundleSymbolicName()), is(bundleDescriptor2));
        assertThat(registry.getBundleDescriptor("NON_EXISTENT"), is(nullValue()));
    }

    @Test
    public void shouldGetPluginIDForAGivenBundleExtensionClass() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        pluginDescriptor1.addExtensionClasses(asList("com.path.to.ExtensionClass1", "com.path.to.ExtensionClass2"));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);
        pluginDescriptor2.addExtensionClasses(singletonList("com.path.to.ExtensionClass3"));

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass1"), is("plugin.1"));
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass2"), is("plugin.1"));
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass3"), is("plugin.2"));
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.DOES_NOT_EXIST"), is(nullValue()));
    }

    @Test
    public void shouldHandleLegacyPluginWhichHasNoDefinedExtensions_WhenAskedForPluginID() {
        final GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);

        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass1"), is("plugin.1"));
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.DOES_NOT_EXIST"), is("plugin.1"));
    }

    @Test
    public void shouldProvideAllRegisteredExtensionsAcrossPluginsInABundle() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        pluginDescriptor1.addExtensionClasses(asList("com.path.to.ExtensionClass1", "com.path.to.ExtensionClass2"));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);
        pluginDescriptor2.addExtensionClasses(singletonList("com.path.to.ExtensionClass3"));

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.extensionClassesIn(bundleDescriptor.bundleSymbolicName()), is(asList("com.path.to.ExtensionClass1", "com.path.to.ExtensionClass2", "com.path.to.ExtensionClass3")));
    }

    @Test
    public void shouldSayThatALegacyPluginHasNoExtensionClassesInTheWholeBundle() {
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.usingId("iD1", null, null, true));
        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.extensionClassesIn(bundleDescriptor.bundleSymbolicName()), is(emptyList()));
    }
}
