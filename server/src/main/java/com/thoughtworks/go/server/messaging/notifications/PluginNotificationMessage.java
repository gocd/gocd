/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.messaging.notifications;

import com.thoughtworks.go.server.messaging.PluginAwareMessage;

import java.io.Serializable;
import java.util.Objects;

public class PluginNotificationMessage<T extends Serializable> implements PluginAwareMessage {
    private String pluginId;
    private final String requestName;
    private final T data;

    public PluginNotificationMessage(String pluginId, String requestName, T data) {
        this.pluginId = pluginId;
        this.requestName = requestName;
        this.data = data;
    }

    public String getRequestName() {
        return requestName;
    }

    public T getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginNotificationMessage<?> that = (PluginNotificationMessage<?>) o;
        return Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(requestName, that.requestName) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {

        return Objects.hash(pluginId, requestName, data);
    }

    @Override
    public String pluginId() {
        return pluginId;
    }

    @Override
    public String toString() {
        return "PluginNotificationMessage{" +
                "pluginId='" + pluginId + '\'' +
                ", requestName='" + requestName + '\'' +
                ", data=" + data +
                '}';
    }
}
