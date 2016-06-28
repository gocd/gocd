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
import java.lang.management.RuntimeMXBean;
import java.util.*;

@Component
public class RuntimeInformationProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 7.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder builder) {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        builder.addSection("Runtime information");
        builder.append(String.format("Name - %s\n", runtimeMXBean.getName()));

        long uptime = runtimeMXBean.getUptime();
        long uptimeInSeconds = uptime / 1000;
        long numberOfHours = uptimeInSeconds / (60 * 60);
        long numberOfMinutes = (uptimeInSeconds / 60) - (numberOfHours * 60);
        long numberOfSeconds = uptimeInSeconds % 60;

        builder.append(String.format("Uptime - %s [About %s hours, %s minutes, %s seconds]\n", uptime, numberOfHours, numberOfMinutes, numberOfSeconds));
        builder.append(String.format("Spec Name - %s\n", runtimeMXBean.getSpecName()));
        builder.append(String.format("Spec Vendor - %s\n", runtimeMXBean.getSpecVendor()));
        builder.append(String.format("Spec Version - %s\n", runtimeMXBean.getSpecVersion()));
        builder.addSubSection("Input Arguments").append(asIndentedMultilineValues(runtimeMXBean.getInputArguments(), ""));
        builder.addSubSection("System Properties").append(asIndentedMultilineValues(runtimeMXBean.getSystemProperties()));
        builder.addSubSection("Classpath").append(prettyClassPath(runtimeMXBean.getClassPath()));
        builder.addSubSection("Boot Classpath").append(prettyClassPath(runtimeMXBean.getBootClassPath()));
    }

    private String asIndentedMultilineValues(List<String> values, String prefix) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            builder.append("  -> ").append(prefix).append(value).append("\n");
        }
        return builder.toString();
    }

    private String asIndentedMultilineValues(Map<String, String> properties) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            builder.append("  -> ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        return builder.toString();
    }

    private Map<String, Object> asIndentedMultilineValuesAsJson(Map<String, String> inputArguments) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : inputArguments.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }
        return json;
    }

    private String prettyClassPath(String classPath) {
        String[] classpathValues = classPath.split(System.getProperty("path.separator"));
        return asIndentedMultilineValues(Arrays.asList(classpathValues), "file:");
    }

    @Override
    public Map<String, Object> asJson() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeMXBean.getUptime();
        long uptimeInSeconds = uptime / 1000;
        long numberOfHours = uptimeInSeconds / (60 * 60);
        long numberOfMinutes = (uptimeInSeconds / 60) - (numberOfHours * 60);
        long numberOfSeconds = uptimeInSeconds % 60;

        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Name", runtimeMXBean.getName());
        json.put("Uptime", runtimeMXBean.getUptime());
        json.put("Uptime (in Time Format)", "[About " + numberOfHours + " hours, " + numberOfMinutes + " minutes, " + numberOfSeconds + " seconds]");
        json.put("Spec Name", runtimeMXBean.getSpecName());
        json.put("Spec Vendor", runtimeMXBean.getSpecVendor());
        json.put("Spec Version", runtimeMXBean.getSpecVersion());

        json.put("Input Arguments", runtimeMXBean.getInputArguments());
        json.put("System Properties", new TreeMap<>(asIndentedMultilineValuesAsJson(runtimeMXBean.getSystemProperties())));

        return json;
    }

    @Override
    public String name() {
        return "Runtime Information";
    }
}
