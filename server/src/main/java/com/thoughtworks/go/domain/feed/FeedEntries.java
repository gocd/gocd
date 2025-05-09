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

import com.thoughtworks.go.domain.BaseCollection;

import java.util.Date;
import java.util.List;

/**
 * Understands a collection of feeds
 */
public class FeedEntries extends BaseCollection<FeedEntry> {

    public FeedEntries(List<FeedEntry> feeds) {
        super(feeds);
    }

    public FeedEntries(FeedEntry... feeds) {
        super(feeds);
    }

    public FeedEntries() {
    }

    public Long firstEntryId() {
        return isEmpty() ? null : first().getEntryId();
    }

    public Long lastEntryId() {
        return isEmpty() ? null : last().getEntryId();
    }

    public Date lastUpdatedDate() {
        return isEmpty() ? new Date() : first().getUpdatedDate();
    }
}
