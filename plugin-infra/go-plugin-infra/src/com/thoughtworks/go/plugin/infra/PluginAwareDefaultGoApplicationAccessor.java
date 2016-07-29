/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginAwareDefaultGoApplicationAccessor extends GoApplicationAccessor {

    private static Logger LOGGER = LoggerFactory.getLogger(PluginAwareDefaultGoApplicationAccessor.class);

    private final GoPluginDescriptor pluginDescriptor;
    private final PluginRequestProcessorRegistry requestProcessorRegistry;

    public PluginAwareDefaultGoApplicationAccessor(GoPluginDescriptor pluginDescriptor, PluginRequestProcessorRegistry requestProcessorRegistry) {
        this.pluginDescriptor = pluginDescriptor;
        this.requestProcessorRegistry = requestProcessorRegistry;
    }

    @Override
    public GoApiResponse submit(final GoApiRequest request) {
        if (requestProcessorRegistry.canProcess(request)) {
            try {
                GoPluginApiRequestProcessor processor = requestProcessorRegistry.processorFor(request);
                return processor.process(pluginDescriptor, request);
            } catch (Exception e) {
                LOGGER.warn(String.format("Error while processing request api [%s]", request.api()), e);
                throw new RuntimeException(String.format("Error while processing request api %s", request.api()), e);
            }
        }
        LOGGER.warn(String.format("Plugin %s sent a request that could not be understood %s at version %s", request.pluginIdentifier().getExtension(), request.api(), request.apiVersion()));
        return unhandledApiRequest();
    }

    private DefaultGoApiResponse unhandledApiRequest() {
        return new DefaultGoApiResponse(404);
    }


    public GoPluginDescriptor pluginDescriptor() {
        return pluginDescriptor;
    }
}
