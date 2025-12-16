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
package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RolesConfigSerializationTest {
    CruiseConfig config;

    @BeforeEach
    public void setUp() throws Exception {
        config = new MagicalGoConfigXmlLoader(ConfigElementImplementationRegistryMother.withNoPlugins()).loadConfigHolder(
                ConfigFileFixture.CONFIG).configForEdit;
    }

    @Test
    public void shouldSupportRoleWithNoUsers() {
        addRole(role("test_role"));
    }

    private void addRole(Role role) {
        config.server().security().addRole(role);
        try {
            new MagicalGoConfigXmlWriter(ConfigElementImplementationRegistryMother.withNoPlugins()).write(config, new ByteArrayOutputStream(), false);
        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldNotSupportMultipleRolesWithTheSameName() {
        addRole(role("test_role"));
        assertThatThrownBy(() -> addRole(role("test_role")))
            .isInstanceOf(GoConfigInvalidException.class)
            .hasMessage("Role names should be unique. Duplicate names found.");
    }

    @Test
    public void shouldNotSupportRoleWithTheMultipleUsersThatAreTheSame() {
        Role role = role("test_role", user("chris"), user("chris"), user("bob"), user("john"));
        assertThat(role.getUsers()).hasSize(3);
    }

    private RoleUser user(String name) {
        return new RoleUser(new CaseInsensitiveString(name));
    }

    private Role role(String name, RoleUser... users) {
        return new RoleConfig(new CaseInsensitiveString(name), users);
    }
}
