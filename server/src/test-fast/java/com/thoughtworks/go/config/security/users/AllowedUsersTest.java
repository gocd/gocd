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
package com.thoughtworks.go.config.security.users;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.PluginRoleUsersStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AllowedUsersTest {
    private PluginRoleUsersStore pluginRoleUsersStore;

    @Before
    public void setUp() throws Exception {
        pluginRoleUsersStore = PluginRoleUsersStore.instance();
    }

    @After
    public void tearDown() throws Exception {
        pluginRoleUsersStore.clearAll();
    }

    @Test
    public void shouldCheckViewPermissionsInACaseInsensitiveWay() throws Exception {
        AllowedUsers users = new AllowedUsers(s("USER1", "user2", "User3", "AnoTherUsEr"), Collections.emptySet());

        assertThat(users.contains("user1"), is(true));
        assertThat(users.contains("USER1"), is(true));
        assertThat(users.contains("User1"), is(true));
        assertThat(users.contains("USER2"), is(true));
        assertThat(users.contains("uSEr3"), is(true));
        assertThat(users.contains("anotheruser"), is(true));
        assertThat(users.contains("NON-EXISTENT-USER"), is(false));
    }

    @Test
    public void usersShouldHaveViewPermissionIfTheyBelongToAllowedPluginRoles() throws Exception {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");

        pluginRoleUsersStore.assignRole("foo", admin);

        AllowedUsers users = new AllowedUsers(Collections.emptySet(), Collections.singleton(admin));

        assertTrue(users.contains("FOO"));
        assertTrue(users.contains("foo"));
        assertFalse(users.contains("bar"));
    }
}
