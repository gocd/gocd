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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.mockito.Mockito.mock;

public class DenyAllAccessFilterTest {
    @Test
    void denyAllAccessWhenFilterIsCalled() throws ServletException, IOException {
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final MockHttpServletRequest request = new MockHttpServletRequest();
        new DenyAllAccessFilter().doFilter(request, response, mock(FilterChain.class));

        MockHttpServletResponseAssert.assertThat(response)
                .isForbidden();
    }
}
