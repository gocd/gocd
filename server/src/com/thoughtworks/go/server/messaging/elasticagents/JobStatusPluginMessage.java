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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.PluginAwareMessage;

import java.util.List;

/**
 * conundrum: if we need to send jobstatus messages to different plugins do we need
 * to use a PluginAwareMessage? If so, somewhere, we need to listen on JobStatusMessage
 * and forward them to the plugins that have subscribed
 *
 * Where?
 */
public class JobStatusPluginMessage extends JobStatusMessage implements PluginAwareMessage {
    private final String pluginId;
    private final String environment;
    private final List<String> resources;

    public JobStatusPluginMessage(JobIdentifier jobIdentifier, JobState state, String agentUuid, String pluginId, String environment, List<String> resources) {
        super(jobIdentifier, state, agentUuid);
        this.pluginId = pluginId;
        this.environment = environment;
        this.resources = resources;
    }

    public String pluginId() {
        return pluginId;
    }

    public List<String> getResources() {
        return resources;
    }

    public String getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobStatusPluginMessage that = (JobStatusPluginMessage) o;

        return pluginId != null ? pluginId.equals(that.pluginId) : that.pluginId == null;
    }

    @Override
    public int hashCode() {
        return pluginId != null ? pluginId.hashCode() : 0;
    }
}
