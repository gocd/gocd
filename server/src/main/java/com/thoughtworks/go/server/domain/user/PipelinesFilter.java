/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;

import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.BUILDING_STATE;
import static com.thoughtworks.go.server.domain.user.DashboardFilter.FAILED_STATE;

public class PipelinesFilter {
    final Set<String> state;
    final List<CaseInsensitiveString> pipelines;

    PipelinesFilter(Set<String> state, List<CaseInsensitiveString> pipelines) {
        this.state = state;
        this.pipelines = DashboardFilter.enforceList(pipelines);
    }

    boolean filterByPipelineList(CaseInsensitiveString pipelineName) {
        return null != pipelines && !pipelines.isEmpty() && pipelines.contains(pipelineName);
    }

    boolean filterByState(StageInstanceModel stage) {
        if (state == null || stage == null)
            return true;

        if (state.contains(BUILDING_STATE) && state.contains(FAILED_STATE)) {
            return stage.isRunning() || stage.hasFailed();
        } else if (state.contains(BUILDING_STATE)) {
            return stage.isRunning();
        } else if (state.contains(FAILED_STATE)) {
            return stage.hasFailed();
        }
        return true;
    }
}
