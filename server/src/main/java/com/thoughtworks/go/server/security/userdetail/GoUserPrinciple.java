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
package com.thoughtworks.go.server.security.userdetail;

import com.google.common.collect.Sets;
import com.thoughtworks.go.server.domain.Username;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public class GoUserPrinciple {
    private final Set<GrantedAuthority> authorities;

    private final String displayName;
    private final Username username;

    public GoUserPrinciple(String username, String displayName, GrantedAuthority... authorities) {
        this(username, displayName, Sets.newHashSet(authorities));
    }

    public GoUserPrinciple(String username, String displayName, Set<GrantedAuthority> authorities) {
        this.username = new Username(username, displayName);
        this.authorities = authorities;
        this.displayName = displayName;
    }

    public String getUsername() {
        return username.getUsername().toString();
    }

    public Username asUsernameObject() {
        return username;
    }

    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoUserPrinciple)) return false;

        GoUserPrinciple that = (GoUserPrinciple) o;

        if (authorities != null ? !authorities.equals(that.authorities) : that.authorities != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        return username != null ? username.equals(that.username) : that.username == null;
    }

    @Override
    public int hashCode() {
        int result = authorities != null ? authorities.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }
}
