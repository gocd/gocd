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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.domain.PipelineTimelineEntry;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static com.thoughtworks.go.helper.PipelineTimelineEntryMother.modification;
import static org.assertj.core.api.Assertions.assertThat;

public class PipelineTimelineEntryTest {
    @Test
    public void shouldReturn0IfComparedToItself() {
        ZonedDateTime now = ZonedDateTime.now();
        PipelineTimelineEntry self = modification(List.of("flyweight"), 1, "123", now);
        assertThat(self.compareTo(self)).isEqualTo(0);

        PipelineTimelineEntry another = modification(List.of("flyweight"), 1, "123", now);
        assertThat(self.compareTo(another)).isEqualTo(0);
        assertThat(another.compareTo(self)).isEqualTo(0);
    }

    @Test
    public void shouldCompareWhenThisModificationOccurredBeforeTheOtherModification() {
        PipelineTimelineEntry modification = modification(List.of("flyweight"), 1, "123", ZonedDateTime.now());
        PipelineTimelineEntry that = modification(2, List.of("flyweight"), List.of(ZonedDateTime.now().plusMinutes(1)), 1, "123");

        assertThat(modification.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(modification)).isEqualTo(1);
    }

    @Test
    public void shouldCompareModsWithMultipleMaterials() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(now.plusMinutes(1), now.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, List.of(now.plusMinutes(4), now.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(modification)).isEqualTo(1);
    }

    @Test
    public void shouldCompareModsWithMultipleMaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(now, now.plusMinutes(3)), 1, "123");
        PipelineTimelineEntry that = modification(2, materials, List.of(now, now.plusMinutes(2)), 1, "123");

        assertThat(modification.compareTo(that)).isEqualTo(1);
        assertThat(that.compareTo(modification)).isEqualTo(-1);
    }

    @Test
    public void shouldCompareModsWithNoMaterialsChanged() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(now, now.plusMinutes(3)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(now, now.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(modification)).isEqualTo(1);
    }

    @Test
    public void shouldBreakTieOnMinimumUsingPipelineCounter() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        ZonedDateTime now = ZonedDateTime.now();

        //Because there is a tie on the lowest value i.e. date 2, use the counter to order
        PipelineTimelineEntry modification = modification(1, materials, List.of(now, now.plusMinutes(3), now.plusMinutes(2), now.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(modification)).isEqualTo(1);
    }

    @Test
    public void shouldCompareModsWith4MaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(now, now.plusMinutes(3), now.plusMinutes(2), now.plusMinutes(4)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(1)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that)).isEqualTo(1);
        assertThat(that.compareTo(modification)).isEqualTo(-1);
    }

    @Test
    public void shouldCompareModsUsingCounterToBreakTies() {
        List<String> materials = List.of("first", "second", "third");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry modification = modification(1, materials, List.of(now, now.plusMinutes(3), now.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(modification)).isEqualTo(1);
    }

    @Test
    public void shouldIgnoreExtraMaterialForComparison() {
        ZonedDateTime now = ZonedDateTime.now();

        //Ignore the extra material
        PipelineTimelineEntry modification = modification(1, List.of("first", "second", "third"), List.of(now, now.plusMinutes(3), now.plusMinutes(2)), 1, "123", "pipeline");
        PipelineTimelineEntry that = modification(2, List.of("first", "second"), List.of(now, now.plusMinutes(2)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that)).isEqualTo(1);
        assertThat(that.compareTo(modification)).isEqualTo(-1);

        //Now break the tie using counter and ignore the extra third material
        modification = modification(1, List.of("first", "second", "third"), List.of(now, now.plusMinutes(3), now.plusMinutes(2)), 1, "123", "pipeline");
        that = modification(2, List.of("first", "second"), List.of(now, now.plusMinutes(3)), 2, "123", "pipeline");

        assertThat(modification.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(modification)).isEqualTo(1);
    }
}
