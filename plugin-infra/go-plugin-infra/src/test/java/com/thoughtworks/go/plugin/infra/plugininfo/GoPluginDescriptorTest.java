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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GoPluginDescriptorTest {
    @Test
    public void shouldMatchValidOSesAgainstCurrentOS() throws Exception {
        assertThat(descriptorWithTargetOSes().isCurrentOSValidForThisPlugin("Linux"), is(true));
        assertThat(descriptorWithTargetOSes().isCurrentOSValidForThisPlugin("Windows"), is(true));

        assertThat(descriptorWithTargetOSes("Linux").isCurrentOSValidForThisPlugin("Linux"), is(true));
        assertThat(descriptorWithTargetOSes("Windows").isCurrentOSValidForThisPlugin("Linux"), is(false));

        assertThat(descriptorWithTargetOSes("Windows", "Linux").isCurrentOSValidForThisPlugin("Linux"), is(true));
        assertThat(descriptorWithTargetOSes("Windows", "SunOS", "Mac OS X").isCurrentOSValidForThisPlugin("Linux"), is(false));
    }

    @Test
    public void shouldDoACaseInsensitiveMatchForValidOSesAgainstCurrentOS() throws Exception {
        assertThat(descriptorWithTargetOSes("linux").isCurrentOSValidForThisPlugin("Linux"), is(true));
        assertThat(descriptorWithTargetOSes("LiNuX").isCurrentOSValidForThisPlugin("Linux"), is(true));
        assertThat(descriptorWithTargetOSes("windows").isCurrentOSValidForThisPlugin("Linux"), is(false));
        assertThat(descriptorWithTargetOSes("windOWS").isCurrentOSValidForThisPlugin("Linux"), is(false));

        assertThat(descriptorWithTargetOSes("WinDOWs", "LINUx").isCurrentOSValidForThisPlugin("Linux"), is(true));
        assertThat(descriptorWithTargetOSes("WINDows", "Sunos", "Mac os x").isCurrentOSValidForThisPlugin("Linux"), is(false));
    }

    private GoPluginDescriptor descriptorWithTargetOSes(String... oses) {
        return new GoPluginDescriptor(null, null, new GoPluginDescriptor.About(null, null, null, null, null, Arrays.asList(oses)), null, null, false);
    }

    @Test
    public void shouldMarkAllPluginsInBundleAsInvalidIfAPluginIsMarkedInvalid() {
        final GoPluginDescriptor pluginDescriptor1 = GoPluginDescriptor.usingId("plugin.1", null, null, false);
        final GoPluginDescriptor pluginDescriptor2 = GoPluginDescriptor.usingId("plugin.2", null, null, false);

        final GoPluginBundleDescriptor bundleDescriptor = new GoPluginBundleDescriptor(pluginDescriptor1, pluginDescriptor2);

        pluginDescriptor1.markAsInvalid(singletonList("Ouch!"), new RuntimeException("Failure ..."));

        assertThat(bundleDescriptor.isInvalid(), is(true));
        assertThat(pluginDescriptor1.isInvalid(), is(true));
        assertThat(pluginDescriptor2.isInvalid(), is(true));

        assertThat(pluginDescriptor1.getStatus().getMessages(), is(singletonList("Ouch!")));
        assertThat(pluginDescriptor2.getStatus().getMessages(), is(singletonList("Ouch!")));
    }
}
