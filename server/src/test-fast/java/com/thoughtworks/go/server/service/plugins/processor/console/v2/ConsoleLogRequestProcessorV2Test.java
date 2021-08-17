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
package com.thoughtworks.go.server.service.plugins.processor.console.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.plugins.processor.console.ConsoleLogRequestProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.service.plugins.processor.console.ConsoleLogRequestProcessor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ConsoleLogRequestProcessorV2Test {
    @Mock
    private PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    @Mock
    private ConsoleService consoleService;
    @Mock
    private GoPluginDescriptor pluginDescriptor;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldRouteMessageToConsoleService() throws IOException, IllegalArtifactLocationException {
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("pipeline_name", "p1");
        requestMap.put("pipeline_counter", "1");
        requestMap.put("stage_name", "s1");
        requestMap.put("stage_counter", "2");
        requestMap.put("job_name", "j1");
        requestMap.put("text", "message1");

        DefaultGoApiRequest goApiRequest = new DefaultGoApiRequest(APPEND_TO_CONSOLE_LOG, VERSION_2, null);
        goApiRequest.setRequestBody(new GsonBuilder().create().toJson(requestMap));

        final ConsoleLogRequestProcessor processor = new ConsoleLogRequestProcessor(pluginRequestProcessorRegistry, consoleService);
        final GoApiResponse response = processor.process(pluginDescriptor, goApiRequest);

        assertThat(response.responseCode(), is(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE));

        final JobIdentifier jobIdentifier = new JobIdentifier("p1", 1, null, "s1", "2", "j1");
        verify(consoleService).appendToConsoleLog(jobIdentifier, "message1");
    }

    @Test
    void shouldRespondWithAMessageIfSomethingGoesWrong() {
        DefaultGoApiRequest goApiRequest = new DefaultGoApiRequest(APPEND_TO_CONSOLE_LOG, VERSION_2, null);
        goApiRequest.setRequestBody("this_is_invalid_JSON");

        final ConsoleLogRequestProcessor processor = new ConsoleLogRequestProcessor(pluginRequestProcessorRegistry, consoleService);
        final GoApiResponse response = processor.process(pluginDescriptor, goApiRequest);

        assertThat(response.responseCode(), is(DefaultGoApiResponse.INTERNAL_ERROR));

        final Map responseContents = new Gson().fromJson(response.responseBody(), Map.class);
        assertThat((String) responseContents.get("message"), containsString("Error:"));
    }
}
