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
package com.thoughtworks.go.server.service.plugins.processor.elasticagent;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1.ElasticAgentRequestProcessorV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.service.plugins.processor.elasticagent.v1.ElasticAgentProcessorRequestsV1.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ElasticAgentRequestProcessorTest {
    @Mock
    private PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    @Mock
    private ElasticAgentRequestProcessorV1 processorForV1;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    @Mock
    private GoApiRequest goApiRequest;

    private ElasticAgentRequestProcessor processor;

    @BeforeEach
    public void setUp() throws Exception {

        final Map<String, VersionableElasticAgentProcessor> processorMap = new HashMap<>();
        processorMap.put("1.0", processorForV1);

        processor = new ElasticAgentRequestProcessor(pluginRequestProcessorRegistry, processorMap);
    }

    @Test
    public void shouldRegisterItselfForRequestProcessing() {
        verify(pluginRequestProcessorRegistry).registerProcessorFor(REQUEST_DISABLE_AGENTS, processor);
        verify(pluginRequestProcessorRegistry).registerProcessorFor(REQUEST_DELETE_AGENTS, processor);
        verify(pluginRequestProcessorRegistry).registerProcessorFor(REQUEST_SERVER_LIST_AGENTS, processor);
    }

    @Test
    public void shouldDelegateRequestToElasticAgentRequestProcessorV1WhenRequestApiVersionIsV1() {
        when(goApiRequest.apiVersion()).thenReturn("1.0");

        processor.process(pluginDescriptor, goApiRequest);

        verify(processorForV1).process(pluginDescriptor, goApiRequest);
    }
}
