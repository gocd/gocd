/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigRevisionTest {
    private Date date;
    private TimeProvider timeProvider;

    @Before
    public void setup() {
        timeProvider = mock(TimeProvider.class);
        date = new Date();
        when(timeProvider.currentTime()).thenReturn(date);
    }

    @Test
    public void shouldGenerateCommentString() {
        GoConfigRevision configRevision = new GoConfigRevision("config-xml", "my-md5", "loser", "100.3.9.71", timeProvider);
        assertThat(configRevision.getComment(), is(String.format("user:loser|timestamp:%s|schema_version:%s|go_edition:OpenSource|go_version:100.3.9.71|md5:my-md5", date.getTime(), GoConstants.CONFIG_SCHEMA_VERSION)));
    }

    @Test
    public void shouldGenerateCommentStringWithJoinCharacterEscaped() {
        GoConfigRevision configRevision = new GoConfigRevision("config-xml", "my-|md5||", "los|er|", "100.3.|9.71||", timeProvider);
        assertThat(configRevision.getComment(), is(String.format("user:los||er|||timestamp:%s|schema_version:%s|go_edition:OpenSource|go_version:100.3.||9.71|||||md5:my-||md5||||", date.getTime(), GoConstants.CONFIG_SCHEMA_VERSION)));
    }

    @Test
    public void shouldParsePartsFromComment() {
        GoConfigRevision configRevision = new GoConfigRevision("config-xml", String.format("user:los||er|||timestamp:%s|schema_version:20|go_edition:Enterprise|go_version:100.3.||9.71|||||md5:my-||md5||||", date.getTime()));
        assertThat(configRevision.getContent(), is("config-xml"));
        assertThat(configRevision.getMd5(), is("my-|md5||"));
        assertThat(configRevision.getGoVersion(), is("100.3.|9.71||"));
        assertThat(configRevision.getGoEdition(), is("Enterprise"));
        assertThat(configRevision.getUsername(), is("los|er|"));
        assertThat(configRevision.getTime(), is(date));
        assertThat(configRevision.getSchemaVersion(), is(20));
    }

    @Test
    public void shouldThrowExceptionWhenCommentIsInvalid() {
        try {
            new GoConfigRevision("config-xml", "foo");
            fail("should have failed for invalid comment");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("failed to parse comment [foo]"));
        }
    }

    @Test
    public void shouldUnderstandEquality() {
        GoConfigRevision rev1 = new GoConfigRevision("blah", "md5", "loser", "2.2.2", new TimeProvider());
        GoConfigRevision rev2 = new GoConfigRevision("blah blah", "md5", "loser 2", "2.2.3", new TimeProvider());

        assertThat(rev1, is(rev2));
    }
}
