/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.service.support;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MemoryInformationProvider extends AbstractMemoryInformationProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 10.0;
    }

    @Override
    public Map<String, Object> asJsonCompatibleMap() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Heap", formatInJson(memoryMXBean.getHeapMemoryUsage()));
        json.put("Non Heap", formatInJson(memoryMXBean.getNonHeapMemoryUsage()));
        return json;
    }

    @Override
    public String name() {
        return "Memory Information";
    }
}
