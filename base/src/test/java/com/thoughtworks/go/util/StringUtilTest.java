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
package com.thoughtworks.go.util;

import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.util.StringUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilTest {

    @Test
    public void shouldFindSimpleRegExMatch() {
        String url = "http://java.sun.com:80/docs/books/tutorial/essential/regex/test_harness.html";
        String baseUrl = StringUtil.matchPattern("^(http://[^/]*)/", url);
        assertThat(baseUrl).isEqualTo("http://java.sun.com:80");
    }

    @Test
    public void shouldHumanize() {
        assertThat(humanize("camelCase")).isEqualTo("camel case");
        assertThat(humanize("camel")).isEqualTo("camel");
        assertThat(humanize("camelCaseForALongString")).isEqualTo("camel case for a long string");
    }

    @Test
    public void shouldStripTillLastOccurrenceOfGivenString() {
        assertThat(stripTillLastOccurrenceOf("HelloWorld@@\\nfoobar\\nquux@@keep_this", "@@")).isEqualTo("keep_this");
        assertThat(stripTillLastOccurrenceOf("HelloWorld", "@@")).isEqualTo("HelloWorld");
        assertThat(stripTillLastOccurrenceOf(null, "@@")).isNull();
        assertThat(stripTillLastOccurrenceOf("HelloWorld", null)).isEqualTo("HelloWorld");
        assertThat(stripTillLastOccurrenceOf(null, null)).isNull();
        assertThat(stripTillLastOccurrenceOf("", "@@")).isEqualTo("");
    }

    @Test
    public void shouldUnQuote() {
        assertThat(unQuote("\"Hello World\"")).isEqualTo("Hello World");
        assertThat(unQuote(null)).isNull();
        assertThat(unQuote("\"Hello World\" to everyone\"")).isEqualTo("Hello World\" to everyone");
    }

}
