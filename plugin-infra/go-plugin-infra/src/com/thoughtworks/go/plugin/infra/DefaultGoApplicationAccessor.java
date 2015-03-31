/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra;


import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultGoApplicationAccessor extends GoApplicationAccessor {

    private static final Logger LOGGER = Logger.getLogger(DefaultGoApplicationAccessor.class);

    private Map<String, GoPluginApiRequestProcessor> processorMap = new HashMap<String, GoPluginApiRequestProcessor>();

    public void registerProcessorFor(String request, GoPluginApiRequestProcessor processor) {
        processorMap.put(request, processor);
    }

    @Override
    public GoApiResponse submit(final GoApiRequest request) {
        if (processorMap.containsKey(request.api())) {
            try {
                return processorMap.get(request.api()).process(request);
            } catch (Exception e) {
                LOGGER.warn(String.format("Error while processing request api %s", request.api()), e);
                throw new RuntimeException(String.format("Error while processing request api %s", request.api()), e);
            }
        }
        return unhandledApiRequest();
    }

    private DefaultGoApiResponse unhandledApiRequest() {
        return new DefaultGoApiResponse(401);
    }
}

