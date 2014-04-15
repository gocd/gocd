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

package com.thoughtworks.go.server.security;

import java.io.IOException;
import java.util.Date;

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.AuthenticationServiceException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationProcessingFilterTest {

    private GoLicenseService goLicenseService;
    private TimeProvider timeProvider;
    private AuthenticationProcessingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpSession session;
    private com.thoughtworks.go.domain.User user;
    private Localizer localizer;

    @Before public void setUp() throws Exception {
        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);

        goLicenseService = mock(GoLicenseService.class);
        timeProvider = mock(TimeProvider.class);
        UserService userService = mock(UserService.class);
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);

        user = setCurrentUser("foo");
        when(userService.findUserByName("foo")).thenReturn(user);
        when(systemEnvironment.getLicenseExpiryWarningTime()).thenReturn(SystemEnvironment.NUMBER_OF_DAYS_TO_EXPIRY);

        localizer = mock(Localizer.class);
        filter = new AuthenticationProcessingFilter(mock(GoConfigService.class), goLicenseService, userService, timeProvider, systemEnvironment, localizer);
    }

    @Test
    public void shouldAddTheNumberOfDaysTheLicenseIsGoingToExpireOnTheSession() throws IOException {
        long now = System.currentTimeMillis();

        when(goLicenseService.getExpirationDate()).thenReturn(new Date(now + daysInMills(SystemEnvironment.NUMBER_OF_DAYS_TO_EXPIRY - 1)));
        when(timeProvider.currentTimeMillis()).thenReturn(now);

        filter.onSuccessfulAuthentication(request, null, null);

        assertThat((Long) session.getAttribute(AuthenticationProcessingFilter.LICENSE_EXPIRING_IN), is(SystemEnvironment.NUMBER_OF_DAYS_TO_EXPIRY - 1L));
    }

    private long daysInMills(final int numberOfDays) {
        return numberOfDays * 24L * 3600 * 1000;
    }

    @Test
    public void theNumberOfDaysToLicenseExpiry_ShouldBeFloorOfTimeInDaysLeft() throws IOException {
        long now = System.currentTimeMillis();

        when(goLicenseService.getExpirationDate()).thenReturn(new Date(now + daysInMills(SystemEnvironment.NUMBER_OF_DAYS_TO_EXPIRY)));
        when(timeProvider.currentTimeMillis()).thenReturn(now);

        filter.onSuccessfulAuthentication(request, null, null);

        assertThat((Long) session.getAttribute(AuthenticationProcessingFilter.LICENSE_EXPIRING_IN), is((long) SystemEnvironment.NUMBER_OF_DAYS_TO_EXPIRY));
    }

    @Test
    public void shouldNotAddNumberOfDaysToLicenseExpiry_WhenGreaterThanConfiguredDays() throws IOException {
        long now = System.currentTimeMillis();

        when(goLicenseService.getExpirationDate()).thenReturn(new Date(now + daysInMills(SystemEnvironment.NUMBER_OF_DAYS_TO_EXPIRY + 1)));
        when(timeProvider.currentTimeMillis()).thenReturn(now);

        filter.onSuccessfulAuthentication(request, null, null);

        assertThat(session.getAttribute(AuthenticationProcessingFilter.LICENSE_EXPIRING_IN), is(nullValue()));
    }

    @Test
    public void shouldNotAddNumberOfDaysToLicenseExpiryIfTheUserHasDismissedIt() throws IOException {
        long now = System.currentTimeMillis();
        user.disableLicenseExpiryWarning();
        when(goLicenseService.getExpirationDate()).thenReturn(new Date(now + daysInMills(SystemEnvironment.NUMBER_OF_DAYS_TO_EXPIRY)));
        when(timeProvider.currentTimeMillis()).thenReturn(now);

        filter.onSuccessfulAuthentication(request, null, null);

        assertThat(session.getAttribute(AuthenticationProcessingFilter.LICENSE_EXPIRING_IN), is(nullValue()));
    }

    @Test
    public void shouldSetSecurityExceptionMessageOnSessionWhenAuthenticationServiceExceptionIsThrownBySpring() throws Exception {
        when(localizer.localize("AUTHENTICATION_SERVICE_EXCEPTION")).thenReturn("some server error");
        filter.onUnsuccessfulAuthentication(request, null, new AuthenticationServiceException("foobar"));
        assertThat(((Exception) session.getAttribute(AuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY)).getMessage(), is("some server error"));
    }

    @Test
    public void shouldNotSetSecurityExceptionMessageOnSessionWhenBadCredentialsExceptionIsThrownBySpring() throws Exception {
        filter.onUnsuccessfulAuthentication(request, null, new BadCredentialsException("foobar"));
        assertThat(session.getAttribute(AuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY), is(nullValue()));
    }

    private com.thoughtworks.go.domain.User setCurrentUser(final String username) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(new User(username,"",true,true,true,true,new GrantedAuthority[]{}), username));
        return new com.thoughtworks.go.domain.User(username);
    }
}
