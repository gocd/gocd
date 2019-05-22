/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.domain.PipelineTimelineEntry;

import java.util.ArrayList;
import java.util.TreeSet;

// this can have things like timestamp etc based on which this object can be evicted from PipelineTimeline.pipelineToEntries
public class PipelineTimelineEntrySet {
    private volatile long maximumId;
    private TreeSet<PipelineTimelineEntry> naturalOrderSet;
    private ArrayList<PipelineTimelineEntry> scheduledOrderSet;

    public PipelineTimelineEntrySet() {
        maximumId = -1;
        naturalOrderSet = new TreeSet<>();
        scheduledOrderSet = new ArrayList<>();
    }
    public TreeSet<PipelineTimelineEntry> getNaturalOrderSet() {
        return naturalOrderSet;
    }

    public ArrayList<PipelineTimelineEntry> getScheduledOrderSet() {
        return scheduledOrderSet;
    }

    public void updateMaximumId(long id) {
        maximumId = Math.max(id, maximumId);
    }

    public void setMaximumId(long id) {
        maximumId = id;
    }

    public long getMaximumId() {
        return maximumId;
    }
}
