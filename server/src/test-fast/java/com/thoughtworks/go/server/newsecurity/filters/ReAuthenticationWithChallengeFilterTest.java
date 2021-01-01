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

import com.thoughtworks.go.server.newsecurity.handlers.BasicAuthenticationWithChallengeFailureResponseHandler;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReAuthenticationWithChallengeFilterTest {
    @Test
    void shouldInvokeHandler() throws IOException {
        final BasicAuthenticationWithChallengeFailureResponseHandler handler = mock(BasicAuthenticationWithChallengeFailureResponseHandler.class);

        final ReAuthenticationWithChallengeFilter filter = new ReAuthenticationWithChallengeFilter(null, null, null, handler, null, null, null);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String message = "foo";

        filter.onAuthenticationFailure(request, response, message);
        verify(handler).handle(request, response, SC_UNAUTHORIZED, message);
    }
}
