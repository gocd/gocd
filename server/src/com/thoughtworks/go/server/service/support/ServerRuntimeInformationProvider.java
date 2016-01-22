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

package com.thoughtworks.go.server.service.support;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @understands providing all the information about the server using JMX beans.
 */
@Component
public class ServerRuntimeInformationProvider implements ServerInfoProvider {

    @Override
    public double priority() {
        return 6.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder infoCollector) {
        osInfo(ManagementFactory.getOperatingSystemMXBean(), infoCollector);
        runtimeInfo(ManagementFactory.getRuntimeMXBean(), infoCollector);
        gcInfo(ManagementFactory.getGarbageCollectorMXBeans(), infoCollector);
        memoryInfo(ManagementFactory.getMemoryMXBean(), infoCollector);
        poolInfo(infoCollector);
        threadInfo(ManagementFactory.getThreadMXBean(), infoCollector);
    }

    private void poolInfo(InformationStringBuilder builder) {
        builder.addSection("Memory pool information");

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            builder.append(String.format("Name: %s, Type: %s\n%s\n", memoryPoolMXBean.getName(), memoryPoolMXBean.getType(), format(memoryPoolMXBean.getUsage())));
        }
    }

    private String format(MemoryUsage memoryUsage) {
        long init = memoryUsage.getInit();
        long used = memoryUsage.getUsed();
        long committed = memoryUsage.getCommitted();
        long max = memoryUsage.getMax();

        return String.format("  init:      %s\n  used:      %s\n  committed: %s\n  max:       %s\n", init, used, committed, max);
    }

    private void threadInfo(ThreadMXBean threadMXBean, InformationStringBuilder builder) {
        builder.addSection("Thread information");

        builder.append(String.format("Current: %s, Total: %s, Daemon: %s, Peak: %s\n", threadMXBean.getThreadCount(), threadMXBean.getTotalStartedThreadCount(), threadMXBean.getDaemonThreadCount(),
                threadMXBean.getPeakThreadCount()));
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            builder.append(String.format("Found %s dead locked threads. Here is there information.\n", deadlockedThreads.length));
            for (long deadlockedThread : deadlockedThreads) {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(deadlockedThread);
                LockInfo lockInfo = threadInfo.getLockInfo();
                if (lockInfo != null) {
                    builder.append(String.format("LockInfo: %s", lockInfo));
                } else {
                    builder.append("This thread is not waiting for any locks\n");
                }
                builder.append(String.format("Monitor Info - Stack Frame where locks were taken.\n"));
                MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
                for (MonitorInfo lockedMonitor : lockedMonitors) {
                    builder.append(String.format("Monitor for class '%s' taken at stack frame '%s'.", lockedMonitor.getClassName(), lockedMonitor.getLockedStackFrame()));
                }
                builder.append("The stack trace of the deadlocked thread\n");
                builder.append(Arrays.toString(threadInfo.getStackTrace()));
            }
        }
        builder.addSubSection("All thread stacktraces");
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        for (ThreadInfo threadInfo : threadInfos) {
            builder.append(String.format("%s, %s, %s\n", threadInfo.getThreadId(), threadInfo.getThreadName(), threadInfo.getThreadState()));
            MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
            builder.append("Locked Monitors:\n");
            for (MonitorInfo lockedMonitor : lockedMonitors) {
                builder.append(String.format("%s at %s", lockedMonitor, lockedMonitor.getLockedStackFrame()));
            }
            LockInfo[] lockedSynchronizers = threadInfo.getLockedSynchronizers();
            builder.append("Locked Synchronizers:\n");
            for (LockInfo lockedSynchronizer : lockedSynchronizers) {
                builder.append(lockedSynchronizer);
            }
            builder.append("Stacktrace:\n  ");
            builder.append(StringUtils.join(threadInfo.getStackTrace(), "\n  "));
            builder.append("\n\n");
        }
    }

    private void memoryInfo(MemoryMXBean memoryMXBean, InformationStringBuilder builder) {
        builder.addSection("Memory information");

        builder.append(String.format("Heap:\n%s\nNon-Heap:\n%s\nPending Finalization: %s\n", format(memoryMXBean.getHeapMemoryUsage()), format(memoryMXBean.getNonHeapMemoryUsage()),
                memoryMXBean.getObjectPendingFinalizationCount()));
    }

    private void gcInfo(List<GarbageCollectorMXBean> garbageCollectorMXBeans, InformationStringBuilder builder) {
        builder.addSection("GC information");
        for (GarbageCollectorMXBean gcBean : garbageCollectorMXBeans) {
            builder.append(String.format("%s %s %s : %s (Count : Time)\n", gcBean.getName(), Arrays.toString(gcBean.getMemoryPoolNames()), gcBean.getCollectionCount(), gcBean.getCollectionTime()));
        }
    }

    private void osInfo(OperatingSystemMXBean operatingSystemMXBean, InformationStringBuilder builder) {
        builder.addSection("OS information");
        builder.append(String.format("%s, %s, %s, %s, %s\n", operatingSystemMXBean.getName(), operatingSystemMXBean.getArch(), operatingSystemMXBean.getVersion(),
                operatingSystemMXBean.getAvailableProcessors(), operatingSystemMXBean.getSystemLoadAverage()));
    }

    private void runtimeInfo(RuntimeMXBean runtimeMXBean, InformationStringBuilder builder) {
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

    private String prettyClassPath(String classPath) {
        String[] classpathValues = classPath.split(System.getProperty("path.separator"));
        return asIndentedMultilineValues(Arrays.asList(classpathValues), "file:");
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
}
