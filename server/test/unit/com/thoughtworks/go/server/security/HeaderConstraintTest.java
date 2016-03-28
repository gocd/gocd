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
    public void shouldBeUnsatisfiedIfRequiredHeadersAreAbsent() {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(systemEnvironment.isApiSafeModeEnabled()).thenReturn(true);

        assertFalse(new HeaderConstraint(systemEnvironment).isSatisfied(request));
    }
}
