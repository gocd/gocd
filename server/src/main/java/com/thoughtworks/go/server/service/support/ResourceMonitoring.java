/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

@Component
public class ResourceMonitoring {
    private SystemEnvironment systemEnvironment;
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMonitoring.class.getName());

    @Autowired
    public ResourceMonitoring(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public void enableIfDiagnosticsModeIsEnabled() {
        if (systemEnvironment.get(SystemEnvironment.GO_DIAGNOSTICS_MODE)) {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            if (threadMXBean.isThreadContentionMonitoringSupported()) {
                threadMXBean.setThreadContentionMonitoringEnabled(true);
            }
            if (threadMXBean.isThreadCpuTimeSupported()) {
                threadMXBean.setThreadCpuTimeEnabled(true);
            }

            if (isThreadAllocatedMemorySupported(threadMXBean)) {
                setThreadAllocatedMemoryEnabled(threadMXBean);
            }
        }
    }

    private boolean isThreadAllocatedMemorySupported(ThreadMXBean threadMXBean) {
        Method method = ReflectionUtils.findMethod(threadMXBean.getClass(), "isThreadAllocatedMemorySupported");
        if (method != null) {
            try {
                method.setAccessible(true);
                return (boolean) method.invoke(threadMXBean);
            } catch (Exception e) {
                LOGGER.error("Error on threadMXBean.isThreadAllocatedMemorySupported : {}", e.getMessage());
            }
        }
        return false;
    }

    private void setThreadAllocatedMemoryEnabled(ThreadMXBean threadMXBean) {
        Method method = ReflectionUtils.findMethod(threadMXBean.getClass(), "setThreadAllocatedMemoryEnabled", boolean.class);
        if (method != null) {
            try {
                method.setAccessible(true);
                method.invoke(threadMXBean, true);
            } catch (Exception e) {
                LOGGER.error("Error on threadMXBean.setThreadAllocatedMemoryEnabled : {}", e.getMessage());
            }
        }
    }
}