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

package com.thoughtworks.go.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class StringUtilTest {
    @Test public void shouldQuoteJavascriptString() throws Exception {
        assertThat(StringUtil.quoteJavascriptString("a b \\c \"d"), is("\"a b \\\\c \\\"d\""));
    }

    @Test public void shouldFindSimpleRegExMatch() throws Exception {
        String url = "http://java.sun.com:80/docs/books/tutorial/essential/regex/test_harness.html";
        String baseUrl = StringUtil.matchPattern("^(http://[^/]*)/", url);
        assertThat(baseUrl, is("http://java.sun.com:80"));
    }

    @Test public void shouldReportPasswordAsStars() throws Exception {
        assertThat(StringUtil.passwordToString("foo"), is("***"));
        assertThat(StringUtil.passwordToString(null), is("not-set"));
    }

    @Test
    public void shouldCalculateSha1Digest() throws IOException {
        File tempFile = TestFileUtil.createTempFile("for_digest.test");
        FileUtils.writeStringToFile(tempFile, "12345");
        assertThat(StringUtil.sha1Digest(tempFile), is("jLIjfQZ5yojbZGTqxg2pY0VROWQ="));
    }

    @Test
    public void shouldHumanize() throws Exception {
        assertThat(StringUtil.humanize("camelCase"), is("camel case"));
        assertThat(StringUtil.humanize("camel"), is("camel"));
        assertThat(StringUtil.humanize("camelCaseForALongString"), is("camel case for a long string"));
    }

    @Test
    public void shouldBeBlankForNullOrEmpty() {
        assertThat(StringUtil.isBlank(""), is(true));
        assertThat(StringUtil.isBlank(null), is(true));
        assertThat(StringUtil.isBlank("  "), is(true));
        assertThat(StringUtil.isBlank("abcd"), is(false));
        assertThat(StringUtil.isBlank("ab cd"), is(false));
    }

    @Test
    public void shouldJoinWithCharEscaped() {
        assertThat(StringUtil.escapeAndJoinStrings("foo", "bar", "baz", "hi_bye"), is("foo_bar_baz_hi__bye"));
    }

    @Test
    public void shouldJoinWithNullsRepresentedAsBlankStrings() {
        assertThat(StringUtil.escapeAndJoinStrings("foo", null, "hi_bye", null, null), is("foo__hi__bye__"));
    }

    @Test
    public void shouldStripTrailingSlash(){
        assertThat(StringUtil.stripTrailingSlash(null), nullValue());
        assertThat(StringUtil.stripTrailingSlash(""), is(""));
        assertThat(StringUtil.stripTrailingSlash("123/"), is("123"));
        assertThat(StringUtil.stripTrailingSlash("123"), is("123"));
        assertThat(StringUtil.stripTrailingSlash("abc/def/g/"), is("abc/def/g"));
    }

    @Test
    public void shouldStripTillLastOccurrenceOfGivenString(){
        assertThat(StringUtil.stripTillLastOccurrenceOf("HelloWorld@@\\nfoobar\\nquux@@keep_this", "@@"), is("keep_this"));
        assertThat(StringUtil.stripTillLastOccurrenceOf("HelloWorld", "@@"), is("HelloWorld"));
        assertThat(StringUtil.stripTillLastOccurrenceOf(null, "@@"), is(nullValue()));
        assertThat(StringUtil.stripTillLastOccurrenceOf("HelloWorld", null), is("HelloWorld"));
        assertThat(StringUtil.stripTillLastOccurrenceOf(null, null), is(nullValue()));
        assertThat(StringUtil.stripTillLastOccurrenceOf("", "@@"), is(""));
    }

}