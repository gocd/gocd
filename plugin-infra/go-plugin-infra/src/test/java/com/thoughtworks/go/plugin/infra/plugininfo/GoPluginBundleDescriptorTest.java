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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class GoPluginBundleDescriptorTest {
    @Test
    void bundleIsConsideredInvalidIfAtleastOnePluginInItIsInvalid() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("plugin.1").build();
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("plugin.2").build();

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        assertThat(bundleDescriptor.isInvalid()).isFalse();

        pluginDescriptor1.markAsInvalid(singletonList("Ouch!"), null);

        assertThat(bundleDescriptor.isInvalid()).isTrue();
    }

    @Test
    void shouldAggregateStatusMessagesFromAcrossPluginsInABundle() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.builder().id("plugin.1").build();
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.builder().id("plugin.2").build();

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        pluginDescriptor1.markAsInvalid(singletonList("Ouch!"), null);
        pluginDescriptor2.markAsInvalid(singletonList("Second one is bad too!"), null);

        /* Overwrites previous message. */
        assertThat(bundleDescriptor.getMessages()).isEqualTo(Arrays.asList("Second one is bad too!", "Second one is bad too!"));
    }
}