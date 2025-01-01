/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GoDashboardPipelines {
    private final Map<CaseInsensitiveString, GoDashboardPipeline> pipelines;
    private final long lastUpdatedTimeStamp;

    public GoDashboardPipelines(HashMap<CaseInsensitiveString, GoDashboardPipeline> pipelines, TimeStampBasedCounter timeStampBasedCounter) {
        this.pipelines = pipelines;
        this.lastUpdatedTimeStamp = timeStampBasedCounter.getNext();
    }

    public long lastUpdatedTimeStamp() {
        return lastUpdatedTimeStamp;
    }

    public Collection<GoDashboardPipeline> getPipelines() {
        return pipelines.values();
    }

    public GoDashboardPipeline find(CaseInsensitiveString name) {
        return pipelines.get(name);
    }

    public boolean isEmpty() {
        return pipelines.isEmpty();
    }
}

