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

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.event.authentication.AbstractAuthenticationFailureEvent;
import org.springframework.security.providers.AuthenticationProvider;

/**
 * @understands enforcing licence limit on number of logged in user
 */
public class UserLicenseEnforcementProvider implements AuthenticationProvider {
    class LicenceUserLimitExceededEvent extends AbstractAuthenticationFailureEvent {
        public LicenceUserLimitExceededEvent(Authentication authentication, AuthenticationException exception) {
            super(authentication, exception);
        }
    }

    private final UserService userService;
    private GoConfigService goConfigService;
    private AuthenticationProvider provider;

    public UserLicenseEnforcementProvider(UserService userService, GoConfigService goConfigService, AuthenticationProvider provider) {
        this.userService = userService;
        this.goConfigService = goConfigService;
        this.provider = provider;
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication auth = provider.authenticate(authentication);
        if (auth != null) {
            userService.addUserIfDoesNotExist(UserHelper.getUserName(auth));
        }
        return auth;
    }

    public boolean supports(Class authentication) {
        return provider.supports(authentication);
    }

    public UserLicenseEnforcementProvider setProvider(AuthenticationProvider provider) {
        this.provider = provider;
        return this;
    }
}
