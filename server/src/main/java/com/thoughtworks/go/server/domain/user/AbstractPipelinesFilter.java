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
package com.thoughtworks.go.server.domain.user;

import com.thoughtworks.go.config.CaseInsensitiveString;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

@EqualsAndHashCode
abstract class AbstractPipelinesFilter implements DashboardFilter {
    private final String name;
    private final Set<String> state;
    protected final List<CaseInsensitiveString> pipelines;

    AbstractPipelinesFilter(String name, List<CaseInsensitiveString> pipelines, Set<String> state) {
        this.name = name;
        this.state = state;
        this.pipelines = DashboardFilter.enforceList(pipelines);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> state() {
        return state;
    }

    @Override
    public abstract boolean isPipelineVisible(CaseInsensitiveString pipeline);

    @Override
    public abstract boolean allowPipeline(CaseInsensitiveString pipeline);

    boolean filterByPipelineList(CaseInsensitiveString pipelineName) {
        return null != pipelines && !pipelines.isEmpty() && pipelines.contains(pipelineName);
    }
}
