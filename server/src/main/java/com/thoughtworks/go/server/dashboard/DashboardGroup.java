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

import com.thoughtworks.go.server.domain.Username;

import java.util.Collection;
import java.util.List;

public interface DashboardGroup {
    String name();

    List<String> pipelines();

    boolean canAdminister(Username username);

    String etag();

    Collection<GoDashboardPipeline> allPipelines();

    /**
     * @return whether or not this group has viewable pipelines; this accounts for filtering and permissions
     */
    boolean hasPipelines();

    /**
     * @return whether or not this group has defined pipelines, regardless of permissions and filtering
     */
    boolean hasDefinedPipelines();

    void addPipeline(GoDashboardPipeline pipeline);
}
