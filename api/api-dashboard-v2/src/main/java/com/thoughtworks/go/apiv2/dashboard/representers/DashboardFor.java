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

package com.thoughtworks.go.apiv2.dashboard.representers;

import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup;
import com.thoughtworks.go.server.domain.Username;

import java.util.List;

public class DashboardFor {
    private final List<GoDashboardPipelineGroup> pipelineGroups;
    private final Username username;
    private final boolean superAdmin;

    public DashboardFor(List<GoDashboardPipelineGroup> pipelineGroups, Username username, boolean superAdmin) {
        this.pipelineGroups = pipelineGroups;
        this.username = username;
        this.superAdmin = superAdmin;
    }

    public List<GoDashboardPipelineGroup> getPipelineGroups() {
        return pipelineGroups;
    }

    public Username getUsername() {
        return username;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }
}
