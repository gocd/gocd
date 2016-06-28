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

package com.thoughtworks.go.domain.cctray.viewers;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

/* Understands: The set of viewers it has. */
public class AllowedViewers implements Viewers {
    private Set<String> allowedUsers = new HashSet<>();

    public AllowedViewers(Set<String> allowedUsers) {
        for (String user : allowedUsers) {
            this.allowedUsers.add(user.toLowerCase());
        }
    }

    @Override
    public boolean contains(String username) {
        return allowedUsers.contains(username.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AllowedViewers that = (AllowedViewers) o;

        return allowedUsers.equals(that.allowedUsers);
    }

    @Override
    public int hashCode() {
        return allowedUsers.hashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
