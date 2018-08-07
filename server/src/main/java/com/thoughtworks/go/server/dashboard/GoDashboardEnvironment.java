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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.server.domain.Username;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GoDashboardEnvironment implements DashboardGroup {
    public GoDashboardEnvironment(String name, Permissions permissions) {
        this.name = name;
        this.permissions = permissions;
    }

    private String name;
    private Permissions permissions;
    private Map<String, GoDashboardPipeline> pipelines = new LinkedHashMap<>();

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> pipelines() {
        return pipelines.keySet();
    }

    @Override
    public boolean canAdminister(Username username) {
        return permissions.admins().contains(username.getUsername().toString());
    }

    @Override
    public String etag() {
        return digest(Integer.toString(permissions.hashCode()), pipelines.values());
    }

    public boolean hasPipelines() {
        return !pipelines.isEmpty();
    }

    public void addPipeline(GoDashboardPipeline pipeline) {
        if (pipeline != null) {
            pipelines.put(pipeline.name().toString(), pipeline);
        }
    }
}
