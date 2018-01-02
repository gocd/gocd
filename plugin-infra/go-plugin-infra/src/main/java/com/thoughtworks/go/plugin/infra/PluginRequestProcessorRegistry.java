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


import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PluginRequestProcessorRegistry {

    private Map<String, GoPluginApiRequestProcessor> processorMap = new ConcurrentHashMap<>();

    public void registerProcessorFor(String request, GoPluginApiRequestProcessor processor) {
        processorMap.put(request, processor);
    }

    public boolean canProcess(GoApiRequest request) {
        return processorMap.containsKey(request.api());
    }

    public GoPluginApiRequestProcessor processorFor(GoApiRequest request) {
        return processorMap.get(request.api());
    }
}

