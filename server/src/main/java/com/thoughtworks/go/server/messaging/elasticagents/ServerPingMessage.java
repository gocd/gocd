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
package com.thoughtworks.go.server.messaging.elasticagents;

import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.server.messaging.PluginAwareMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServerPingMessage implements PluginAwareMessage {
    private final String pluginId;
    private List<ClusterProfile> clusterProfiles;

    public ServerPingMessage(String pluginId, List<ClusterProfile> clusterProfiles) {
        this.pluginId = pluginId;
        this.clusterProfiles = clusterProfiles;
    }

    @Override
    public String pluginId() {
        return pluginId;
    }

    public List<Map<String, String>> getClusterProfilesAsConfigList() {
        return clusterProfiles.stream().map(profile -> profile.getConfigurationAsMap(true, true)).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerPingMessage that = (ServerPingMessage) o;
        return Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(clusterProfiles, that.clusterProfiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, clusterProfiles);
    }

    @Override
    public String toString() {
        return "ServerPingMessage{" +
                "pluginId='" + pluginId + '\'' +
                ", clusterProfiles=" + clusterProfiles +
                '}';
    }
}
