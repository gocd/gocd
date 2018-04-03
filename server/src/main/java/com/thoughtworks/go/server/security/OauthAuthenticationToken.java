/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class OauthAuthenticationToken extends AbstractAuthenticationToken {
    private final String token;
    private UserDetails userDetails;

    public OauthAuthenticationToken(String token) {
        super(null);
        this.token = token;
    }

    public OauthAuthenticationToken(UserDetails user) {
        super(user.getAuthorities());
        userDetails = user;
        token = user.getPassword();
        setAuthenticated(true);
    }

    public String getCredentials() {
        return token;
    }

    public UserDetails getPrincipal() {
        return userDetails;
    }
}
