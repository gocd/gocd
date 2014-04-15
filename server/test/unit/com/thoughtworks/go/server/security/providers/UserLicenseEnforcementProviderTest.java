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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.UserLicenseLimitExceededException;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.AuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserLicenseEnforcementProviderTest {
    private UserService userService;
    private UserLicenseEnforcementProvider enforcementProvider;
    private UsernamePasswordAuthenticationToken auth;
    private Authentication resultantAuthorization;
    private AuthenticationProvider underlyingProvider;
    private GoConfigService goConfigService;

    @Before public void setUp() throws Exception {
        userService = mock(UserService.class);
        underlyingProvider = mock(AuthenticationProvider.class);
        goConfigService = mock(GoConfigService.class);
        enforcementProvider = new UserLicenseEnforcementProvider(userService, goConfigService, underlyingProvider);
        auth = new UsernamePasswordAuthenticationToken(new User("user", "pass", true, true, true, true, new GrantedAuthority[] {}), "credentials");
        resultantAuthorization = new UsernamePasswordAuthenticationToken(new User("user-authenticated", "pass", true, true, true, true,
                new GrantedAuthority[]{GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority()}), "credentials");
        when(underlyingProvider.authenticate(auth)).thenReturn(resultantAuthorization);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(userService);
    }

    @Test
    public void shouldEnforceLicenseLimit() throws Exception {
        Authentication authentication = enforcementProvider.authenticate(auth);
        assertThat(authentication, is(resultantAuthorization));
        verify(userService).addUserIfDoesNotExist(UserHelper.getUserName(resultantAuthorization));
    }

    @Test
    public void shouldNotCallFilterChainOnLicenseLimitException() throws Exception {
        UserLicenseLimitExceededException actualException = new UserLicenseLimitExceededException("exceeded");
        doThrow(actualException).when(userService).addUserIfDoesNotExist(UserHelper.getUserName(resultantAuthorization));
        try {
            enforcementProvider.authenticate(auth);
            fail("should have thrown UserLicenseLimitExceededException");
        } catch (UserLicenseLimitExceededException expected) {
            assertThat(expected, sameInstance(actualException));
        }
        verify(userService).addUserIfDoesNotExist(UserHelper.getUserName(resultantAuthorization));
    }
    
    @Test
    public void shouldNotFailWhenUnderlyingProviderDoesNotAuthenticate() throws Exception {
        when(underlyingProvider.authenticate(auth)).thenReturn(null);
        Authentication authentication = enforcementProvider.authenticate(auth);
        assertThat(authentication, is(nullValue()));
    }
}
