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

package com.thoughtworks.go.agent.plugin.consolelog;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.command.TaggedStreamConsumer;
import com.thoughtworks.go.work.GoPublisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConsoleLogRequestProcessorTest {
    @Mock
    private GoPublisher goPublisher;
    @Mock
    private GoPluginDescriptor descriptor;
    @Mock
    private GoApiRequest apiRequest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldErrorOutIfCanNotHandleArtifactConsoleLogRequest() {
        when(apiRequest.apiVersion()).thenReturn("1.0");
        when(apiRequest.api()).thenReturn("unknown-request");

        final GoApiResponse response = new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        assertThat(response.responseCode(), is(500));
        assertThat(response.responseBody(), is("Invalid console log request name."));
    }

    @Test
    public void shouldErrorOutWhenInvalidAPIVersionProvidedInRequest() {
        when(apiRequest.apiVersion()).thenReturn("2.0");
        when(descriptor.id()).thenReturn("cd.go.scm.github.pr");
        when(apiRequest.api()).thenReturn(ConsoleLogRequest.SCM_PLUGIN_CONSOLE_LOG.requestName());

        final GoApiResponse response = new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        assertThat(response.responseCode(), is(500));
        assertThat(response.responseBody(), is("Unsupported 'go.processor.scm.console-log' API version: 2.0. Supported versions: [1.0]"));
    }

    @Test
    public void shouldHandleArtifactConsoleLogRequestForInfoMessage() {
        when(descriptor.id()).thenReturn("cd.go.artifact.docker");
        when(apiRequest.apiVersion()).thenReturn("1.0");
        when(apiRequest.api()).thenReturn(ConsoleLogRequest.ARTIFACT_PLUGIN_CONSOLE_LOG.requestName());
        when(apiRequest.requestBody()).thenReturn("{\"logLevel\":\"INFO\",\"message\":\"This is info message.\"}");

        new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        verify(goPublisher).taggedConsumeLine(TaggedStreamConsumer.PUBLISH, "[cd.go.artifact.docker] This is info message.");
    }

    @Test
    public void shouldHandleArtifactConsoleLogRequestForErrorMessage() {
        when(descriptor.id()).thenReturn("cd.go.artifact.docker");
        when(apiRequest.apiVersion()).thenReturn("1.0");
        when(apiRequest.api()).thenReturn(ConsoleLogRequest.ARTIFACT_PLUGIN_CONSOLE_LOG.requestName());
        when(apiRequest.requestBody()).thenReturn("{\"logLevel\":\"ERROR\",\"message\":\"This is error message.\"}");

        new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        verify(goPublisher).taggedConsumeLine(TaggedStreamConsumer.PUBLISH_ERR, "[cd.go.artifact.docker] This is error message.");
    }

    @Test
    public void shouldHandleSCMConsoleLogRequestForInfoMessage() {
        when(descriptor.id()).thenReturn("cd.go.scm.github.pr");
        when(apiRequest.apiVersion()).thenReturn("1.0");
        when(apiRequest.api()).thenReturn(ConsoleLogRequest.SCM_PLUGIN_CONSOLE_LOG.requestName());
        when(apiRequest.requestBody()).thenReturn("{\"logLevel\":\"INFO\",\"message\":\"This is info message.\"}");

        new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        verify(goPublisher).taggedConsumeLine(TaggedStreamConsumer.PREP, "[cd.go.scm.github.pr] This is info message.");
    }

    @Test
    public void shouldHandleSCMConsoleLogRequestForErrorMessage() {
        when(descriptor.id()).thenReturn("cd.go.scm.github.pr");
        when(apiRequest.apiVersion()).thenReturn("1.0");
        when(apiRequest.api()).thenReturn(ConsoleLogRequest.SCM_PLUGIN_CONSOLE_LOG.requestName());
        when(apiRequest.requestBody()).thenReturn("{\"logLevel\":\"ERROR\",\"message\":\"This is error message.\"}");

        new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        verify(goPublisher).taggedConsumeLine(TaggedStreamConsumer.PREP_ERR, "[cd.go.scm.github.pr] This is error message.");
    }

    @Test
    public void shouldHandleTaskConsoleLogRequestForInfoMessage() {
        when(descriptor.id()).thenReturn("cd.go.task.gradle");
        when(apiRequest.apiVersion()).thenReturn("1.0");
        when(apiRequest.api()).thenReturn(ConsoleLogRequest.TASK_PLUGIN_CONSOLE_LOG.requestName());
        when(apiRequest.requestBody()).thenReturn("{\"logLevel\":\"INFO\",\"message\":\"This is info message.\"}");

        new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        verify(goPublisher).taggedConsumeLine(TaggedStreamConsumer.OUT, "[cd.go.task.gradle] This is info message.");
    }

    @Test
    public void shouldHandleTaskConsoleLogRequestForErrorMessage() {
        when(descriptor.id()).thenReturn("cd.go.task.gradle");
        when(apiRequest.apiVersion()).thenReturn("1.0");
        when(apiRequest.api()).thenReturn(ConsoleLogRequest.TASK_PLUGIN_CONSOLE_LOG.requestName());
        when(apiRequest.requestBody()).thenReturn("{\"logLevel\":\"ERROR\",\"message\":\"This is error message.\"}");

        new ConsoleLogRequestProcessor(goPublisher).process(descriptor, apiRequest);

        verify(goPublisher).taggedConsumeLine(TaggedStreamConsumer.ERR, "[cd.go.task.gradle] This is error message.");
    }
}