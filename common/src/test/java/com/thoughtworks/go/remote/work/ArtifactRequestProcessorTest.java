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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor.Request.CONSOLE_LOG;
import static com.thoughtworks.go.util.command.TaggedStreamConsumer.PUBLISH;
import static com.thoughtworks.go.util.command.TaggedStreamConsumer.PUBLISH_ERR;
import static org.mockito.Mockito.*;

public class ArtifactRequestProcessorTest {
    private GoApiRequest request;
    private GoPluginDescriptor descriptor;
    private DefaultGoPublisher goPublisher;
    private ArtifactRequestProcessor artifactRequestProcessor;

    @Before
    public void setUp() throws Exception {
        request = mock(GoApiRequest.class);
        descriptor = mock(GoPluginDescriptor.class);
        goPublisher = mock(DefaultGoPublisher.class);

        artifactRequestProcessor = new ArtifactRequestProcessor(goPublisher);

        when(request.apiVersion()).thenReturn("1.0");
        when(descriptor.id()).thenReturn("cd.go.artifact.docker");
        when(request.api()).thenReturn(CONSOLE_LOG.requestName());
    }

    @Test
    public void shouldPublishErrorLogToConsoleLog() {
        when(request.requestBody()).thenReturn("{\"logLevel\":\"ERROR\",\"message\":\"Error while pushing docker image to registry: foo.\"}");

        artifactRequestProcessor.process(descriptor, request);

        verify(goPublisher, times(1)).taggedConsumeLine(PUBLISH_ERR, "[cd.go.artifact.docker] Error while pushing docker image to registry: foo.");
    }

    @Test
    public void shouldPublishInfoLogToConsoleLog() {
        when(request.requestBody()).thenReturn("{\"logLevel\":\"INFO\",\"message\":\"Pushing docker image to registry: foo.\"}");

        artifactRequestProcessor.process(descriptor, request);

        verify(goPublisher, times(1)).taggedConsumeLine(PUBLISH, "[cd.go.artifact.docker] Pushing docker image to registry: foo.");
    }
}