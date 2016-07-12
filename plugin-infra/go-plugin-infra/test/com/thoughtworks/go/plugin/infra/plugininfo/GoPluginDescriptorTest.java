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
}
