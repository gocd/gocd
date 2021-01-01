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
package com.thoughtworks.go.server.controller;

import java.io.UnsupportedEncodingException;

import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import org.springframework.mock.web.MockHttpServletResponse;

public class RestfulActionTestHelper {

    public static void assertContentStatusWithTextPlain(MockHttpServletResponse response,
                                                        int status, String content)
            throws UnsupportedEncodingException {
        assertValidContentAndStatus(response, status, RESPONSE_CHARSET, content);
    }

    public static void assertValidContentAndStatus(MockHttpServletResponse response, int status, String contentType,
                                                   String content) throws UnsupportedEncodingException {
        assertThat(response.getStatus(), is(status));
        assertThat(response.getContentType(), is(contentType));
        //NOTE: Explicitly using junit assertion here since in IntelliJ this will give us a diff
        assertEquals(content, response.getContentAsString());


    }

}
