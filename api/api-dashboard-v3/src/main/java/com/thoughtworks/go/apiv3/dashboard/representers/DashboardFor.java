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
package com.thoughtworks.go.apiv3.dashboard.representers;

import com.thoughtworks.go.server.dashboard.DashboardGroup;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import com.thoughtworks.go.server.domain.Username;

import java.util.List;
import java.util.stream.Collectors;

public class DashboardFor {
    private final List<? extends DashboardGroup> pipelineGroups;
    private List<? extends DashboardGroup> environments;
    private final Username username;

    private String personalizationEtag;

    public DashboardFor(List<? extends DashboardGroup> pipelineGroups, List<? extends DashboardGroup> environments, Username username, String personalizationEtag) {
        this.pipelineGroups = pipelineGroups;
        this.environments = environments;
        this.username = username;
        this.personalizationEtag = personalizationEtag;
    }

    public List<? extends DashboardGroup> getPipelineGroups() {
        return pipelineGroups;
    }

    public List<? extends DashboardGroup> getEnvironments() {
        return environments;
    }

    public List<GoDashboardPipeline> getPipelines() {
        return pipelineGroups.stream().flatMap(group -> group.allPipelines().stream()).collect(Collectors.toList());
    }

    public Username getUsername() {
        return username;
    }

    public String getPersonalizationEtag() {
        return personalizationEtag;
    }
}
