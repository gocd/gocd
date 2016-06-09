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
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OSInformationProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 6.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder builder) {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        builder.addSection("OS information");
        builder.append(String.format("%s, %s, %s, %s, %s\n", operatingSystemMXBean.getName(), operatingSystemMXBean.getArch(), operatingSystemMXBean.getVersion(),
                operatingSystemMXBean.getAvailableProcessors(), operatingSystemMXBean.getSystemLoadAverage()));
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
