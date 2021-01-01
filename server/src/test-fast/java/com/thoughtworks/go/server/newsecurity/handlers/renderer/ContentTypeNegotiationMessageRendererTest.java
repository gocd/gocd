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
package com.thoughtworks.go.server.newsecurity.handlers.renderer;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class ContentTypeNegotiationMessageRendererTest {

    @ParameterizedTest
    @ValueSource(strings = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    void shouldGenerateXMLResponseMessageForContentType(String contentType) {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/")
                .withHeader("Accept", contentType)
                .build();

        final ContentTypeAwareResponse response = new ContentTypeNegotiationMessageRenderer().getResponse(request);

        assertThat(response.getContentType().toString()).isEqualTo(contentType);
        assertThat(response.getFormattedMessage("foo")).isEqualTo("<access-denied>\n  <message>foo</message>\n</access-denied>\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            MediaType.APPLICATION_JSON_VALUE,
            "application/vnd.go.cd.v1+json",
            "application/vnd.go.cd.v2+json",
            "application/vnd.go.cd.v3+json",
            "application/vnd.go.cd.v4+json",
            "application/vnd.go.cd.v5+json",
            "application/vnd.go.cd.v6+json",
            "application/vnd.go.cd.v7+json",
            "application/vnd.go.cd.v8+json",
            "application/vnd.go.cd.v9+json",
            "application/vnd.go.cd.v50+json",
            "application/vnd.go.cd.v99+json"
    })
    void shouldGenerateJSONResponseMessageForContentType(String contentType) {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/")
                .withHeader("Accept", contentType)
                .build();

        final ContentTypeAwareResponse response = new ContentTypeNegotiationMessageRenderer().getResponse(request);

        assertThat(response.getContentType().toString()).isEqualTo(contentType);
        assertThat(response.getFormattedMessage("foo")).isEqualTo("{\n  \"message\": \"foo\"\n}");
    }

    @Test
    void shouldGenerateXMLResponseMessageWhenRequestIsForXMLFile() {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/foo.xml")
                .build();

        final ContentTypeAwareResponse response = new ContentTypeNegotiationMessageRenderer().getResponse(request);

        assertThat(response.getContentType().toString()).isEqualTo("application/xml");
        assertThat(response.getFormattedMessage("foo")).isEqualTo("<access-denied>\n  <message>foo</message>\n</access-denied>\n");
    }

    @Test
    void shouldJsonResponseMessageWhenRequestIsMadeWithoutContentTypeAndItIsNotForXMLFile() {
        final MockHttpServletRequest request = HttpRequestBuilder.GET("/random")
                .build();

        final ContentTypeAwareResponse response = new ContentTypeNegotiationMessageRenderer().getResponse(request);

        assertThat(response.getContentType().toString()).isEqualTo("application/json");
        assertThat(response.getFormattedMessage("foo")).isEqualTo("{\n  \"message\": \"foo\"\n}");
    }
}
