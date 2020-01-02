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

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DaemonThreadStatsCollector {
    private final ConcurrentHashMap<Long, Info> cpuInfoConcurrentHashMap = new ConcurrentHashMap<>();

    public void captureStats(long threadId) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long threadCpuTime = threadMXBean.getThreadCpuTime(threadId);
        cpuInfoConcurrentHashMap.put(threadId, new Info(threadCpuTime, UUID.randomUUID().toString()));
    }

    public void clearStats(long threadId) {
        if (cpuInfoConcurrentHashMap.containsKey(threadId)) {
            cpuInfoConcurrentHashMap.remove(threadId);
        }
    }

    public Map<String, Object> statsFor(long threadId) {
        if (!cpuInfoConcurrentHashMap.containsKey(threadId)) {
            return null;
        }
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long end = threadMXBean.getThreadCpuTime(threadId);
        Info info = cpuInfoConcurrentHashMap.get(threadId);
        Long start = info.time;
        HashMap<String, Object> map = new HashMap<>();
        map.put("CPUTime(nanoseconds)", end - start);
        map.put("UUID", info.uuid);
        return map;
    }

    private class Info {
        public long time;
        public String uuid;

        public Info(long threadCpuTime, String uuid) {
            time = threadCpuTime;
            this.uuid = uuid;
        }
    }

}