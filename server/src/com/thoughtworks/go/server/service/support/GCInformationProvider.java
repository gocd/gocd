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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GCInformationProvider implements ServerInfoProvider {
    @Override
    public double priority() {
        return 8.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder builder) {
        List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        builder.addSection("GC information");
        for (GarbageCollectorMXBean gcBean : garbageCollectorMXBeans) {
            builder.append(String.format("%s %s %s : %s (Count : Time)\n", gcBean.getName(), Arrays.toString(gcBean.getMemoryPoolNames()), gcBean.getCollectionCount(), gcBean.getCollectionTime()));
        }
    }

    @Override
    public Map<String, Object> asJson() {
        List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        for (GarbageCollectorMXBean gcBean : garbageCollectorMXBeans) {
            LinkedHashMap<String, Object> gcBeanParms = new LinkedHashMap<>();
            gcBeanParms.put("Memory Pool Names", Arrays.toString(gcBean.getMemoryPoolNames()));
            gcBeanParms.put("Collection Count", gcBean.getCollectionCount());
            gcBeanParms.put("Collection Time", gcBean.getCollectionTime());
            json.put(gcBean.getName(), gcBeanParms);
        }
        return json;
    }

    @Override
    public String name() {
        return "GC Information";
    }
}
