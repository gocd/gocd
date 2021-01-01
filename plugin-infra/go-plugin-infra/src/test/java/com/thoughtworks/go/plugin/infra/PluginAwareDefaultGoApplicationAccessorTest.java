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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginAwareDefaultGoApplicationAccessorTest {

    @Test
    void shouldHandleExceptionThrownByProcessor() {
        String api = "api-uri";
        GoPluginApiRequestProcessor processor = mock(GoPluginApiRequestProcessor.class);
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        when(goApiRequest.api()).thenReturn(api);
        Throwable cause = new RuntimeException("error");
        when(processor.process(descriptor, goApiRequest)).thenThrow(cause);

        PluginRequestProcessorRegistry pluginRequestProcessorRegistry = new PluginRequestProcessorRegistry();
        pluginRequestProcessorRegistry.registerProcessorFor(api, processor);
        PluginAwareDefaultGoApplicationAccessor accessor = new PluginAwareDefaultGoApplicationAccessor(descriptor, pluginRequestProcessorRegistry);

        assertThatCode(() -> accessor.submit(goApiRequest))
                .hasMessage(String.format("Error while processing request api %s", api))
                .hasCause(cause);
    }
}
