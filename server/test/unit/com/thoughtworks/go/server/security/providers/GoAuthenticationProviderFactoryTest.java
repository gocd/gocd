/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class GoAuthenticationProviderFactoryTest {
    private UserService userService;
    private GoAuthenticationProviderFactory factory;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        factory = new GoAuthenticationProviderFactory(userService);
    }

    @Test
    public void shouldCreateLicenseEnforcementProviderWithUserServicePassedIn() throws Exception {
        GoAuthenticationProvider licenseEnforcementProvider = (GoAuthenticationProvider) factory.getObject();
        AuthenticationProvider underlyingProvider = mock(AuthenticationProvider.class);
        licenseEnforcementProvider.setProvider(underlyingProvider);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("foo", "bar");
        UsernamePasswordAuthenticationToken resultantAuthorization = new UsernamePasswordAuthenticationToken(
                new User("foo-user", "pass", true, true, true, true, Collections.singletonList(GoAuthority.ROLE_USER.asAuthority())), "bar");
        when(underlyingProvider.authenticate(auth)).thenReturn(resultantAuthorization);
        licenseEnforcementProvider.authenticate(auth);
        verify(userService).addUserIfDoesNotExist(UserHelper.getUser(resultantAuthorization));
    }

    @Test
    public void shouldReturnUserLicenseEnforcementClass() throws Exception {
        assertTrue(factory.getObjectType() == GoAuthenticationProvider.class);
    }

    @Test
    public void shouldCreateNewInstancesEveryTime() throws Exception {
        assertFalse(factory.isSingleton());
    }
}
