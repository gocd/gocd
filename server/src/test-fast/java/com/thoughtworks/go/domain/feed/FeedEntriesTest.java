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
package com.thoughtworks.go.domain.feed;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.util.Dates;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedEntriesTest {
    private static final Date LATER_DATE = Dates.from(LocalDate.of(2000, 12, 24).atStartOfDay());
    private static final Date EARLIER_DATE = Dates.from(LocalDate.of(2000, 12, 23).atStartOfDay());

    @Test
    public void shouldReturnFirstAndLastEntryId() {
        assertThat(feedEntries().firstEntryId()).isEqualTo(12L);
        assertThat(feedEntries().lastEntryId()).isEqualTo(15L);
    }

    private FeedEntries feedEntries() {
        return new FeedEntries(List.of(entry(12L, LATER_DATE), entry(15L, EARLIER_DATE)));
    }

    private FeedEntry entry(long entryId, Date updatedDate) {
        return new StageFeedEntry(11L, 1L, new StageIdentifier(), entryId, updatedDate, StageResult.Cancelled);
    }

    @Test
    public void shouldLastUpdatedDate() {
        assertThat(feedEntries().lastUpdatedDate()).isEqualTo(LATER_DATE);
    }


    @Test
    public void shouldReturnFirstEntryId_Empty_List() {
        assertThat(new FeedEntries().firstEntryId()).isNull();
        assertThat(new FeedEntries().lastEntryId()).isNull();
    }

    @Test
    public void shouldLastUpdatedDate_Empty_List() {
        assertThat(new FeedEntries().lastUpdatedDate()).isCloseTo(Instant.now(), 10L);
    }

}
