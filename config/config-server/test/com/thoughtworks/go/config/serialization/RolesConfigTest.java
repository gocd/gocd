/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RolesConfigTest {
    CruiseConfig config;
    @Before
    public void setUp() throws Exception {
        config = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).loadConfigHolder(
                ConfigFileFixture.CONFIG).configForEdit;
    }

    @Test
    public void shouldSupportRoleWithNoUsers() throws Exception {
        addRole(role("test_role"));
    }

    private void addRole(Role role) {
        config.server().security().addRole(role);
        try {
            new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).write(config, new ByteArrayOutputStream(), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldNotSupportMultipleRolesWithTheSameName() throws Exception {
        addRole(role("test_role"));
        try {
            addRole(role("test_role"));
            fail("Role already exists");
        } catch (Exception expected) {
            assertThat(expected.getCause().getMessage(), is("Role names should be unique. Duplicate names found."));
        }
    }

    @Test
    public void shouldNotSupportRoleWithTheMultipleUsersThatAreTheSame() throws Exception {
        try {
            addRole(role("test_role", user("chris"), user("chris")));
            fail("User already exists");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), is("User 'chris' already exists in 'test_role'."));
        }
    }

    private RoleUser user(String name) {
        return new RoleUser(new CaseInsensitiveString(name));
    }

    private Role role(String name, RoleUser... users) {
        return new Role(new CaseInsensitiveString(name), users);
    }
}
