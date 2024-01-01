/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.*;

@Component
public class ThreadInformationProvider implements ServerInfoProvider {
    private final DaemonThreadStatsCollector daemonThreadStatsCollector;

    @Autowired
    public ThreadInformationProvider(DaemonThreadStatsCollector daemonThreadStatsCollector) {
        this.daemonThreadStatsCollector = daemonThreadStatsCollector;
    }

    @Override
    public double priority() {
        return 13.0;
    }

    private Map<String, Object> getThreadCount(ThreadMXBean threadMXBean) {
        Map<String, Object> count = new LinkedHashMap<>();
        count.put("Current", threadMXBean.getThreadCount());
        count.put("Total", threadMXBean.getTotalStartedThreadCount());
        count.put("Daemon", threadMXBean.getDaemonThreadCount());
        count.put("Peak", threadMXBean.getPeakThreadCount());
        return count;
    }

    private Map<String, Object> getDeadLockThreadInformation(ThreadMXBean threadMXBean) {
        Map<String, Object> json = new LinkedHashMap<>();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            json.put("Count", deadlockedThreads.length);
            for (long deadlockedThread : deadlockedThreads) {
                Map<String, Object> threadsInfo = new LinkedHashMap<>();
                Map<String, Object> lockedMonitorsInfo = new LinkedHashMap<>();
                Map<String, Object> stackTrackInfo = new LinkedHashMap<>();
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(deadlockedThread);
                LockInfo lockInfo = threadInfo.getLockInfo();
                threadsInfo.put(threadInfo.getThreadName(), Objects.requireNonNullElse(lockInfo, "This thread is not waiting for any locks"));
                MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
                for (MonitorInfo lockedMonitor : lockedMonitors) {
                    lockedMonitorsInfo.put("Monitor for class " + lockedMonitor.getClassName(), "taken at stack frame " + lockedMonitor.getLockedStackFrame());
                }
                stackTrackInfo.put(Long.toString(deadlockedThread), Arrays.toString(threadInfo.getStackTrace()));
                json.put("Thread Information", threadsInfo);
                json.put("Monitor Information Stack Frame where locks were taken", lockedMonitorsInfo);
                json.put("Stack Trace Of DeadLock Threads", stackTrackInfo);
            }
        }
        return json;
    }

    private Map<Long, Map<String, Object>> getThreadInformation(ThreadMXBean threadMXBean) {
        Map<Long, Map<String, Object>> traces = new TreeMap<>();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        for (ThreadInfo threadInfo : threadInfos) {
            Map<String, Object> threadStackTrace = new LinkedHashMap<>();
            threadStackTrace.put("Id", threadInfo.getThreadId());
            threadStackTrace.put("Name", threadInfo.getThreadName());
            threadStackTrace.put("State", threadInfo.getThreadState());
            threadStackTrace.put("UserTime(nanoseconds)", threadMXBean.getThreadUserTime(threadInfo.getThreadId()));
            threadStackTrace.put("CPUTime(nanoseconds)", threadMXBean.getThreadCpuTime(threadInfo.getThreadId()));
            threadStackTrace.put("DaemonThreadInfo", daemonThreadStatsCollector.statsFor(threadInfo.getThreadId()));
            threadStackTrace.put("AllocatedMemory(Bytes)", getAllocatedMemory(threadInfo));

            Map<String, Object> lockMonitorInfo = new LinkedHashMap<>();
            lockMonitorInfo.put("Locked Monitors", asJSON(threadInfo.getLockedMonitors()));
            lockMonitorInfo.put("Locked Synchronizers", asJSON(threadInfo.getLockedSynchronizers()));
            threadStackTrace.put("Lock Monitor Info", lockMonitorInfo);

            Map<String, Object> blockedInfo = new LinkedHashMap<>();
            blockedInfo.put("Blocked Time(ms)", threadInfo.getBlockedTime());
            blockedInfo.put("Blocked Count", threadInfo.getBlockedCount());
            threadStackTrace.put("Blocked Info", blockedInfo);

            Map<String, Object> timeInfo = new LinkedHashMap<>();
            timeInfo.put("Waited Time(ms)", threadInfo.getWaitedTime());
            timeInfo.put("Waited Count", threadInfo.getWaitedCount());
            threadStackTrace.put("Time Info", timeInfo);

            Map<String, Object> lockInfoMap = new LinkedHashMap<>();
            LockInfo lockInfo = threadInfo.getLockInfo();
            lockInfoMap.put("Locked On", asJSON(lockInfo));
            lockInfoMap.put("Lock Owner Thread Id", threadInfo.getLockOwnerId());
            lockInfoMap.put("Lock Owner Thread Name", threadInfo.getLockOwnerName());
            threadStackTrace.put("Lock Info", lockInfoMap);

            Map<String, Object> stateInfo = new LinkedHashMap<>();
            stateInfo.put("Suspended", threadInfo.isSuspended());
            stateInfo.put("InNative", threadInfo.isInNative());
            threadStackTrace.put("State Info", stateInfo);

            threadStackTrace.put("Stack Trace", asJSON(threadInfo.getStackTrace()));
            traces.put(threadInfo.getThreadId(), threadStackTrace);
        }
        return traces;
    }

    private long getAllocatedMemory(ThreadInfo threadInfo) {
        return Optional.ofNullable(ManagementFactory.getPlatformMXBean(com.sun.management.ThreadMXBean.class))
            .map(threadMXBean -> threadMXBean.getThreadAllocatedBytes(threadInfo.getThreadId()))
            .orElse(-1L);
    }

    private Object asJSON(StackTraceElement[] stackTrace) {
        List<String> strings = new ArrayList<>();

        for (StackTraceElement o : stackTrace) {
            strings.add(o.toString());
        }
        return strings;

    }

    private List<Map<String, Object>> asJSON(MonitorInfo[] lockedMonitors) {
        List<Map<String, Object>> json = new ArrayList<>();

        for (MonitorInfo lockedMonitor : lockedMonitors) {
            Map<String, Object> lockedMonitorJson = new LinkedHashMap<>();
            lockedMonitorJson.put("Class", lockedMonitor.getClassName());
            lockedMonitorJson.put("IdentityHashCode", lockedMonitor.getIdentityHashCode());
            lockedMonitorJson.put("LockedStackDepth", lockedMonitor.getLockedStackDepth());
            lockedMonitorJson.put("StackFrame", lockedMonitor.getLockedStackFrame().toString());
            json.add(lockedMonitorJson);
        }
        return json;
    }

    private List<Map<String, Object>> asJSON(LockInfo[] lockInfos) {
        List<Map<String, Object>> json = new ArrayList<>();

        for (LockInfo lockInfo : lockInfos) {
            json.add(asJSON(lockInfo));
        }
        return json;
    }

    private Map<String, Object> asJSON(LockInfo lockInfo) {
        Map<String, Object> lockedOn = new LinkedHashMap<>();
        if (lockInfo != null) {
            lockedOn.put("Class", lockInfo.getClassName());
            lockedOn.put("IdentityHashCode", lockInfo.getIdentityHashCode());
        }
        return lockedOn;
    }

    @Override
    public Map<String, Object> asJson() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("Thread Count", getThreadCount(threadMXBean));
        json.put("DeadLock Threads", getDeadLockThreadInformation(threadMXBean));
        json.put("Stack Trace", getThreadInformation(threadMXBean));
        return json;
    }

    @Override
    public String name() {
        return "Thread Information";
    }
}
