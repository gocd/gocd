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

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ElasticAgentExtensionTest {

    private PluginManager pluginManager;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
    }

    @Test
    public void shouldMakeStatusReportCallForPluginsWhichSupportV2() throws Exception {
        final GoPluginDescriptor.About about = new GoPluginDescriptor.About("ECS Plugin", "2.0", null, null, null, null);

        final GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor("ecs.plugin", "1", about, null, null, false);

        when(pluginManager.isPluginOfType(ElasticAgentPluginConstants.EXTENSION_NAME, "ecs.plugin")).thenReturn(true);
        when(pluginManager.resolveExtensionVersion("ecs.plugin", Arrays.asList("1.0", "2.0"))).thenReturn("2.0");
        when(pluginManager.getPluginDescriptorFor("ecs.plugin")).thenReturn(pluginDescriptor);
        when(pluginManager.submitTo(eq("ecs.plugin"), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, "{\n" +
                "\"view\": \"foo\"\n" +
                "}"));

        final String statusReport = new ElasticAgentExtension(pluginManager).getStatusReport("ecs.plugin");

        assertThat(statusReport, is("foo"));
        assertThat(requestArgumentCaptor.getValue().extension(), is(ElasticAgentPluginConstants.EXTENSION_NAME));
        assertThat(requestArgumentCaptor.getValue().requestName(), is(ElasticAgentPluginConstants.REQUEST_STATUS_REPORT));
    }

    @Test
    public void shouldFetchCapabilitiesForPluginsWhichSupportV2() throws Exception {
        final GoPluginDescriptor.About about = new GoPluginDescriptor.About("ECS Plugin", "2.0", null, null, null, null);

        final GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor("ecs.plugin", "1", about, null, null, false);

        when(pluginManager.isPluginOfType(ElasticAgentPluginConstants.EXTENSION_NAME, "ecs.plugin")).thenReturn(true);
        when(pluginManager.resolveExtensionVersion("ecs.plugin", Arrays.asList("1.0", "2.0"))).thenReturn("2.0");
        when(pluginManager.getPluginDescriptorFor("ecs.plugin")).thenReturn(pluginDescriptor);
        when(pluginManager.submitTo(eq("ecs.plugin"), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE,
                "{\"supports_status_report\":\"true\"}"));

        Capabilities capabilities = new ElasticAgentExtension(pluginManager).getCapabilities("ecs.plugin");

        assertTrue(capabilities.supportsStatusReport());
        assertThat(requestArgumentCaptor.getValue().extension(), is(ElasticAgentPluginConstants.EXTENSION_NAME));
        assertThat(requestArgumentCaptor.getValue().requestName(), is("go.cd.elastic-agent.get-capabilities"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getCapabilitiesIsSupportedOnlyInVersion2() throws Exception {
        final GoPluginDescriptor.About about = new GoPluginDescriptor.About("ECS Plugin", "1.0", null, null, null, null);

        final GoPluginDescriptor pluginDescriptor = new GoPluginDescriptor("ecs.plugin", "1", about, null, null, false);
        when(pluginManager.getPluginDescriptorFor("ecs.plugin")).thenReturn(pluginDescriptor);

        new ElasticAgentExtension(pluginManager).getCapabilities("ecs.plugin");

        verify(pluginManager, times(0)).submitTo(any(), any());
    }
}