/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.util.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ServerStatusService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStatusService.class);

    private final List<ServerInfoProvider> providers = new ArrayList<>();

    @Autowired
    public ServerStatusService(ServerInfoProvider... providerArray) {
        providers.addAll(Arrays.asList(providerArray));
        providers.sort(Comparator.comparingDouble(ServerInfoProvider::priority));
    }

    public Map<String, Object> asJsonCompatibleMap() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Timestamp", Dates.formatIso8601SystemCompactOffsetNoMillis(new Date()));

        for (ServerInfoProvider provider : providers) {
            try {
                json.put(provider.name(), provider.asJsonCompatibleMap());
            } catch (Exception e) {
                json.put(provider.getClass().getCanonicalName(), String.format("Provider %s threw an exception: %s", provider.getClass(), e.getMessage()));
                LOGGER.warn("An API support page provider failed.", e);
            }
        }
        return json;
    }
}
