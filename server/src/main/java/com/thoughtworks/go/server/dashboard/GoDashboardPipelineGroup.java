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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.server.domain.Username;

public class GoDashboardPipelineGroup extends AbstractDashboardGroup {
    private Permissions permissions;

    public GoDashboardPipelineGroup(String name, Permissions permissions, boolean hasDefinedPipelines) {
        super(name, hasDefinedPipelines);
        this.permissions = permissions;
    }

    @Override
    public boolean canAdminister(Username username) {
        return permissions.admins().contains(username.getUsername().toString());
    }

    @Override
    public String etag() {
        return digest(Integer.toString(permissions.hashCode()));
    }

    public boolean canBeViewedBy(Username userName) {
        return permissions.viewers().contains(userName.getUsername().toString());
    }

    public boolean hasPermissions() {
        return permissions != null;
    }
}
