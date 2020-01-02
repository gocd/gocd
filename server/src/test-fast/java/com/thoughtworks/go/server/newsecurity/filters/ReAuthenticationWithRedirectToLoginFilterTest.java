/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.savedrequest.SavedRequest;

import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReAuthenticationWithRedirectToLoginFilterTest {
    @Test
    void shouldInvokeHandler() throws IOException {
        final ReAuthenticationWithRedirectToLoginFilter filter = new ReAuthenticationWithRedirectToLoginFilter(null, null, null, null, null, null);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String message = "foo";
        SavedRequest savedRequest = mock(SavedRequest.class);

        SessionUtils.saveRequest(request, savedRequest);
        HttpSession originalSession = request.getSession(true);

        filter.onAuthenticationFailure(request, response, message);

        assertThat(SessionUtils.getAuthenticationError(request)).isEqualTo("foo");
        assertThat(request.getSession(false)).isNotSameAs(originalSession);
        assertThat(SessionUtils.savedRequest(request)).isSameAs(savedRequest);
        assertThat(SessionUtils.hasAuthenticationToken(request)).isFalse();

        MockHttpServletResponseAssert.assertThat(response)
                .redirectsTo("/go/auth/login");
    }
}
