/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SecurityAuthConfigDeleteCommandTest {
    private BasicCruiseConfig cruiseConfig;
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldDeleteAProfile() throws Exception {
        SecurityAuthConfig authConfig = new SecurityAuthConfig("foo", "ldap");
        cruiseConfig.server().security().securityAuthConfigs().add(authConfig);

        SecurityAuthConfigDeleteCommand command = new SecurityAuthConfigDeleteCommand(null, authConfig, null, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().security().securityAuthConfigs(), is(empty()));
    }

    @Test
    public void shouldRaiseExceptionInCaseProfileDoesNotExist() throws Exception {
        SecurityAuthConfig authConfig = new SecurityAuthConfig("foo", "ldap");

        assertThat(cruiseConfig.server().security().securityAuthConfigs(), is(empty()));
        SecurityAuthConfigDeleteCommand command = new SecurityAuthConfigDeleteCommand(null, authConfig, null, null, new HttpLocalizedOperationResult());

        thrown.expect(RecordNotFoundException.class);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().security().securityAuthConfigs(), is(empty()));
    }

    @Test
    public void shouldNotValidateIfProfileIsInUseByRole() throws Exception {
        SecurityAuthConfig authConfig = new SecurityAuthConfig("foo", "ldap");
        cruiseConfig.server().security().addRole(new PluginRoleConfig("blackbird", "foo"));

        SecurityAuthConfigDeleteCommand command = new SecurityAuthConfigDeleteCommand(null, authConfig, null, null, new HttpLocalizedOperationResult());
        thrown.expect(GoConfigInvalidException.class);
        thrown.expectMessage("The security auth config 'foo' is being referenced by role(s): blackbird.");
        command.isValid(cruiseConfig);
    }

    @Test
    public void shouldValidateIfProfileIsNotInUseByPipeline() throws Exception {
        SecurityAuthConfig authConfig = new SecurityAuthConfig("foo", "ldap");

        assertThat(cruiseConfig.server().security().securityAuthConfigs(), is(empty()));
        SecurityAuthConfigDeleteCommand command = new SecurityAuthConfigDeleteCommand(null, authConfig, null, null, new HttpLocalizedOperationResult());
        assertTrue(command.isValid(cruiseConfig));
    }
}
