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
package com.thoughtworks.go.domain;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ServerSiteUrlConfigTest {

    @Test
    public void shouldReturnUrlForToString() {
        ServerSiteUrlConfig url = new SiteUrl("http://siteurl.com");
        assertThat(url.toString()).isEqualTo("http://siteurl.com");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    public void shouldHandleBlankUrlsConsistently(String input) throws Exception {
        ServerSiteUrlConfig url = new SiteUrl(input);
        assertThat(url.toString()).isEmpty();
        assertThat(url.isBlank()).isTrue();
        assertThat(url.isHttps()).isFalse();
        assertThat(url.withPath("/foo/bar?foo=bar#quux")).isEmpty();
    }

    @Nested
    class WithPath {
        @Test
        public void shouldGenerateSiteUrlForGivenPath() throws Exception {
            ServerSiteUrlConfig url = new SiteUrl("http://siteurl.com");
            assertThat(url.withPath("/foo/bar")).contains(url("http://siteurl.com/foo/bar"));
        }

        @Test
        public void shouldFailWithRelativePath() {
            ServerSiteUrlConfig url = new SiteUrl("http://siteurl.com");
            assertThatThrownBy(() -> url.withPath("http/bar"))
                .isInstanceOf(URISyntaxException.class)
                .hasMessageContaining("Relative path");
        }

        @Test
        public void shouldGenerateSiteUrlUsingPortFromConfiguredSiteUrl() throws Exception {
            ServerSiteUrlConfig url = new SiteUrl("http://siteurl.com:8153");
            assertThat(url.withPath("/foo/bar")).contains(url("http://siteurl.com:8153/foo/bar"));
            assertThat(url.withPath("/foo/bar")).contains(url("http://siteurl.com:8153/foo/bar"));
            url = new SiteUrl("http://siteurl.com:8153/");
            assertThat(url.withPath("/foo/bar")).contains(url("http://siteurl.com:8153/foo/bar"));
            assertThat(url.withPath("/foo/bar")).contains(url("http://siteurl.com:8153/foo/bar"));
        }

        @Test
        public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForQueryString() throws Exception {
            ServerSiteUrlConfig url = new SiteUrl("http://siteurl.com");
            assertThat(url.withPath("/foo/bar?foo=bar")).contains(url("http://siteurl.com/foo/bar?foo=bar"));
            assertThat(url.withPath("/foo/bar?foo=bar&baz=quux")).contains(url("http://siteurl.com/foo/bar?foo=bar&baz=quux"));
            url = new SiteUrl("http://siteurl.com/");
            assertThat(url.withPath("/foo/bar?foo=bar")).contains(url("http://siteurl.com/foo/bar?foo=bar"));
            assertThat(url.withPath("/foo/bar?foo=bar&baz=quux")).contains(url("http://siteurl.com/foo/bar?foo=bar&baz=quux"));
        }

        @Test
        public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForFragment() throws Exception {
            ServerSiteUrlConfig url = new SiteUrl("http://siteurl.com");
            assertThat(url.withPath("/foo/bar?foo=bar#quux")).contains(url("http://siteurl.com/foo/bar?foo=bar#quux"));
            url = new SiteUrl("http://siteurl.com/");
            assertThat(url.withPath("/foo/bar#something")).contains(url("http://siteurl.com/foo/bar#something"));
        }

        private static @NonNull URL url(String url) throws MalformedURLException {
            return URI.create(url).toURL();
        }
    }
}
