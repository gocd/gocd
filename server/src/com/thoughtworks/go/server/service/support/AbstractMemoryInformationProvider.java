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

import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractMemoryInformationProvider {
    Map<String, Object> formatInJson(MemoryUsage heapMemoryUsage) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("init", heapMemoryUsage.getInit());
        json.put("used", heapMemoryUsage.getUsed());
        json.put("committed", heapMemoryUsage.getCommitted());
        json.put("max", heapMemoryUsage.getMax());
        return json;
    }

    String format(MemoryUsage memoryUsage) {
        long init = memoryUsage.getInit();
        long used = memoryUsage.getUsed();
        long committed = memoryUsage.getCommitted();
        long max = memoryUsage.getMax();

        return String.format("  init:      %s\n  used:      %s\n  committed: %s\n  max:       %s\n", init, used, committed, max);
    }
}
