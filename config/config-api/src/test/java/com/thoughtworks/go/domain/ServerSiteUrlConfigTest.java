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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerSiteUrlConfigTest {
    @Test
    public void shouldGenerateSiteUrlForGivenPath() throws URISyntaxException {
        ServerSiteUrlConfig url = new SiteUrl("http://someurl.com");
        assertThat(url.siteUrlFor("/foo/bar")).isEqualTo("/foo/bar");
        assertThat(url.siteUrlFor("http/bar")).isEqualTo("http/bar");
    }

    @Test
    public void shouldGenerateSiteUrlForGivenUrl() throws URISyntaxException {
        ServerSiteUrlConfig url = new SiteUrl("http://someurl.com");
        assertThat(url.siteUrlFor("http://test.host/foo/bar")).isEqualTo("http://someurl.com/foo/bar");
    }

    @Test
    public void shouldGenerateSiteUrlUsingPortFromConfiguredSiteUrl() throws URISyntaxException {
        ServerSiteUrlConfig url = new SiteUrl("http://someurl.com:8153");
        assertThat(url.siteUrlFor("http://test.host/foo/bar")).isEqualTo("http://someurl.com:8153/foo/bar");
        assertThat(url.siteUrlFor("http://test.host:3000/foo/bar")).isEqualTo("http://someurl.com:8153/foo/bar");
        url = new SiteUrl("http://someurl.com:8153/");
        assertThat(url.siteUrlFor("http://test.host/foo/bar")).isEqualTo("http://someurl.com:8153/foo/bar");
        assertThat(url.siteUrlFor("http://test.host:4000/foo/bar")).isEqualTo("http://someurl.com:8153/foo/bar");
    }

    @Test
    public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForQueryString() throws URISyntaxException {
        ServerSiteUrlConfig url = new SiteUrl("http://someurl.com");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar")).isEqualTo("http://someurl.com/foo/bar?foo=bar");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar&baz=quux")).isEqualTo("http://someurl.com/foo/bar?foo=bar&baz=quux");
        url = new SiteUrl("http://someurl.com/");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar")).isEqualTo("http://someurl.com/foo/bar?foo=bar");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar&baz=quux")).isEqualTo("http://someurl.com/foo/bar?foo=bar&baz=quux");
    }

    @Test
    public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForFragment() throws URISyntaxException {
        ServerSiteUrlConfig url = new SiteUrl("http://someurl.com");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar#quux")).isEqualTo("http://someurl.com/foo/bar?foo=bar#quux");
        url = new SiteUrl("http://someurl.com/");
        assertThat(url.siteUrlFor("http://test.host/foo/bar#something")).isEqualTo("http://someurl.com/foo/bar#something");
    }

    @Test
    public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForAuth() throws URISyntaxException {
        ServerSiteUrlConfig url = new SiteUrl("http://someurl.com");
        assertThat(url.siteUrlFor("http://admin:badger@test.host/foo")).isEqualTo("http://admin:badger@someurl.com/foo");
        assertThat(url.siteUrlFor("http://admin@test.host/foo")).isEqualTo("http://admin@someurl.com/foo");
    }

    @Test
    public void shouldReturnUrlForToString() {
        ServerSiteUrlConfig url = new SiteUrl("http://someurl.com");
        assertThat(url.toString()).isEqualTo("http://someurl.com");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    public void shouldHandleBlankUrlsConsistently(String input) throws Exception {
        ServerSiteUrlConfig url = new SiteUrl(input);
        assertThat(url.toString()).isEmpty();
        assertThat(url.isBlank()).isTrue();
        assertThat(url.isAHttpsUrl()).isFalse();
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar#quux")).isEqualTo("http://test.host/foo/bar?foo=bar#quux");
    }
}
