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
package com.thoughtworks.go.domain.feed.stage;

import java.util.Date;

import com.thoughtworks.go.config.MingleConfig;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.server.ui.MingleCard;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StageFeedEntryTest {
    @Test
    public void shouldNotAddDuplicateMingleCardsToMingleCardsList(){
        StageFeedEntry entry = new StageFeedEntry(1, 1, new StageIdentifier(), 1, new Date(), StageResult.Passed);
        entry.addCard(new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#1234"));
        entry.addCard(new MingleCard(new MingleConfig("mingle-url", "project-name", null), "#1234"));
        entry.addCard(new MingleCard(new MingleConfig("mingle-url", "project-name-2", null), "#5678"));
        assertThat(entry.getMingleCards().size(), is(2));
    }

    @Test
    public void shouldNotAddDuplicateAuthorsCardsToAuthorsList(){
        StageFeedEntry entry = new StageFeedEntry(1, 1, new StageIdentifier(), 1, new Date(), StageResult.Passed);
        entry.addAuthor(new Author("name", "email"));
        entry.addAuthor(new Author("name", "email"));
        entry.addAuthor(new Author("name1", "email1"));
        assertThat(entry.getAuthors().size(), is(2));
    }
}
