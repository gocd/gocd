/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.helper.PipelineTimelineEntryMother.modification;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PipelineTimelineEntryTest {
    @Test
    public void shouldReturn0IfComparedToItself() {
        DateTime now = new DateTime();
        PipelineTimelineEntry self = modification(List.of("flyweight"), 1, "123", now);
        assertThat(self.compareTo(self), is(0));

        PipelineTimelineEntry another = modification(List.of("flyweight"), 1, "123", now);
        assertThat(self.compareTo(another), is(0));
        assertThat(another.compareTo(self), is(0));
    }

    @Test
    public void shouldCompareWhenThisModificationOccurredBeforeTheOtherModification() {
        PipelineTimelineEntry modification = modification(List.of("flyweight"), 1, "123", new DateTime());
        PipelineTimelineEntry that = modification(2, List.of("flyweight"), List.of(new DateTime().plusMinutes(1)), 1, "123");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test
    public void shouldCompareModsWithMultipleMaterials() {
        List<String> materials = List.of("flyweight", "another");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base.plusMinutes(1), base.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, List.of(base.plusMinutes(4), base.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test
    public void shouldCompareModsWithMultipleMaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("flyweight", "another");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));
    }

    @Test
    public void shouldCompareModsWithNoMaterialsChanged() {
        List<String> materials = List.of("flyweight", "another");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test
    public void shouldBreakTieOnMinimumUsingPipelineCounter() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        DateTime base = new DateTime();

        //Because there is a tie on the lowest value i.e. date 2, use the counter to order
        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3), base.plusMinutes(2), base.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2), base.plusMinutes(3), base.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test
    public void shouldCompareModsWith4MaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3), base.plusMinutes(2), base.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2), base.plusMinutes(3), base.plusMinutes(1)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));
    }

    @Test
    public void shouldCompareModsUsingCounterToBreakTies() {
        List<String> materials = List.of("first", "second", "third");
        DateTime base = new DateTime();

        PipelineTimelineEntry modification = modification(1, materials, List.of(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(base, base.plusMinutes(2), base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }

    @Test
    public void shouldIgnoreExtraMaterialForComparison() {
        DateTime base = new DateTime();

        //Ignore the extra material
        PipelineTimelineEntry modification = modification(1, List.of("first", "second", "third"), List.of(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, List.of("first", "second"), List.of(base, base.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(1));
        assertThat(that.compareTo(modification), is(-1));

        //Now break the tie using counter and ignore the extra third material
        modification = modification(1, List.of("first", "second", "third"), List.of(base, base.plusMinutes(3), base.plusMinutes(2)), 1, "123", "pipeline");
        that = modification(2, List.of("first", "second"), List.of(base, base.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that), is(-1));
        assertThat(that.compareTo(modification), is(1));
    }
}
