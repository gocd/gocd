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

package com.thoughtworks.go.server.security.userdetail;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.User;

/**
 * @understands a user principle in Go
 */
public class GoUserPrinciple extends User {
    private final String displayName;
    private String loginName;

    public GoUserPrinciple(String username, String displayName, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, GrantedAuthority[] authorities) throws IllegalArgumentException {
        this(username, displayName, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities, username);
    }

    public GoUserPrinciple(String username, String displayName, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, GrantedAuthority[] authorities,
                           String loginName) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.displayName = displayName;
        this.loginName = loginName;
    }

    public GoUserPrinciple(String username, String displayName, String password, GrantedAuthority[] authorities, String loginName) {
        super(username, password, true, true, true, true, authorities);
        this.displayName = displayName;
        this.loginName = loginName;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
