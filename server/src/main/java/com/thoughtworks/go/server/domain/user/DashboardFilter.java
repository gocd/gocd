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

import java.util.*;

public interface DashboardFilter {
    static <T> List<T> enforceList(List<T> list) {
        return Optional.ofNullable(list).orElse(new ArrayList<>());
    }

    String DEFAULT_NAME = "Default";
    String BUILDING_STATE = "building";
    String FAILED_STATE = "failing";
    Set<String> VALID_STATES = new HashSet<>(Arrays.asList(BUILDING_STATE, FAILED_STATE));

    String name();

    Set<String> state();

    boolean isPipelineVisible(CaseInsensitiveString pipeline);

    /**
     * Idempotent operation on filter to allow a specified pipeline to be visible
     *
     * @param pipeline - the name of the pipeline
     * @return true if the filter was modified, false if unchanged
     */
    boolean allowPipeline(CaseInsensitiveString pipeline);
}
