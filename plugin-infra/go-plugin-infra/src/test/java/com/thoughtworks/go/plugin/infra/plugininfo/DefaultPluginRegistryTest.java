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
package com.thoughtworks.go.plugin.infra.plugininfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPluginRegistryTest {
    private DefaultPluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultPluginRegistry();
    }

    @Test
    void shouldMarkAllPluginsInBundleAsInvalidWithMessage() {
        File pluginFile = mock(File.class);
        String message = "random failure";

        final GoPluginDescriptor pluginDescriptor1 = getPluginDescriptor("plugin-id_1", "Abc", "1.0", pluginFile.getAbsolutePath());
        final GoPluginDescriptor pluginDescriptor2 = getPluginDescriptor("plugin-id_2", "Xyz", "1.0", pluginFile.getAbsolutePath());
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);
        registry.loadPlugin(descriptor);

        registry.markPluginInvalid(descriptor.bundleSymbolicName(), singletonList(message));

        GoPluginDescriptor loadedDescriptor1 = registry.plugins().get(0);
        assertThat(loadedDescriptor1.isInvalid()).isTrue();
        assertThat(loadedDescriptor1.getStatus().getMessages()).contains(message);

        GoPluginDescriptor loadedDescriptor2 = registry.plugins().get(1);
        assertThat(loadedDescriptor2.isInvalid()).isTrue();
        assertThat(loadedDescriptor2.getStatus().getMessages()).contains(message);
    }

    private GoPluginDescriptor getPluginDescriptor(String id, String name, String version, String pluginJarLocation) {
        return GoPluginDescriptor.builder().id(id)
                .version("1.0")
                .about(GoPluginDescriptor.About.builder()
                        .name(name)
                        .targetGoVersion("19.5")
                        .version(version)
                        .build())
                .pluginJarFileLocation(pluginJarLocation)
                .isBundledPlugin(true)
                .build();
    }


    @Test
    void testThrowExceptionWhenBundleSymbolicNameNotFound() {
        assertThatCode(() -> registry.markPluginInvalid("invalid-bundle-symbolic-name", singletonList("some message")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid bundle symbolic name 'invalid-bundle-symbolic-name'");
    }

    @Test
    void shouldThrowExceptionWhenBundleSymbolicNameIsNull() {
        assertThatCode(() -> registry.markPluginInvalid(null, singletonList("some message")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid bundle symbolic name 'null'");
    }

    @Test
    void shouldListAllLoadedPlugins() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("id1").isBundledPlugin(true).build();
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor1));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("id2").isBundledPlugin(true).build();
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor2));

        assertThat(registry.plugins().size()).isEqualTo(2);
        assertThat(registry.plugins()).contains(pluginDescriptor1, pluginDescriptor2);
    }

    @Test
    void shouldRegisterAllPluginsInABundle() {
        GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("id1").isBundledPlugin(true).build();
        GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("id2").isBundledPlugin(true).build();

        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2));

        assertThat(registry.plugins().size()).isEqualTo(2);
        assertThat(registry.plugins()).contains(pluginDescriptor1, pluginDescriptor2);
    }

    @Test
    void shouldReturnThePluginWithGivenId() {
        final GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("id").isBundledPlugin(true).build();
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor));

        assertThat(registry.getPlugin("id")).isEqualTo(pluginDescriptor);
        assertThat(registry.getPlugin("ID")).isEqualTo(pluginDescriptor);
        assertThat(registry.getPlugin("Id")).isEqualTo(pluginDescriptor);
    }

    @Test
    void shouldUnloadPluginFromRegistry() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("id1")
                .bundleLocation(new File("location-one"))
                .pluginJarFileLocation("location-one.jar")
                .isBundledPlugin(true)
                .build();
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor1));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("id2")
                .bundleLocation(new File("location-two"))
                .pluginJarFileLocation("location-two.jar")
                .isBundledPlugin(true)
                .build();
        registry.loadPlugin(new GoPluginBundleDescriptor(pluginDescriptor2));

        assertThat(registry.plugins().size()).isEqualTo(2);
        assertThat(registry.plugins()).contains(pluginDescriptor1, pluginDescriptor2);

        registry.unloadPlugin(new GoPluginBundleDescriptor(pluginDescriptor2));

        assertThat(registry.plugins().size()).isEqualTo(1);
        assertThat(registry.plugins()).contains(pluginDescriptor1);
    }

    @Test
    void shouldUnloadAllPluginsInABundleFromRegistry() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("id1")
                .bundleLocation(new File("location-one"))
                .pluginJarFileLocation("location-one.jar")
                .isBundledPlugin(true)
                .build();
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("id2")
                .bundleLocation(new File("location-two"))
                .pluginJarFileLocation("location-two.jar")
                .isBundledPlugin(true)
                .build();
        final GoPluginDescriptor pluginDescriptor3 = GoPluginDescriptor.builder().id("id3")
                .bundleLocation(new File("location-two"))
                .pluginJarFileLocation("location-two.jar")
                .isBundledPlugin(true)
                .build();

        final GoPluginBundleDescriptor bundle1 = new GoPluginBundleDescriptor(pluginDescriptor1);
        final GoPluginBundleDescriptor bundle2 = new GoPluginBundleDescriptor(pluginDescriptor2, pluginDescriptor3);

        registry.loadPlugin(bundle1);
        registry.loadPlugin(bundle2);

        assertThat(registry.plugins().size()).isEqualTo(3);
        assertThat(registry.plugins()).contains(pluginDescriptor1, pluginDescriptor2, pluginDescriptor3);

        registry.unloadPlugin(bundle2);

        assertThat(registry.plugins().size()).isEqualTo(1);
        assertThat(registry.plugins()).contains(pluginDescriptor1);
    }

    @Test
    void shouldBeAbleToUnloadThePluginBasedOnFileNameEvenIfTheIDHasBeenChanged() {
        File bundleLocation = mock(File.class);
        when(bundleLocation.getName()).thenReturn("plugin-id");
        GoPluginBundleDescriptor oldBundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("old-plugin-id")
                .bundleLocation(bundleLocation)
                .pluginJarFileLocation("some-plugin.jar")
                .isBundledPlugin(true)
                .build());
        registry.loadPlugin(oldBundleDescriptor);


        GoPluginBundleDescriptor descriptorOfPluginToBeUnloaded = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("new-plugin-id")
                .bundleLocation(bundleLocation)
                .pluginJarFileLocation("some-plugin.jar")
                .isBundledPlugin(true)
                .build());
        GoPluginBundleDescriptor descriptorOfUnloadedPlugin = registry.unloadPlugin(descriptorOfPluginToBeUnloaded);

        assertThat(descriptorOfUnloadedPlugin).isEqualTo(oldBundleDescriptor);
        assertThat(registry.plugins().size()).isEqualTo(0);
    }

    @Test
    void shouldNotUnloadAPluginIfItWasNotLoadedBefore() {
        assertThatCode(() -> registry.unloadPlugin(new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("id1").isBundledPlugin(true).build())))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldNotLoadAnyPluginsInBundleIfThereIsOneMorePluginWithTheSameIDAlreadyInTheRegistry() {
        final GoPluginDescriptor pluginDescriptor = getPluginDescriptor("id_Z", "name1", "1.0", "/tmp/path/1");
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);
        registry.loadPlugin(bundleDescriptor);

        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("id_Y")
                .bundleLocation(null)
                .pluginJarFileLocation("/tmp/path/2")
                .isBundledPlugin(true)
                .build();
        final GoPluginDescriptor pluginDescriptor2 = getPluginDescriptor("id_Z", "name2", "2.0", "/tmp/path/1");
        GoPluginBundleDescriptor newPluginBundle = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        try {
            registry.loadPlugin(newPluginBundle);
        } catch (RuntimeException e) {
            assertThat(registry.plugins().size()).isEqualTo(1);
            assertThat(registry.plugins().get(0)).isEqualTo(pluginDescriptor);
        }
    }

    @Test
    void shouldNotLoadPluginIfThereIsOneMorePluginWithTheSameIDAndDifferentCase() {
        GoPluginBundleDescriptor descriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("id1").isBundledPlugin(true).build());
        registry.loadPlugin(descriptor);

        GoPluginBundleDescriptor secondPluginBundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("iD1").isBundledPlugin(true).build());

        assertThatCode(() -> registry.loadPlugin(secondPluginBundleDescriptor))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldFindABundleBySymbolicName() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("plugin.1").build();
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("plugin.2").build();
        final GoPluginBundleDescriptor bundleDescriptor1 = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        final GoPluginDescriptor pluginDescriptor3 = GoPluginDescriptor.builder().id("plugin.3").build();
        final GoPluginDescriptor pluginDescriptor4 = GoPluginDescriptor.builder().id("plugin.4").build();
        final GoPluginBundleDescriptor bundleDescriptor2 = new GoPluginBundleDescriptor(pluginDescriptor3, pluginDescriptor4);

        registry.loadPlugin(bundleDescriptor1);
        registry.loadPlugin(bundleDescriptor2);

        assertThat(registry.getBundleDescriptor(bundleDescriptor1.bundleSymbolicName())).isEqualTo(bundleDescriptor1);
        assertThat(registry.getBundleDescriptor(bundleDescriptor2.bundleSymbolicName())).isEqualTo(bundleDescriptor2);
        assertThat(registry.getBundleDescriptor("NON_EXISTENT")).isNull();
    }

    @Test
    void shouldGetPluginIDForAGivenBundleExtensionClass() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("plugin.1").build();
        pluginDescriptor1.addExtensionClasses(asList("com.path.to.ExtensionClass1", "com.path.to.ExtensionClass2"));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("plugin.2").build();
        pluginDescriptor2.addExtensionClasses(singletonList("com.path.to.ExtensionClass3"));

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass1")).isEqualTo("plugin.1");
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass2")).isEqualTo("plugin.1");
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass3")).isEqualTo("plugin.2");
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.DOES_NOT_EXIST")).isNull();
    }

    @Test
    void shouldHandleLegacyPluginWhichHasNoDefinedExtensions_WhenAskedForPluginID() {
        final GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("plugin.1").build();
        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor);

        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.ExtensionClass1")).isEqualTo("plugin.1");
        assertThat(registry.pluginIDFor(bundleDescriptor.bundleSymbolicName(), "com.path.to.DOES_NOT_EXIST")).isEqualTo("plugin.1");
    }

    @Test
    void shouldProvideAllRegisteredExtensionsAcrossPluginsInABundle() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("plugin.1").build();
        pluginDescriptor1.addExtensionClasses(asList("com.path.to.ExtensionClass1", "com.path.to.ExtensionClass2"));

        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("plugin.2").build();
        pluginDescriptor2.addExtensionClasses(singletonList("com.path.to.ExtensionClass3"));

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.extensionClassesIn(bundleDescriptor.bundleSymbolicName())).isEqualTo(asList("com.path.to.ExtensionClass1", "com.path.to.ExtensionClass2", "com.path.to.ExtensionClass3"));
    }

    @Test
    void shouldSayThatALegacyPluginHasNoExtensionClassesInTheWholeBundle() {
        GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder().id("iD1").isBundledPlugin(true).build());
        registry.loadPlugin(bundleDescriptor);

        assertThat(registry.extensionClassesIn(bundleDescriptor.bundleSymbolicName())).isEqualTo(emptyList());
    }
}
