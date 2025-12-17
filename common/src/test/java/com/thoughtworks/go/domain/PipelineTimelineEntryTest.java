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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.domain.PipelineTimelineEntry.EarliestRev;
import com.thoughtworks.go.util.Dates;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static com.thoughtworks.go.helper.PipelineTimelineEntryMother.entryRev;
import static com.thoughtworks.go.helper.PipelineTimelineEntryMother.timelineEntry;
import static org.assertj.core.api.Assertions.assertThat;

public class PipelineTimelineEntryTest {
    @Test
    public void shouldReturn0IfComparedToItself() {
        ZonedDateTime now = ZonedDateTime.now();
        PipelineTimelineEntry self = timelineEntry(List.of("flyweight"), 1, now);
        //noinspection EqualsWithItself contract testing
        assertThat(self.compareTo(self)).isEqualTo(0);

        PipelineTimelineEntry another = timelineEntry(List.of("flyweight"), 1, now);
        assertThat(self.compareTo(another)).isEqualTo(0);
        assertThat(another.compareTo(self)).isEqualTo(0);
    }

    @Test
    public void shouldCompareWhenThisEntryOccurredBeforeTheOtherEntry() {
        PipelineTimelineEntry entry = timelineEntry(List.of("flyweight"), 1, ZonedDateTime.now());
        PipelineTimelineEntry that = timelineEntry(2, List.of("flyweight"), List.of(ZonedDateTime.now().plusMinutes(1)), 1);

        assertThat(entry.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(entry)).isEqualTo(1);
    }

    @Test
    public void shouldCompareModsWithMultipleMaterials() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry entry = timelineEntry(1, materials, List.of(now.plusMinutes(1), now.plusMinutes(3)), 1);
        PipelineTimelineEntry that = timelineEntry(2, materials, List.of(now.plusMinutes(4), now.plusMinutes(2)), 1);

