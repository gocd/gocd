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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class GoPluginDescriptorTest {
    @Test
    void shouldMatchValidOSesAgainstCurrentOS() throws Exception {
        assertThat(descriptorWithTargetOSes().isCurrentOSValidForThisPlugin("Linux")).isTrue();
        assertThat(descriptorWithTargetOSes().isCurrentOSValidForThisPlugin("Windows")).isTrue();

        assertThat(descriptorWithTargetOSes("Linux").isCurrentOSValidForThisPlugin("Linux")).isTrue();
        assertThat(descriptorWithTargetOSes("Windows").isCurrentOSValidForThisPlugin("Linux")).isFalse();

        assertThat(descriptorWithTargetOSes("Windows", "Linux").isCurrentOSValidForThisPlugin("Linux")).isTrue();
        assertThat(descriptorWithTargetOSes("Windows", "SunOS", "Mac OS X").isCurrentOSValidForThisPlugin("Linux")).isFalse();
    }

    @Test
    void shouldDoACaseInsensitiveMatchForValidOSesAgainstCurrentOS() throws Exception {
        assertThat(descriptorWithTargetOSes("linux").isCurrentOSValidForThisPlugin("Linux")).isTrue();
        assertThat(descriptorWithTargetOSes("LiNuX").isCurrentOSValidForThisPlugin("Linux")).isTrue();
        assertThat(descriptorWithTargetOSes("windows").isCurrentOSValidForThisPlugin("Linux")).isFalse();
        assertThat(descriptorWithTargetOSes("windOWS").isCurrentOSValidForThisPlugin("Linux")).isFalse();

        assertThat(descriptorWithTargetOSes("WinDOWs", "LINUx").isCurrentOSValidForThisPlugin("Linux")).isTrue();
        assertThat(descriptorWithTargetOSes("WINDows", "Sunos", "Mac os x").isCurrentOSValidForThisPlugin("Linux")).isFalse();
    }

    private GoPluginDescriptor descriptorWithTargetOSes(String... oses) {
        return new GoPluginDescriptor(null, null, new GoPluginDescriptor.About(null, null, null, null, null, Arrays.asList(oses)), null, null, false);
    }

    @Test
    void shouldMarkAllPluginsInBundleAsInvalidIfAPluginIsMarkedInvalid() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        pluginDescriptor1.markAsInvalid(singletonList("Ouch!"), new RuntimeException("Failure ..."));

        assertThat(bundleDescriptor.isInvalid()).isTrue();
        assertThat(pluginDescriptor1.isInvalid()).isTrue();
        assertThat(pluginDescriptor2.isInvalid()).isTrue();

        assertThat(pluginDescriptor1.getStatus().getMessages()).isEqualTo(singletonList("Ouch!"));
        assertThat(pluginDescriptor2.getStatus().getMessages()).isEqualTo(singletonList("Ouch!"));
    }
}
