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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class NaiveUserDetailsService implements UserDetailsService{

    private final AuthorityGranter authorityGranter;
    private UserService userService;

    @Autowired
    public NaiveUserDetailsService(AuthorityGranter authorityGranter, UserService userService) {
        this.authorityGranter = authorityGranter;
        this.userService = userService;
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        final User user = userService.findUserByName(username);
        if (StringUtil.isBlank(user.getName())) {
            return new GoUserPrinciple(user.getName(), user.getDisplayName(), "", false, false, false, false, new GrantedAuthority[]{});
        } else {
            return new GoUserPrinciple(user.getName(), user.getDisplayName(), "", true, true, true, true, authorityGranter.authorities(username));
        }
    }
}
