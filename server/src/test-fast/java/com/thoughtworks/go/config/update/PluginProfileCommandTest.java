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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginProfileCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;

    @BeforeEach
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginProfileCommand command = new StubSecurityAuthConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.message(), equalTo("Unauthorized to edit."));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAuthorized() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginProfileCommand command = new StubSecurityAuthConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
        assertThat(result.httpCode(), is(200));
    }

    @Test
    public void shouldValidateIfSecurityAuthConfigIdIsNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig(null, "some-plugin", new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        cruiseConfig.server().security().securityAuthConfigs().add(securityAuthConfig);

        PluginProfileCommand command = new StubSecurityAuthConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .hasMessageContaining(EntityType.ElasticProfile.idCannotBeBlank());
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() throws Exception {
        SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.ldap");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        PluginProfileCommand command = new StubSecurityAuthConfigCommand(goConfigService, securityAuthConfig, currentUser, result);
        assertThat(cruiseConfig.server().security().securityAuthConfigs().find("ldap"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    private class StubSecurityAuthConfigCommand extends PluginProfileCommand<SecurityAuthConfig, SecurityAuthConfigs> {

        public StubSecurityAuthConfigCommand(GoConfigService goConfigService, SecurityAuthConfig profile, Username currentUser, LocalizedOperationResult result) {
            super(goConfigService, profile, currentUser, result);
        }

        @Override
        public void update(CruiseConfig preprocessedConfig) throws Exception {

        }

        @Override
        public boolean isValid(CruiseConfig preprocessedConfig) {
            return isValidForCreateOrUpdate(preprocessedConfig);
        }

        @Override
        protected SecurityAuthConfigs getPluginProfiles(CruiseConfig preprocessedConfig) {
            return null;
        }

        @Override
        public ValidationResult validateUsingExtension(String pluginId, Map<String, String> configuration) {
            return null;
        }

        @Override
        protected EntityType getObjectDescriptor() {
            return EntityType.ElasticProfile;
        }

        @Override
        protected final boolean isAuthorized() {
            if (goConfigService.isUserAdmin(currentUser)) {
                return true;
            }
            result.forbidden(forbiddenToEdit(), forbidden());
            return false;
        }
    }
}
