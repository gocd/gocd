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
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Objects;

public class Username implements Serializable {
    public static final Username ANONYMOUS = new Username(new CaseInsensitiveString("anonymous"));
    public static final Username BLANK = new Username(new CaseInsensitiveString(""));
    public static final Username CRUISE_TIMER = new Username(new CaseInsensitiveString("timer"));

    private final CaseInsensitiveString username;
    private final String displayName;

    public Username(final CaseInsensitiveString userName) {
        this(userName, CaseInsensitiveString.str(userName));
    }

    public Username(final String userName) {
        this(new CaseInsensitiveString(userName));
    }

    public Username(final Username userName, String displayName) {
        this(userName.getUsername(), displayName);
    }

    public Username(final CaseInsensitiveString userName, String displayName) {
        this.username = userName;
        this.displayName = displayName;
    }

    public Username(final String userName, String displayName) {
        this(new CaseInsensitiveString(userName), displayName);
    }

    public String getDisplayName() {
        return displayName;
    }

    public CaseInsensitiveString getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean isAnonymous() {
        return this.equals(ANONYMOUS);
    }

    public boolean isGoAgentUser() {
        return this.username.toLower().startsWith("_go_agent_");
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
        if (!Objects.equals(displayName, other.displayName)) {
            return false;
        }
        return Objects.equals(username, other.username);

    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }

    public static Username valueOf(String username) {
        return new Username(new CaseInsensitiveString(username));
    }
}
