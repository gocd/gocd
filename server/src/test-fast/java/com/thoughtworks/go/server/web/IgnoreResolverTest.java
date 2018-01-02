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

package com.thoughtworks.go.server.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class IgnoreResolverTest {
    @DataPoint public static final String URL_CRUISE_LOGIN = "/go/auth/login";
    @DataPoint public static final String URL_CRUISE_LOGIN_WITH_SESSION_ID = "/go/auth/login;jsessionid=1b4x150x1eln94";
    @DataPoint public static final String URL_DOWNLOAD_AGENT_JAR = "/go/admin/agent";
    @DataPoint public static final String URL_ABOUT = "/go/about";
    @DataPoint public static final String URL_PLUGIN_INTERACT = "/go/plugin/interact/plugin.id/request.name";
    @DataPoint public static final String STYLESHEETS = "/go/stylesheets/foo.css?world=hello";
    @DataPoint public static final String STYLESHEETS_FROM_CSS_DIRECTORY = "/go/css/hello.css?foo=bar";
    @DataPoint public static final String JAVASCRIPTS = "/go/javascripts/hello.js?foo=bar";
    @DataPoint public static final String JAVASCRIPTS_FROM_DEEP_DOWN = "/go/javascripts/foo/bar/baz.js?hello=world";
    @DataPoint public static final String ALL_JS = "/go/compressed/all.js?foo=bar";
    //images
    @DataPoint public static final String PNG = "/go/images/foo.png?foo=bar";
    @DataPoint public static final String JPG = "/go/hello/bar.jpg";
    @DataPoint public static final String JPEG = "/go/quux/baz.jpeg";
    @DataPoint public static final String GIF = "/go/images/quux.gif";
    @DataPoint public static final String PSD = "/go/images/quux.psd";
    @DataPoint public static final String ICO = "/go/images/hello.ico";

    private MockHttpServletRequest mockHttpServletRequest;
    private IgnoreResolver ignoreResolver = new IgnoreResolver();

    @Before
    public void setUp() {
        mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setContextPath("/go");
    }

    @Theory
    public void shouldIgnoreTheRequestTo(String url) throws Exception {
        mockHttpServletRequest.setRequestURI(url);
        assertThat(url + " should be ignored, but not", ignoreResolver.shouldIgnore(mockHttpServletRequest), is(true));
    }

    @Test
    public void shouldNotIgnoreSuchUrl() throws URISyntaxException {
        String url = "/go/tab/pipeline/about";
        mockHttpServletRequest.setRequestURI(url);
        assertThat(url + " should not be ignored", ignoreResolver.shouldIgnore(mockHttpServletRequest), is(false));
    }

    @Test
    public void shouldIgnorePostRequest() {
        mockHttpServletRequest.setMethod("POST");
        assertThat("should ignore post request", ignoreResolver.shouldIgnore(mockHttpServletRequest), is(true));
    }

    @Test
    public void shouldIgnorePutRequest() {
        mockHttpServletRequest.setMethod("PUT");
        assertThat("should ignore put request", ignoreResolver.shouldIgnore(mockHttpServletRequest), is(true));
    }
}
