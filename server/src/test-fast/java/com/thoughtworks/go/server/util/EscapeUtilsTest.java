/*
 * Copyright 2022 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EscapeUtilsTest {
    @Test
    public void escapeHtmlShouldDoNothingForNullValues() {
        assertThat(new EscapeUtils().html(null)).isNull();
    }

    @Test
    public void escapeHtmlShouldCoerceToString() {
        assertThat(new EscapeUtils().html(256)).isEqualTo("256");
    }

    @Test
    public void escapeHtmlShouldEscapeHtml() {
        assertThat(new EscapeUtils().html("<html/>")).isEqualTo("&lt;html/&gt;");
    }

    @Test
    public void escapeJavascriptShouldDoNothingForNullValues() {
        assertThat(new EscapeUtils().javascript(null)).isNull();
    }

    @Test
    public void escapeJavascriptShouldCoerceToString() {
        assertThat(new EscapeUtils().javascript(256)).isEqualTo("256");
    }

    @Test
    public void escapeJavascriptShouldEscapeJavascript() {
        assertThat(new EscapeUtils().javascript("console.warn('world');")).isEqualTo("console.warn(\\'world\\');");
    }
}