        assertThat(entry.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(entry)).isEqualTo(1);
    }

    @Test
    public void shouldCompareModsWithMultipleMaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry entry = timelineEntry(1, materials, List.of(now, now.plusMinutes(3)), 1);
        PipelineTimelineEntry that = timelineEntry(2, materials, List.of(now, now.plusMinutes(2)), 1);

        assertThat(entry.compareTo(that)).isEqualTo(1);
        assertThat(that.compareTo(entry)).isEqualTo(-1);
    }

    @Test
    public void shouldCompareModsWithNoMaterialsChanged() {
        List<String> materials = List.of("flyweight", "another");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry entry = timelineEntry(1, materials, List.of(now, now.plusMinutes(3)), 1);
        PipelineTimelineEntry that = timelineEntry(2, materials, List.of(now, now.plusMinutes(3)), 2);

        assertThat(entry.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(entry)).isEqualTo(1);
    }

    @Test
    public void shouldBreakTieOnMinimumUsingPipelineCounter() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        ZonedDateTime now = ZonedDateTime.now();

        //Because there is a tie on the lowest value i.e. date 2, use the counter to order
        PipelineTimelineEntry entry = timelineEntry(1, materials, List.of(now, now.plusMinutes(3), now.plusMinutes(2), now.plusMinutes(4)), 1);
        PipelineTimelineEntry that = timelineEntry(2, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(2)), 2);

        assertThat(entry.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(entry)).isEqualTo(1);
    }

    @Test
    public void shouldBreakTieOnMinimumUsingPipelineCounterDiscoveringEarliestRevLater() {
        List<String> materials = List.of("first", "second", "third");
        ZonedDateTime now = ZonedDateTime.now();

        // there is a tie on the lowest value but we discover it later
        PipelineTimelineEntry entry = timelineEntry(1, materials, List.of(now.plusMinutes(6), now.plusMinutes(3), now), 1);
        PipelineTimelineEntry that = timelineEntry(2, materials, List.of(now.plusMinutes(6), now, now.plusMinutes(4)), 2);

        assertThat(entry.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(entry)).isEqualTo(1);
    }

    @Test
    public void shouldCompareModsWith4MaterialsWithOneMaterialNotChanged() {
        List<String> materials = List.of("first", "second", "third", "fourth");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry entry = timelineEntry(1, materials, List.of(now, now.plusMinutes(3), now.plusMinutes(2), now.plusMinutes(4)), 1);
        PipelineTimelineEntry that = timelineEntry(2, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3), now.plusMinutes(1)), 2);

        assertThat(entry.compareTo(that)).isEqualTo(1);
        assertThat(that.compareTo(entry)).isEqualTo(-1);
    }

    @Test
    public void shouldCompareModsUsingCounterToBreakTies() {
        List<String> materials = List.of("first", "second", "third");
        ZonedDateTime now = ZonedDateTime.now();

        PipelineTimelineEntry entry = timelineEntry(1, materials, List.of(now, now.plusMinutes(3), now.plusMinutes(2)), 1);
        PipelineTimelineEntry that = timelineEntry(2, materials, List.of(now, now.plusMinutes(2), now.plusMinutes(3)), 2);

        assertThat(entry.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(entry)).isEqualTo(1);
    }

    @Test
    public void shouldIgnoreExtraMaterialForComparison() {
        ZonedDateTime now = ZonedDateTime.now();

        //Ignore the extra material
        PipelineTimelineEntry entry = timelineEntry(1, List.of("first", "second", "third"), List.of(now, now.plusMinutes(3), now.plusMinutes(2)), 1);
        PipelineTimelineEntry that = timelineEntry(2, List.of("first", "second"), List.of(now, now.plusMinutes(2)), 2);

        assertThat(entry.compareTo(that)).isEqualTo(1);
        assertThat(that.compareTo(entry)).isEqualTo(-1);

        //Now break the tie using counter and ignore the extra third material
        entry = timelineEntry(1, List.of("first", "second", "third"), List.of(now, now.plusMinutes(3), now.plusMinutes(2)), 1);
        that = timelineEntry(2, List.of("first", "second"), List.of(now, now.plusMinutes(3)), 2);

        assertThat(entry.compareTo(that)).isEqualTo(-1);
        assertThat(that.compareTo(entry)).isEqualTo(1);
    }

    @Test
    public void shouldIgnoreExtraMaterialForComparisonWithNoMatchingMaterials() {
        ZonedDateTime now = ZonedDateTime.now();

        //Ignore the extra material
        PipelineTimelineEntry entry = timelineEntry(1, List.of("first"), List.of(now.plusMinutes(2)), 1);
        PipelineTimelineEntry that = timelineEntry(2, List.of("second"), List.of(now), 2);

        assertThat(entry.compareTo(that)).isEqualTo(-1); // via counter
        assertThat(that.compareTo(entry)).isEqualTo(1); // via counter
    }

    @Nested
    public class EarliestRevTests {
        @Test
        public void maxIsInconclusive() {
            assertThat(EarliestRev.MAX_VALUE.isInconclusive()).isTrue();
        }

        @Test
        public void shouldBeUnchangedOnNullDates() {
            assertThat(EarliestRev.MAX_VALUE.chooseEarliest(null, null)).isSameAs(EarliestRev.MAX_VALUE);
            assertThat(EarliestRev.MAX_VALUE.chooseEarliest(null, entryRev(ZonedDateTime.now()))).isSameAs(EarliestRev.MAX_VALUE);
            assertThat(EarliestRev.MAX_VALUE.chooseEarliest(entryRev(ZonedDateTime.now()), null)).isSameAs(EarliestRev.MAX_VALUE);
        }

        @Test
        public void shouldBeUnchangedOnEqualDates() {
            ZonedDateTime now = ZonedDateTime.now();
            assertThat(EarliestRev.MAX_VALUE.chooseEarliest(entryRev(now), entryRev(now))).isSameAs(EarliestRev.MAX_VALUE);
        }

        @Test
        public void earliestRevDerivedFromMaxOnLeftSide() {
            ZonedDateTime now = ZonedDateTime.now();
            EarliestRev earliest = EarliestRev.MAX_VALUE.chooseEarliest(entryRev(now), entryRev(now.plusMinutes(1)));

            assertThat(earliest.date()).isEqualTo(Dates.from(now));
            assertThat(earliest.thisComparedToEarliest()).isEqualTo(-1);
            assertThat(earliest.isInconclusive()).isFalse();
        }

        @Test
        public void earliestRevDerivedFromMaxOnRightSide() {
            ZonedDateTime now = ZonedDateTime.now();
            EarliestRev earliest = EarliestRev.MAX_VALUE.chooseEarliest(entryRev(now.plusMinutes(1)), entryRev(now));

            assertThat(earliest.date()).isEqualTo(Dates.from(now));
            assertThat(earliest.thisComparedToEarliest()).isEqualTo(1);
            assertThat(earliest.isInconclusive()).isFalse();
        }

        @Test
        public void greaterResultsAreIgnored() {
            ZonedDateTime now = ZonedDateTime.now();
            EarliestRev earliest = EarliestRev.MAX_VALUE
                .chooseEarliest(entryRev(now), entryRev(now.plusMinutes(1)))
                .chooseEarliest(entryRev(now.plusMinutes(2)), entryRev(now.plusMinutes(3)));

            assertThat(earliest.date()).isEqualTo(Dates.from(now));
            assertThat(earliest.thisComparedToEarliest()).isEqualTo(-1);
            assertThat(earliest.isInconclusive()).isFalse();
        }

        @Test
        public void equivalentResultsAreMaintainedWhenLesser() {
            ZonedDateTime now = ZonedDateTime.now();
            EarliestRev earliest = EarliestRev.MAX_VALUE
                .chooseEarliest(entryRev(now), entryRev(now.plusMinutes(1)));

            EarliestRev earlierSameDirection = earliest
                .chooseEarliest(entryRev(now), entryRev(now.plusMinutes(2)));

            assertThat(earlierSameDirection.date()).isEqualTo(Dates.from(now));
            assertThat(earlierSameDirection.thisComparedToEarliest()).isEqualTo(-1);
            assertThat(earlierSameDirection.isInconclusive()).isFalse();
            assertThat(earlierSameDirection).isSameAs(earliest); // Same object; things did not change
        }

        @Test
        public void equivalentResultsAreMaintainedWhenGreater() {
            ZonedDateTime now = ZonedDateTime.now();
            EarliestRev earliest = EarliestRev.MAX_VALUE
                .chooseEarliest(entryRev(now.plusMinutes(3)), entryRev(now));

            EarliestRev earlierSameDirection = earliest
                .chooseEarliest(entryRev(now.plusMinutes(2)), entryRev(now));

            assertThat(earlierSameDirection.date()).isEqualTo(Dates.from(now));
            assertThat(earlierSameDirection.thisComparedToEarliest()).isEqualTo(1);
            assertThat(earlierSameDirection.isInconclusive()).isFalse();
            assertThat(earlierSameDirection).isSameAs(earliest); // Same object; things did not change
        }

        @Test
        public void inconclusiveResultsStayInconclusive() {
            ZonedDateTime now = ZonedDateTime.now();
            EarliestRev earliest = EarliestRev.MAX_VALUE
                .chooseEarliest(entryRev(now), entryRev(now.plusMinutes(1)))
                .chooseEarliest(entryRev(now.plusMinutes(2)), entryRev(now))
                .chooseEarliest(entryRev(now), entryRev(now.plusMinutes(1)));

            assertThat(earliest.date()).isEqualTo(Dates.from(now));
            assertThat(earliest.thisComparedToEarliest()).isEqualTo(0);
            assertThat(earliest.isInconclusive()).isTrue();
        }
    }
}
