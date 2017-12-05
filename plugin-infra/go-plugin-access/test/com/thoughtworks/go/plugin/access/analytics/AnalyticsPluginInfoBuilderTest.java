/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.analytics;


import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.analytics.Capabilities;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AnalyticsPluginInfoBuilderTest {

    private AnalyticsExtension extension;

    @Before
    public void setUp() throws Exception {
        extension = mock(AnalyticsExtension.class);
        stub(extension.getCapabilities(any(String.class))).toReturn(new Capabilities( true));
    }

    @Test
    public void shouldBuildPluginInfoWithCapablities() throws Exception {
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin1", null, null, null, null, false);
        Capabilities capabilities = new Capabilities(true);

        when(extension.getCapabilities(descriptor.id())).thenReturn(capabilities);

        AnalyticsPluginInfo pluginInfo = new AnalyticsPluginInfoBuilder(extension).pluginInfoFor(descriptor);

        assertThat(pluginInfo.getCapabilities(), is(capabilities));
    }
}
