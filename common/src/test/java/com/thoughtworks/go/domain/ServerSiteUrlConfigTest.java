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

import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ServerSiteUrlConfigTest {
    @Test
    public void shouldGenerateSiteUrlForGivenPath() throws URISyntaxException {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig("http://someurl.com");
        assertThat(url.siteUrlFor("/foo/bar"), is("/foo/bar"));
        assertThat(url.siteUrlFor("http/bar"), is("http/bar"));
    }

    @Test
    public void shouldGenerateSiteUrlForGivenUrl() throws URISyntaxException {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig("http://someurl.com");
        assertThat(url.siteUrlFor("http://test.host/foo/bar"), is("http://someurl.com/foo/bar"));
    }

    @Test
    public void shouldGenerateSiteUrlUsingPortFromConfiguredSiteUrl() throws URISyntaxException {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig("http://someurl.com:8153");
        assertThat(url.siteUrlFor("http://test.host/foo/bar"), is("http://someurl.com:8153/foo/bar"));
        assertThat(url.siteUrlFor("http://test.host:3000/foo/bar"), is("http://someurl.com:8153/foo/bar"));
        url = new ServerSiteUrlConfig("http://someurl.com:8153/");
        assertThat(url.siteUrlFor("http://test.host/foo/bar"), is("http://someurl.com:8153/foo/bar"));
        assertThat(url.siteUrlFor("http://test.host:4000/foo/bar"), is("http://someurl.com:8153/foo/bar"));
    }

    @Test
    public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForQueryString() throws URISyntaxException {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig("http://someurl.com");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar"), is("http://someurl.com/foo/bar?foo=bar"));
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar&baz=quux"), is("http://someurl.com/foo/bar?foo=bar&baz=quux"));
        url = new ServerSiteUrlConfig("http://someurl.com/");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar"), is("http://someurl.com/foo/bar?foo=bar"));
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar&baz=quux"), is("http://someurl.com/foo/bar?foo=bar&baz=quux"));
    }

    @Test
    public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForFragment() throws URISyntaxException {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig("http://someurl.com");
        assertThat(url.siteUrlFor("http://test.host/foo/bar?foo=bar#quux"), is("http://someurl.com/foo/bar?foo=bar#quux"));
        url = new ServerSiteUrlConfig("http://someurl.com/");
        assertThat(url.siteUrlFor("http://test.host/foo/bar#something"), is("http://someurl.com/foo/bar#something"));
    }

    @Test
    public void shouldGenerateSiteUrlUsingConfiguredSiteUrlForAuth() throws URISyntaxException {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig("http://someurl.com");
        assertThat(url.siteUrlFor("http://admin:badger@test.host/foo"), is("http://admin:badger@someurl.com/foo"));
        assertThat(url.siteUrlFor("http://admin@test.host/foo"), is("http://admin@someurl.com/foo"));
    }

    @Test
    public void shouldReturnUrlForToString() throws Exception {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig("http://someurl.com");
        assertThat(url.toString(), is("http://someurl.com"));
    }

    @Test
    public void shouldReturnEmptyStringForToStringWhenTheUrlIsNotSet() throws Exception {
        ServerSiteUrlConfig url = new ServerSiteUrlConfig();
        assertThat(url.toString(), is(""));
    }
}
