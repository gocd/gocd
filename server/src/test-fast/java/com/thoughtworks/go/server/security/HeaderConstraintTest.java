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
package com.thoughtworks.go.server.security;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HeaderConstraintTest {

    @Test
    public void shouldBeSatisfiedIfAPISafeModeIsTurnedOff() {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(systemEnvironment.isApiSafeModeEnabled()).thenReturn(false);
        request.addHeader("Confirm", "false");

        assertTrue(new HeaderConstraint(systemEnvironment).isSatisfied(request));
    }

    @Test
    public void shouldBeSatisfiedInPresenceOfRequiredHeader() {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(systemEnvironment.isApiSafeModeEnabled()).thenReturn(true);
        request.addHeader("Confirm", "True");

        assertTrue(new HeaderConstraint(systemEnvironment).isSatisfied(request));
    }

    @Test
    public void shouldBeSatisfiedInPresenceOfNewConfirmHeader() {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(systemEnvironment.isApiSafeModeEnabled()).thenReturn(true);
        request.addHeader("X-GoCD-Confirm", "True");

        assertTrue(new HeaderConstraint(systemEnvironment).isSatisfied(request));
    }

    @Test
    public void shouldBeUnsatisfiedIfRequiredHeadersAreAbsent() {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(systemEnvironment.isApiSafeModeEnabled()).thenReturn(true);

        assertFalse(new HeaderConstraint(systemEnvironment).isSatisfied(request));
    }
}