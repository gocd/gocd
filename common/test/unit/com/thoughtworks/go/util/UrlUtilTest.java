/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UrlUtilTest {

    @Test
    public void shouldEncodeUrl() {
        assertThat(UrlUtil.encodeInUtf8("a%b"), is("a%25b"));
    }

    @Test
    public void shouldEncodeAllPartsInUrl() {
        assertThat(UrlUtil.encodeInUtf8("a%b/c%d"), is("a%25b/c%25d"));
    }

    @Test
    public void shouldKeepPrecedingSlash() {
        assertThat(UrlUtil.encodeInUtf8("/a%b/c%d"), is("/a%25b/c%25d"));
    }

    @Test
    public void shouldKeepTrailingSlash() {
        assertThat(UrlUtil.encodeInUtf8("a%b/c%d/"), is("a%25b/c%25d/"));
    }

    @Test
    public void shouldAppendQueryString() throws Exception {
        assertThat(UrlUtil.urlWithQuery("http://baz.quux", "foo", "bar"), is("http://baz.quux?foo=bar"));
        assertThat(UrlUtil.urlWithQuery("http://baz.quux?bang=boom&hello=world", "foo", "bar"), is("http://baz.quux?bang=boom&hello=world&foo=bar"));
        assertThat(UrlUtil.urlWithQuery("http://baz.quux:1000/hello/world?bang=boom", "foo", "bar"), is("http://baz.quux:1000/hello/world?bang=boom&foo=bar"));
        assertThat(UrlUtil.urlWithQuery("http://baz.quux:1000/hello/world?bang=boom%20bang&quux=bar/baz&sha1=2jmj7l5rSw0yVb%2FvlWAYkK%2FYBwk%3D", "foo", "bar\\baz"), is("http://baz.quux:1000/hello/world?bang=boom+bang&quux=bar%2Fbaz&sha1=2jmj7l5rSw0yVb%2FvlWAYkK%2FYBwk%3D&foo=bar%5Cbaz"));
        assertThat(UrlUtil.urlWithQuery("http://baz.quux:1000/hello/world?bang=boom#in_hell", "foo", "bar"), is("http://baz.quux:1000/hello/world?bang=boom&foo=bar#in_hell"));
        assertThat(UrlUtil.urlWithQuery("http://user:loser@baz.quux:1000/hello/world#in_hell", "foo", "bar"), is("http://user:loser@baz.quux:1000/hello/world?foo=bar#in_hell"));
    }

    @Test
    public void shouldGetGivenQueryParamFromUrl() throws Exception {
        String url = "http://localhost:8153?code=123&new_code=xyz";
        assertThat(UrlUtil.getQueryParamFromUrl(url, "code"),is("123"));
        assertThat(UrlUtil.getQueryParamFromUrl(url, "new_code"),is("xyz"));
    }

    @Test
    public void shouldReturnEmptyStringIfQueryParamIsNotAvailable() throws Exception {
        String url = "http://localhost:8153?code=123&new_code=xyz";
        assertThat(UrlUtil.getQueryParamFromUrl(url, "not_available"),is(""));
    }

    @Test
    public void shouldReturnEmptyStringIfUrlIsInvalid() throws Exception {
        String url = "this is not valid url";
        assertThat(UrlUtil.getQueryParamFromUrl(url, "param"),is(""));
    }

    @Test
    public void concatPathWithBaseUrl() throws Exception {
        assertThat(UrlUtil.concatPath("http://foo", "bar"), is("http://foo/bar"));
        assertThat(UrlUtil.concatPath("http://foo/", "bar"), is("http://foo/bar"));
    }
}
