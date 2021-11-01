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

package com.thoughtworks.go.spark;

import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.spark.HtmlErrorPage.errorPage;
import static org.assertj.core.api.Assertions.assertThat;

class HtmlErrorPageTest {

    @Test
    public void shouldReplaceStatusCodeAndMessage() {
        assertThat(errorPage(404, "Error message"))
                .contains("<h1>404</h1>")
                .contains("<h2>Error message</h2>");
    }

    @Test
    public void shouldEscapeHtmlInMessages() {
        String htmlInMessage = "<img src=\"blah\"/>";
        assertThat(errorPage(404, htmlInMessage))
                .doesNotContain(htmlInMessage)
                .contains("<h2>&lt;img src=&quot;blah&quot;/&gt;</h2>");
    }
}