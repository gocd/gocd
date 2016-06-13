/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service.support;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MemoryPoolInformationProvider extends AbstractMemoryInformationProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 10.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder builder) {
        builder.addSection("Memory pool information");

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            builder.append(String.format("Name: %s, Type: %s\n%s\n", memoryPoolMXBean.getName(), memoryPoolMXBean.getType(), format(memoryPoolMXBean.getUsage())));
        }
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            Map<String, Object> params = formatInJson(memoryPoolMXBean.getUsage());
            params.put("Type", memoryPoolMXBean.getType());
            json.put(memoryPoolMXBean.getName(), params);
        }
        return json;
    }

    @Override
    public String name() {
        return "Memory Pool Information";
    }
}
