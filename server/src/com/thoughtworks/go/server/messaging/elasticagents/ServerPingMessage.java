/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.messaging.elasticagents;

import com.thoughtworks.go.server.messaging.PluginAwareMessage;

public class ServerPingMessage implements PluginAwareMessage {
    private final String pluginId;

    public ServerPingMessage(String pluginId) {
        this.pluginId = pluginId;
    }

    public String pluginId() {
        return pluginId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServerPingMessage that = (ServerPingMessage) o;

        return pluginId != null ? pluginId.equals(that.pluginId) : that.pluginId == null;
    }

    @Override
    public int hashCode() {
        return pluginId != null ? pluginId.hashCode() : 0;
    }
}
