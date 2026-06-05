/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;

public class Username implements Serializable {
    public static final Username ANONYMOUS = new Username(cis("anonymous"));
    public static final Username BLANK = new Username(cis(""));
    public static final Username CRUISE_TIMER = new Username(cis("timer"));

    private final CaseInsensitiveString username;
    private final String displayName;

    public Username(final CaseInsensitiveString userName) {
        this(userName, CaseInsensitiveString.str(userName));
    }

    public Username(final String userName) {
        this(cis(userName));
    }

    public Username(final Username userName, String displayName) {
        this(userName.getUsername(), displayName);
    }

    public Username(final CaseInsensitiveString userName, String displayName) {
        this.username = userName;
        this.displayName = displayName;
    }

    public Username(final String userName, String displayName) {
        this(cis(userName), displayName);
    }

    public String getDisplayName() {
        return displayName;
    }

    public CaseInsensitiveString getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Username.class.getSimpleName() + "[", "]")
            .add("username=" + username)
            .add("displayName='" + displayName + "'")
            .toString();
    }

    public boolean isAnonymous() {
        return this.equals(ANONYMOUS);
    }

    public boolean isGoAgentUser() {
        return this.username.startsWith("_go_agent_");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Username other = (Username) o;
        return Objects.equals(displayName, other.displayName) &&
            Objects.equals(username, other.username);

    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }

    public static Username valueOf(String username) {
        return new Username(cis(username));
    }
}
