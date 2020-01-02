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
package com.thoughtworks.go.domain;

import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentPipelinesConfig;

/**
 * @understands how to reference an logical grouping of machines
 */
public class EnvironmentPipelineMatcher {
    private final CaseInsensitiveString name;
    private final List<String> agentUuids;
    private final EnvironmentPipelinesConfig pipelineConfigs;

    public EnvironmentPipelineMatcher(final CaseInsensitiveString name, List<String> agentUuids, EnvironmentPipelinesConfig pipelineNames) {
        this.name = name;
        this.agentUuids = agentUuids;
        pipelineConfigs = pipelineNames;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (that == null) { return false; }
        if (getClass() != that.getClass()) { return false; }

        return equals((EnvironmentPipelineMatcher) that);
    }

    private boolean equals(EnvironmentPipelineMatcher that) {
        if (!name.equals(that.name)) { return false; }
        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public boolean match(String pipelineName, String uuid) {
        return hasPipeline(pipelineName) && hasAgent(uuid);
    }

    boolean hasAgent(String uuid) {
        return agentUuids.contains(uuid);
    }

    public boolean hasPipeline(String pipelineName) {
        return pipelineConfigs.containsPipelineNamed(new CaseInsensitiveString(pipelineName));
    }

    public CaseInsensitiveString name() {
        return name;
    }
}
