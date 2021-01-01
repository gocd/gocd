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
package com.thoughtworks.go.server.service.support;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OSInformationProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 4.0;
    }

    @Override
    public Map<String, Object> asJson() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("OS Name", operatingSystemMXBean.getName());
        json.put("OS Version", operatingSystemMXBean.getVersion());
        json.put("System Architecture", operatingSystemMXBean.getArch());
        json.put("Available Processors", operatingSystemMXBean.getAvailableProcessors());
        json.put("Average System Load", operatingSystemMXBean.getSystemLoadAverage());
        return json;
    }

    @Override
    public String name() {
        return "OS Information";
    }
}
