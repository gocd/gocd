/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.providers.AuthenticationProvider;

/**
 * @understands Creates user records in db on successful authentication
 */
public class GoAuthenticationProvider implements AuthenticationProvider {
    private final UserService userService;
    private AuthenticationProvider provider;

    public GoAuthenticationProvider(UserService userService, AuthenticationProvider provider) {
        this.userService = userService;
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

    public GoAuthenticationProvider setProvider(AuthenticationProvider provider) {
        this.provider = provider;
        return this;
    }
}
