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
package com.thoughtworks.go.server.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.thoughtworks.go.remote.StandardHeaders.REQUEST_CONFIRM_MODIFICATION;
import static com.thoughtworks.go.remote.StandardHeaders.REQUEST_CONFIRM_MODIFICATION_DEPRECATED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfirmationConstraintTest {

    @Test
    public void shouldBeSatisfiedInPresenceOfDeprecatedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader(REQUEST_CONFIRM_MODIFICATION_DEPRECATED, "True");

        assertTrue(new ConfirmationConstraint().isSatisfied(request));
    }

    @Test
    public void shouldBeSatisfiedInPresenceOfNewConfirmHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader(REQUEST_CONFIRM_MODIFICATION, "True");

        assertTrue(new ConfirmationConstraint().isSatisfied(request));
    }

    @Test
    public void shouldBeUnsatisfiedIfRequiredHeadersAreAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertFalse(new ConfirmationConstraint().isSatisfied(request));
    }
}