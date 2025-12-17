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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.util.Dates;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class PipelineTimelineEntryMother {
    private static long revId = 1;

    public static PipelineTimelineEntry timelineEntry(List<String> materials, int id, ZonedDateTime... times) {
        return timelineEntry(1, materials, Arrays.asList(times), id);
    }

    public static PipelineTimelineEntry timelineEntry(long id, List<String> materials, List<ZonedDateTime> times, int counter) {
        return timelineEntry(id, materials, times, counter, "123", "pipeline");
    }

    public static PipelineTimelineEntry timelineEntry(long id, List<String> materials, List<ZonedDateTime> times, int counter, String rev) {
        return timelineEntry(id, materials, times, counter, rev, "pipeline");
    }

    public static PipelineTimelineEntry timelineEntry(long id, List<String> materials, List<ZonedDateTime> times, int counter, String rev, String pipelineName) {
        assertThat(materials.size()).as("Setup data so there is one time for every material").isEqualTo(times.size());

        Map<String, List<PipelineTimelineEntry.Revision>> revisionsByFingerprint = new HashMap<>();
        for (int i = 0; i < materials.size(); i++) {
            String fingerprint = materials.get(i);
            revisionsByFingerprint.put(fingerprint, List.of(entryRev(times.get(i), rev)));
        }
        return new PipelineTimelineEntry(pipelineName, id, counter, revisionsByFingerprint);
    }

    public static PipelineTimelineEntry.Revision entryRev(ZonedDateTime date, String rev) {
        return new PipelineTimelineEntry.Revision(Dates.from(date), rev, PipelineTimelineEntryMother.revId++);
    }

    public static PipelineTimelineEntry.Revision entryRev(ZonedDateTime date) {
        return entryRev(date, "123");
    }
}
