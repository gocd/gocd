/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

public class Username implements Serializable {
    public static final Username ANONYMOUS = new Username(new CaseInsensitiveString("anonymous"));
    public static final Username BLANK = new Username(new CaseInsensitiveString(""));
    public static final Username CRUISE_TIMER = new Username(new CaseInsensitiveString("timer"));

    private String displayName;
    private CaseInsensitiveString username;

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

    public String getDisplayName() {
        return displayName;
    }

    public CaseInsensitiveString getUsername() {
        return username;
    }

    public boolean hasDistinctDisplayName(){
        return !displayName.equals(username.toString());
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean isAnonymous() {
        return this.equals(ANONYMOUS);
    }

    public String appendNameToText(String text) {
        return text + CaseInsensitiveString.str(getUsername());
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
        if (displayName != null ? !displayName.equals(other.displayName) : other.displayName != null) {
            return false;
        }
        return !(username != null ? !username.equals(other.username) : other.username != null);

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
