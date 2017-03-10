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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleLogEndpointConfiguratorTest {

    private ConsoleLogEndpointConfigurator configurator;
    private Authentication authentication;
    private User user;
    private SecurityService service;
    private UserService userService;

    @Before
    public void setup() {
        configurator = new ConsoleLogEndpointConfigurator();
        authentication = mock(Authentication.class);

        user = mock(User.class);
        service = mock(SecurityService.class);
        userService = mock(UserService.class);
    }

    @Test
    public void isAuthenticated() throws Exception {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);

        assertFalse("Should deny if there is no security context in the session", configurator.isAuthenticated(null));

        when(authentication.isAuthenticated()).thenReturn(false);
        assertFalse("Should deny if user is not authenticated", configurator.isAuthenticated(context));

        when(authentication.isAuthenticated()).thenReturn(true);
        assertTrue("Should allow if user is authenticated", configurator.isAuthenticated(context));
    }

    @Test
    public void isAuthorizedForPipeline() throws Exception {
        setupMocksForAuthorizationOnPipeline("whatever");

        assertTrue("Allows when user is enabled, has the ROLE_USER authority, and has view access to the pipeline group", checkAuthorization("whatever"));

        when(service.hasViewPermissionForPipeline(user.getUsername(), "whatever")).thenReturn(false);
        assertFalse("Should deny if user is not granted view access to the pipeline group", checkAuthorization("whatever"));

        when(service.hasViewPermissionForPipeline(user.getUsername(), "whatever")).thenReturn(true);
        when(user.isEnabled()).thenReturn(false);
        assertFalse("Should deny if user is disabled", checkAuthorization("whatever"));

        assertFalse("Should deny if pipelineName is null", checkAuthorization(null));
        assertFalse("Should deny if pipelineName is empty string", checkAuthorization(""));

        when(authentication.getAuthorities()).thenReturn(new GrantedAuthority[]{GoAuthority.ROLE_ANONYMOUS.asAuthority()});
        assertFalse("Should deny if user does not have ROLE_USER authority", checkAuthorization("sam"));
    }

    private boolean checkAuthorization(String pipeline) {
        return configurator.isAuthorizedForPipeline(pipeline, authentication, userService, service);
    }

    private void setupMocksForAuthorizationOnPipeline(String pipelineName) {
        when(user.getUsername()).thenReturn(new Username("kyleizkool"));

        GoUserPrinciple kyle = mock(GoUserPrinciple.class);
        when(kyle.getDisplayName()).thenReturn("Kyle Olivo");
        when(kyle.getUsername()).thenReturn("kyleizkool");
        when(authentication.getPrincipal()).thenReturn(kyle);

        when(userService.findUserByName("kyleizkool")).thenReturn(user);

        when(authentication.getAuthorities()).thenReturn(new GrantedAuthority[]{GoAuthority.ROLE_USER.asAuthority()});
        when(user.isEnabled()).thenReturn(true);
        when(service.hasViewPermissionForPipeline(user.getUsername(), pipelineName)).thenReturn(true);
    }

}