/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.session;

import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class JsonMessageHandler1_0 implements JsonMessageHandler {
    @Override
    public SessionData requestMessageSessionPut(String message) {
        Map map = JsonHelper.fromJson(message, Map.class);
        String pluginId = getPluginId(map);
        Map sessionData;
        try {
            sessionData = (Map) map.get("session-data");
        } catch (Exception e) {
            throw new RuntimeException("'sessionData' should of map type");
        }
        return new SessionData(pluginId, sessionData);
    }

    @Override
    public String requestMessageSessionGetAndRemove(String message) {
        Map map = JsonHelper.fromJson(message, Map.class);
        String pluginId = getPluginId(map);
        return pluginId;
    }

    private String getPluginId(Map map) {
        String pluginId;
        try {
            pluginId = (String) map.get("plugin-id");
        } catch (Exception e) {
            throw new RuntimeException("'plugin-id' should of string type");
        }
        if (StringUtils.isBlank(pluginId)) {
            throw new RuntimeException("'plugin-id' cannot be empty");
        }
        return pluginId;
    }
}
