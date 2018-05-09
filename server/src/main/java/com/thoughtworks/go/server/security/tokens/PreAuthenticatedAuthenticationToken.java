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

package com.thoughtworks.go.server.security.tokens;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.AbstractAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;

import java.util.Map;

public class PreAuthenticatedAuthenticationToken extends AbstractAuthenticationToken {
    private final UserDetails principal;
    private final Map<String, String> credentials;
    private final String pluginId;

    public PreAuthenticatedAuthenticationToken(UserDetails principal, Map<String, String> credentials, String pluginId, GrantedAuthority[] authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.pluginId = pluginId;
    }

    public PreAuthenticatedAuthenticationToken(UserDetails principal, Map<String, String> credentials, String pluginId) {
        this(principal, credentials, pluginId, null);
    }

    @Override
    public Map<String, String> getCredentials() {
        return credentials;
    }

    @Override
    public UserDetails getPrincipal() {
        return principal;
    }

    public String getPluginId() {
        return pluginId;
    }
}
