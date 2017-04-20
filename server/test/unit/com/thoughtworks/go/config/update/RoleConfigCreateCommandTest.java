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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.RoleNotFoundException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoleConfigCreateCommandTest {
    private AuthorizationExtension extension;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        extension = mock(AuthorizationExtension.class);
    }

    @Test
    public void shouldAddPluginRoleConfig() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        RoleConfigCreateCommand command = new RoleConfigCreateCommand(null, role, extension, null, null);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.server().security().getRoles().findByName(new CaseInsensitiveString("blackbird")), equalTo(role));
    }

    @Test
    public void shouldInvokePluginValidationsBeforeSave() throws Exception {
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key", "error"));
        when(extension.validateRoleConfiguration(eq("aws"), Matchers.any())).thenReturn(validationResult);
        PluginRoleConfig role = new PluginRoleConfig("blackbird", "ldap");
        RoleConfigCreateCommand command = new RoleConfigCreateCommand(mock(GoConfigService.class), role, extension, null, new HttpLocalizedOperationResult());
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();

        thrown.expect(RoleNotFoundException.class);
        thrown.expectMessage("Role `blackbird` does not exist.");
        command.isValid(cruiseConfig);
        command.update(cruiseConfig);
        assertThat(role.first().errors().size(), is(1));
        assertThat(role.first().errors().asString(), is("error"));
    }
}