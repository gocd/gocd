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

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DaemonThreadStatsCollector {
    private final ConcurrentMap<Long, Info> cpuInfoByThreadId = new ConcurrentHashMap<>();

    public void captureStats(long threadId) {
        long threadCpuTime = ManagementFactory.getThreadMXBean().getThreadCpuTime(threadId);
        if (threadCpuTime != -1 ) {
            cpuInfoByThreadId.put(threadId, new Info(threadCpuTime, UUID.randomUUID().toString()));
        }
    }

    public void clearStats(long threadId) {
        cpuInfoByThreadId.remove(threadId);
    }

    public Map<String, Object> statsFor(long threadId) {
        Info info = cpuInfoByThreadId.get(threadId);
        long end = ManagementFactory.getThreadMXBean().getThreadCpuTime(threadId);
        if (info == null || end == -1) {
            return null;
        }
        long start = info.time;
        Map<String, Object> map = new HashMap<>();
        map.put("CPUTime(nanoseconds)", end - start);
        map.put("UUID", info.uuid);
        return map;
    }

    private static class Info {
        public long time;
        public String uuid;

        public Info(long threadCpuTime, String uuid) {
            time = threadCpuTime;
            this.uuid = uuid;
        }
    }

}