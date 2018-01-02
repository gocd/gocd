/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 *
 */

package com.thoughtworks.go.server.security;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.BadCredentialsException;

import static org.junit.Assert.assertEquals;

public class BasicProcessingFilterEntryPointTest {

    @Test
    public void testShouldRender401WithJSONBodyWithApiAcceptHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Accept", "application/vnd.go.cd.v1+json");

        new BasicProcessingFilterEntryPoint().commence(request, response, null);

        assertEquals("application/vnd.go.cd.v1+json", response.getContentType());
        assertEquals("Basic realm=\"GoCD\"", response.getHeader("WWW-Authenticate"));
        assertEquals(401, response.getStatus());
        assertEquals(response.getContentAsString(), "{\n  \"message\": \"You are not authorized to access this resource!\"\n}\n");
    }

    @Test
    public void testShouldRender401WithJSONBodyWithApiAcceptHeaderForAnyVersion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Accept", "application/vnd.go.cd.v2+json");

        new BasicProcessingFilterEntryPoint().commence(request, response, null);

        assertEquals("application/vnd.go.cd.v2+json", response.getContentType());
        assertEquals("Basic realm=\"GoCD\"", response.getHeader("WWW-Authenticate"));
        assertEquals(401, response.getStatus());
        assertEquals(response.getContentAsString(), "{\n  \"message\": \"You are not authorized to access this resource!\"\n}\n");
    }

    @Test
    public void testShouldRender401WithWithHTMLWithNoAcceptHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new BasicProcessingFilterEntryPoint().commence(request, response, new BadCredentialsException("foo"));

        assertEquals("Basic realm=\"GoCD\"", response.getHeader("WWW-Authenticate"));
        assertEquals(401, response.getStatus());
        assertEquals("foo", response.getErrorMessage());
    }
}
