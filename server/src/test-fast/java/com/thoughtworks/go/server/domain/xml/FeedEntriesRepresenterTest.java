/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.domain.xml;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.junit5.FileSource;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.xmlunit.assertj.XmlAssert;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedEntriesRepresenterTest {
    @ParameterizedTest
    @FileSource(files = "/feeds/stages-with-entries.xml")
    void shouldGenerateFeedXml(String expectedXML) {
        String pipelineName = "up42";
        StageFeedEntry entryOne = cancelled();
        StageFeedEntry entryTwo = passed();
        entryOne.getAuthors().add(new Author("bob", "bob@gocd.org"));
        entryTwo.getAuthors().add(new Author("joe <joe@gocd.org>", null));
        XmlWriterContext context = new XmlWriterContext("https://go-server/go", null, null, null, new SystemEnvironment());
        FeedEntriesRepresenter representable = new FeedEntriesRepresenter(pipelineName, new FeedEntries(entryOne, entryTwo));

        Document document = representable.toXml(context);

        XmlAssert.assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }

    @ParameterizedTest
    @FileSource(files = "/feeds/stages-with-no-entries.xml")
    void shouldGenerateXmlWithoutEntryWhenEmpty(String expectedXML) {
        String pipelineName = "up42";
        XmlWriterContext context = new XmlWriterContext("https://go-server/go", null, null, null, new SystemEnvironment());
        FeedEntries feedEntries = mock(FeedEntries.class);
        when(feedEntries.lastUpdatedDate()).thenReturn(DateUtils.parseISO8601("2019-12-31T07:28:30+05:30"));

        Document document = new FeedEntriesRepresenter(pipelineName, feedEntries).toXml(context);

        XmlAssert.assertThat(document.asXML()).and(expectedXML)
            .ignoreWhitespace()
            .areIdentical();
    }

    private static StageFeedEntry passed() {
        Date date = DateUtils.parseISO8601("2019-12-31T07:28:30+05:30");
        StageIdentifier identifier = new StageIdentifier("up42", 2, "unit-tests", "100");
        return new StageFeedEntry(1L, 1L, identifier, 124L, date, StageResult.Passed, "", "Bob", null);
    }

    private static StageFeedEntry cancelled() {
        Date date = DateUtils.parseISO8601("2019-12-31T07:28:30+05:30");
        StageIdentifier identifier = new StageIdentifier("up42", 2, "integration-tests", "100");
        return new StageFeedEntry(1L, 1L, identifier, 123L, date, StageResult.Cancelled, "", "Bob", "Admin");
    }
}
