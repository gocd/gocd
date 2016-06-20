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

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.*;

@Component
public class ThreadInformationProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 11.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder builder) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
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

    private Map<String, Object> getThreadCount(ThreadMXBean threadMXBean) {
        LinkedHashMap<String, Object> count = new LinkedHashMap<>();
        count.put("Current", threadMXBean.getThreadCount());
        count.put("Total", threadMXBean.getTotalStartedThreadCount());
        count.put("Daemon", threadMXBean.getDaemonThreadCount());
        count.put("Peak", threadMXBean.getPeakThreadCount());
        return count;
    }

    private Map<String, Object> getDeadLockThreadInformation(ThreadMXBean threadMXBean) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            json.put("Count", deadlockedThreads.length);
            for (long deadlockedThread : deadlockedThreads) {
                LinkedHashMap<String, Object> threadsInfo = new LinkedHashMap<>();
                LinkedHashMap<String, Object> lockedMonitorsInfo = new LinkedHashMap<>();
                LinkedHashMap<String, Object> stackTrackInfo = new LinkedHashMap<>();
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(deadlockedThread);
                LockInfo lockInfo = threadInfo.getLockInfo();
                if (lockInfo != null) {
                    threadsInfo.put(threadInfo.getThreadName(), lockInfo);
                } else {
                    threadsInfo.put(threadInfo.getThreadName(), "This thread is not waiting for any locks");
                }
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

    private TreeMap<Long, Map<String, Object>> getStackTraceInformation(ThreadMXBean threadMXBean) {
        TreeMap<Long, Map<String, Object>> traces = new TreeMap<>();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        for (ThreadInfo threadInfo : threadInfos) {
            LinkedHashMap<String, Object> threadStackTrace = new LinkedHashMap<>();
            threadStackTrace.put("Id", threadInfo.getThreadId());
            threadStackTrace.put("Name", threadInfo.getThreadName());
            threadStackTrace.put("State", threadInfo.getThreadState());

            LinkedHashMap<String, Object> lockMonitorInfo = new LinkedHashMap<>();
            MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
            ArrayList<Map<String, Object>> lockedMonitorsJson = new ArrayList<>();

            for (MonitorInfo lockedMonitor : lockedMonitors) {
                LinkedHashMap<String, Object> lockedMonitorJson = new LinkedHashMap<>();
                lockedMonitorJson.put("Class", lockedMonitor.getClassName());
                lockedMonitorJson.put("IdentityHashCode", lockedMonitor.getIdentityHashCode());
                lockedMonitorJson.put("LockedStackDepth", lockedMonitor.getLockedStackDepth());
                lockedMonitorJson.put("StackFrame", lockedMonitor.getLockedStackFrame().toString());
                lockedMonitorsJson.add(lockedMonitorJson);
            }

            lockMonitorInfo.put("Locked Monitors", lockedMonitorsJson);
            lockMonitorInfo.put("Locked Synchronizers", asJSON(threadInfo.getLockedSynchronizers()));
            threadStackTrace.put("Lock Monitor Info", lockMonitorInfo);

            LinkedHashMap<String, Object> blockedInfo = new LinkedHashMap<>();
            blockedInfo.put("Blocked Time", threadInfo.getBlockedTime() == -1 ? null : threadInfo.getBlockedTime());
            blockedInfo.put("Blocked Count", threadInfo.getBlockedCount());
            threadStackTrace.put("Blocked Info", blockedInfo);

            LinkedHashMap<String, Object> timeInfo = new LinkedHashMap<>();
            timeInfo.put("Waited Time", threadInfo.getWaitedTime() == -1 ? null : threadInfo.getWaitedTime());
            timeInfo.put("Waited Count", threadInfo.getWaitedCount());
            threadStackTrace.put("Time Info", timeInfo);

            LinkedHashMap<String, Object> lockInfoMap = new LinkedHashMap<>();
            LockInfo lockInfo = threadInfo.getLockInfo();
            lockInfoMap.put("Locked On", asJSON(lockInfo));
            lockInfoMap.put("Lock Owner Thread Id", threadInfo.getLockOwnerId() == -1 ? null : threadInfo.getLockOwnerId());
            lockInfoMap.put("Lock Owner Thread Name", threadInfo.getLockOwnerName());
            threadStackTrace.put("Lock Info", lockInfoMap);

            LinkedHashMap<String, Object> stateInfo = new LinkedHashMap<>();
            stateInfo.put("Suspended", threadInfo.isSuspended());
            stateInfo.put("InNative", threadInfo.isInNative());
            threadStackTrace.put("State Info", stateInfo);

            threadStackTrace.put("Stack Trace", asJSON(threadInfo.getStackTrace()));
            traces.put(threadInfo.getThreadId(), threadStackTrace);
        }
        return traces;
    }

    private Object asJSON(StackTraceElement[] stackTrace) {
        ArrayList<String> strings = new ArrayList<>();

        for (StackTraceElement o : stackTrace) {
            strings.add(o.toString());
        }
        return strings;

    }

    private ArrayList<LinkedHashMap<String, Object>> asJSON(LockInfo[] lockInfos) {
        ArrayList<LinkedHashMap<String, Object>> objects = new ArrayList<>();

        for (LockInfo lockInfo : lockInfos) {
            objects.add(asJSON(lockInfo));
        }
        return objects;
    }

    private LinkedHashMap<String, Object> asJSON(LockInfo lockInfo) {
        LinkedHashMap<String, Object> lockedOn = new LinkedHashMap<>();
        if (lockInfo != null) {
            lockedOn.put("Class", lockInfo.getClassName());
            lockedOn.put("IdentityHashCode", lockInfo.getIdentityHashCode());
        }
        return lockedOn;
    }

    @Override
    public Map<String, Object> asJson() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Thread Count", getThreadCount(threadMXBean));
        json.put("DeadLock Threads", getDeadLockThreadInformation(threadMXBean));
        json.put("Stack Trace", getStackTraceInformation(threadMXBean));
        return json;
    }

    @Override
    public String name() {
        return "Thread Information";
    }
}
