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

package com.thoughtworks.go.config;

import com.thoughtworks.go.ClearSingleton;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigModifyingUserTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Test
    public void shouldIdentifyPassedInUserNameAsConfigModifyingUser() {
        ConfigModifyingUser user = new ConfigModifyingUser("loser_boozer");
        assertThat(user.getUserName(), is("loser_boozer"));
    }

    @Test
    public void shouldIdentifyLoggedInUserAsModifyingUser_WhenNoModifyingUserIsGiven() {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new User("loser_boozer", "pass", true, true, true, true, new GrantedAuthority[]{}), null));
        ConfigModifyingUser user = new ConfigModifyingUser();
        assertThat(user.getUserName(), is("loser_boozer"));
    }
}
