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
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentProfileCommandTest {
    private Username currentUser;
    private GoConfigService goConfigService;
    private BasicCruiseConfig cruiseConfig;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        currentUser = new Username("bob");
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticAgentProfileCreateCommand command = new ElasticAgentProfileCreateCommand(elasticProfile, goConfigService, currentUser, result);
        assertThat(cruiseConfig.server().getElasticConfig().getProfiles().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldValidateIfElasticProfileIdIsNull() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticProfile profile = new ElasticProfile(null, "some-plugin", new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        cruiseConfig.server().getElasticConfig().getProfiles().add(profile);
        ElasticAgentProfileCommand command = new ElasticAgentProfileCreateCommand(profile, goConfigService, currentUser, result);
        thrown.expectMessage("Elastic profile id cannot be null.");
        command.isValid(cruiseConfig);
    }


    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticAgentProfileCreateCommand command = new ElasticAgentProfileCreateCommand(elasticProfile, goConfigService, currentUser, result);
        assertThat(cruiseConfig.server().getElasticConfig().getProfiles().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsGroupAdmin() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("foo", "docker");

        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(true);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticAgentProfileCreateCommand command = new ElasticAgentProfileCreateCommand(elasticProfile, goConfigService, currentUser, result);
        assertThat(cruiseConfig.server().getElasticConfig().getProfiles().find("foo"), nullValue());

        assertThat(command.canContinue(cruiseConfig), is(true));
    }
}
