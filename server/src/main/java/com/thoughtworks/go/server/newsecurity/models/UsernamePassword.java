/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.models;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;

public class UsernamePassword implements Credentials {
    private final String username;
    private final String password;

    public UsernamePassword(String username, String password) throws BadCredentialsException {
        this.username = username;
        this.password = password;
        if (!StringUtils.hasLength(username)) {
            throw new BadCredentialsException("Username cannot be blank!");
        }
        if (!StringUtils.hasLength(password)) {
            throw new BadCredentialsException("Password cannot be blank!");
        }
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsernamePassword)) return false;

        UsernamePassword that = (UsernamePassword) o;

        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return password != null ? password.equals(that.password) : that.password == null;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}
