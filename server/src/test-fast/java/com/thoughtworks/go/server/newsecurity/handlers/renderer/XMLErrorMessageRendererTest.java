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
package com.thoughtworks.go.server.newsecurity.handlers.renderer;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class XMLErrorMessageRendererTest {

    @Test
    void shouldHandleRequest() {
        final XMLErrorMessageRenderer messageRenderer = new XMLErrorMessageRenderer(MediaType.APPLICATION_XML);
        assertThat(messageRenderer.getContentType()).isEqualTo(MediaType.APPLICATION_XML);
        assertThat(messageRenderer.getFormattedMessage("You are not authorized to access this URL. Some<xml>!")).isEqualTo("<access-denied>\n" +
                "  <message>You are not authorized to access this URL. Some&lt;xml&gt;!</message>\n" +
                "</access-denied>\n");
    }
}
