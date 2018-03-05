/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.elasticagent;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1.ElasticAgentRequestProcessorV1;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticAgentRequestProcessorTest {
    @Mock
    private PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    @Mock
    private ElasticAgentRequestProcessorV1 processorForV1;
    @Mock
    private ElasticAgentRequestProcessorV1 processorForV2;
    @Mock
    private ElasticAgentRequestProcessorV1 processorForV3;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    @Mock
    private GoApiRequest goApiRequest;

    private ElasticAgentRequestProcessor processor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        final Map<String, VersionableElasticAgentProcessor> processorMap = new HashMap<>();
        processorMap.put("1.0", processorForV1);
        processorMap.put("2.0", processorForV2);
        processorMap.put("3.0", processorForV3);

        processor = new ElasticAgentRequestProcessor(pluginRequestProcessorRegistry, processorMap);
    }

    @Test
    public void shouldDelegateRequestToElasticAgentRequestProcessorV1WhenRequestApiVersionIsV1() {
        when(goApiRequest.apiVersion()).thenReturn("1.0");

        processor.process(pluginDescriptor, goApiRequest);

        verify(processorForV1).process(pluginDescriptor, goApiRequest);
        verify(processorForV2, times(0)).process(pluginDescriptor, goApiRequest);
        verify(processorForV3, times(0)).process(pluginDescriptor, goApiRequest);
    }

    @Test
    public void shouldDelegateRequestToElasticAgentRequestProcessorV2WhenRequestApiVersionIsV2() {
        when(goApiRequest.apiVersion()).thenReturn("2.0");

        processor.process(pluginDescriptor, goApiRequest);

        verify(processorForV2).process(pluginDescriptor, goApiRequest);

        verify(processorForV1, times(0)).process(pluginDescriptor, goApiRequest);
        verify(processorForV3, times(0)).process(pluginDescriptor, goApiRequest);
    }

    @Test
    public void shouldDelegateRequestToElasticAgentRequestProcessorV3WhenRequestApiVersionIsV3() {
        when(goApiRequest.apiVersion()).thenReturn("3.0");

        processor.process(pluginDescriptor, goApiRequest);

        verify(processorForV3).process(pluginDescriptor, goApiRequest);

        verify(processorForV1, times(0)).process(pluginDescriptor, goApiRequest);
        verify(processorForV2, times(0)).process(pluginDescriptor, goApiRequest);
    }
}
