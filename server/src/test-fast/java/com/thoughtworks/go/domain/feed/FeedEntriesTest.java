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
package com.thoughtworks.go.domain.feed;

import java.util.Arrays;
import java.util.Date;

import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.lessThan;
import org.joda.time.DateTime;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class FeedEntriesTest {
    private static final Date LATER_DATE = new DateTime().withDate(2000, 12, 24).toDate();
    private static final Date EARLIER_DATE = new DateTime().withDate(2000, 12, 23).toDate();

    @Test
    public void shouldReturnFirstAndLastEntryId() {
        assertThat(feedEntries().firstEntryId(), is(12L));
        assertThat(feedEntries().lastEntryId(), is(15L));
    }

    private FeedEntries feedEntries() {
        return new FeedEntries(Arrays.asList(entry(12L, LATER_DATE), entry(15L, EARLIER_DATE)));
    }

    private FeedEntry entry(long entryId, Date updatedDate) {
        return new StageFeedEntry(11L, 1L, new StageIdentifier(), entryId, updatedDate, StageResult.Cancelled);
    }

    @Test
    public void shouldLastUpdatedDate() {
        assertThat(feedEntries().lastUpdatedDate(), is(LATER_DATE));
    }


    @Test
    public void shouldReturnFirstEntryId_Empty_List() {
        assertThat(new FeedEntries().firstEntryId(), is(nullValue()));
        assertThat(new FeedEntries().lastEntryId(), is(nullValue()));
    }

    @Test
    public void shouldLastUpdatedDate_Empty_List() {
        assertDateWithTolerance(new FeedEntries().lastUpdatedDate(), new DateTime().toDate());
    }

    private void assertDateWithTolerance(Date firstDate, Date secondDate) {
        assertThat(Math.abs(firstDate.getTime() - secondDate.getTime()), lessThan(10L));
    }
}
