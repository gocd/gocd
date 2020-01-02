/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.PasswordArgument;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor.Request.CONSOLE_LOG;
import static com.thoughtworks.go.util.command.TaggedStreamConsumer.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public abstract class ArtifactRequestProcessorTestBase {
    private GoApiRequest request;
    private GoPluginDescriptor descriptor;
    private DefaultGoPublisher goPublisher;
    private ArtifactRequestProcessor artifactRequestProcessorForPublish;
    private ArtifactRequestProcessor artifactRequestProcessorForFetch;

    @Before
    public void setUp() throws Exception {
        request = mock(GoApiRequest.class);
        descriptor = mock(GoPluginDescriptor.class);
        goPublisher = mock(DefaultGoPublisher.class);
        EnvironmentVariableContext environmentVariableContext = mock(EnvironmentVariableContext.class);
        when(environmentVariableContext.secrets()).thenReturn(Collections.singletonList(new PasswordArgument("secret.value")));

        artifactRequestProcessorForPublish = ArtifactRequestProcessor.forPublishArtifact(goPublisher, environmentVariableContext);
        artifactRequestProcessorForFetch = ArtifactRequestProcessor.forFetchArtifact(goPublisher, environmentVariableContext);

        when(request.apiVersion()).thenReturn(getRequestPluginVersion());
        when(descriptor.id()).thenReturn("cd.go.artifact.docker");
        when(request.api()).thenReturn(CONSOLE_LOG.requestName());
    }

    protected abstract String getRequestPluginVersion();

    @Test
    public void shouldFailForAVersionOutsideOfSupportedVersions() {
        reset(request);
        when(request.apiVersion()).thenReturn("3.0");
        when(request.api()).thenReturn(CONSOLE_LOG.requestName());

        when(request.requestBody()).thenReturn("{\"logLevel\":\"ERROR\",\"message\":\"Error while pushing docker image to registry: foo.\"}");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            artifactRequestProcessorForPublish.process(descriptor, request);
        });

        assertThat(exception.getMessage(), containsString("Unsupported 'go.processor.artifact.console-log' API version: 3.0"));
    }

    @Test
    public void shouldSendErrorLogToConsoleLogForPublish() {
        when(request.requestBody()).thenReturn("{\"logLevel\":\"ERROR\",\"message\":\"Error while pushing docker image to registry: foo.\"}");

        artifactRequestProcessorForPublish.process(descriptor, request);

        verify(goPublisher, times(1)).taggedConsumeLine(PUBLISH_ERR, "[cd.go.artifact.docker] Error while pushing docker image to registry: foo.");
    }

    @Test
    public void shouldSendInfoLogToConsoleLogForPublish() {
        when(request.requestBody()).thenReturn("{\"logLevel\":\"INFO\",\"message\":\"Pushing docker image to registry: foo.\"}");

        artifactRequestProcessorForPublish.process(descriptor, request);

        verify(goPublisher, times(1)).taggedConsumeLine(PUBLISH, "[cd.go.artifact.docker] Pushing docker image to registry: foo.");
    }

    @Test
    public void shouldSendErrorLogToConsoleLogForFetch() {
        when(request.requestBody()).thenReturn("{\"logLevel\":\"ERROR\",\"message\":\"Error while pushing docker image to registry: foo.\"}");

        artifactRequestProcessorForFetch.process(descriptor, request);

        verify(goPublisher, times(1)).taggedConsumeLine(ERR, "[cd.go.artifact.docker] Error while pushing docker image to registry: foo.");
    }

    @Test
    public void shouldSendInfoLogToConsoleLogForFetch() {
        when(request.requestBody()).thenReturn("{\"logLevel\":\"INFO\",\"message\":\"Pushing docker image to registry: foo.\"}");

        artifactRequestProcessorForFetch.process(descriptor, request);

        verify(goPublisher, times(1)).taggedConsumeLine(OUT, "[cd.go.artifact.docker] Pushing docker image to registry: foo.");
    }

    @Test
    public void shouldMaskSecretsFromEnvironmentVariables() {
        when(request.requestBody()).thenReturn("{\"logLevel\":\"INFO\",\"message\":\"This is a secret: secret.value.\"}");

        artifactRequestProcessorForFetch.process(descriptor, request);

        verify(goPublisher, times(1)).taggedConsumeLine(OUT, "[cd.go.artifact.docker] This is a secret: ******.");
    }
}
