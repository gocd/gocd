/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra.plugininfo;

import java.util.Arrays;

import org.junit.Test;

import static com.thoughtworks.go.util.OperatingSystem.LINUX;
import static com.thoughtworks.go.util.OperatingSystem.WINDOWS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GoPluginDescriptorTest {
    @Test
    public void shouldMatchValidOSesAgainstCurrentOS() throws Exception {
        assertThat(descriptorWithTargetOSes().isCurrentOSValidForThisPlugin(LINUX), is(true));
        assertThat(descriptorWithTargetOSes().isCurrentOSValidForThisPlugin(WINDOWS), is(true));

        assertThat(descriptorWithTargetOSes("Linux").isCurrentOSValidForThisPlugin(LINUX), is(true));
        assertThat(descriptorWithTargetOSes("Windows").isCurrentOSValidForThisPlugin(LINUX), is(false));

        assertThat(descriptorWithTargetOSes("Windows", "Linux").isCurrentOSValidForThisPlugin(LINUX), is(true));
        assertThat(descriptorWithTargetOSes("Windows", "SunOS", "Mac OS X").isCurrentOSValidForThisPlugin(LINUX), is(false));
    }

    @Test
    public void shouldDoACaseInsensitiveMatchForValidOSesAgainstCurrentOS() throws Exception {
        assertThat(descriptorWithTargetOSes("linux").isCurrentOSValidForThisPlugin(LINUX), is(true));
        assertThat(descriptorWithTargetOSes("LiNuX").isCurrentOSValidForThisPlugin(LINUX), is(true));
        assertThat(descriptorWithTargetOSes("windows").isCurrentOSValidForThisPlugin(LINUX), is(false));
        assertThat(descriptorWithTargetOSes("windOWS").isCurrentOSValidForThisPlugin(LINUX), is(false));

        assertThat(descriptorWithTargetOSes("WinDOWs", "LINUx").isCurrentOSValidForThisPlugin(LINUX), is(true));
        assertThat(descriptorWithTargetOSes("WINDows", "Sunos", "Mac os x").isCurrentOSValidForThisPlugin(LINUX), is(false));
    }

    private GoPluginDescriptor descriptorWithTargetOSes(String... oses) {
        return new GoPluginDescriptor(null, null, new GoPluginDescriptor.About(null, null, null, null, null, Arrays.asList(oses)), null, null, false);
    }
}
