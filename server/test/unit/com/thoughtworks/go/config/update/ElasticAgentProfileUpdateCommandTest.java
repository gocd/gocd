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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ElasticProfileNotFoundException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentProfileUpdateCommandTest {
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
    public void shouldRaiseErrorWhenUpdatingNonExistentProfile() throws Exception {
        cruiseConfig.server().getElasticConfig().getProfiles().clear();
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(null, null, new ElasticProfile("foo", "docker"), null, new HttpLocalizedOperationResult(), null);
        thrown.expect(ElasticProfileNotFoundException.class);
        command.update(cruiseConfig);
    }

    @Test
    public void shouldUpdateExistingProfile() throws Exception {
        ElasticProfile oldProfile = new ElasticProfile("foo", "docker");
        ElasticProfile newProfile = new ElasticProfile("foo", "aws");

        cruiseConfig.server().getElasticConfig().getProfiles().add(oldProfile);
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(null, null, newProfile, null, null, null);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.server().getElasticConfig().getProfiles().find("foo"), is(equalTo(newProfile)));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);

        ElasticProfile oldProfile = new ElasticProfile("foo", "docker");
        ElasticProfile newProfile = new ElasticProfile("foo", "aws");

        cruiseConfig.server().getElasticConfig().getProfiles().add(oldProfile);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(goConfigService, null, newProfile, null, result, currentUser);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        ElasticProfile oldProfile = new ElasticProfile("foo", "docker");
        ElasticProfile newProfile = new ElasticProfile("foo", "aws");

        cruiseConfig.server().getElasticConfig().getProfiles().add(oldProfile);

        EntityHashingService entityHashingService = mock(EntityHashingService.class);

        when(entityHashingService.md5ForEntity(oldProfile)).thenReturn("md5");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(goConfigService, entityHashingService, newProfile, "bad-md5", result, currentUser);

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("STALE_RESOURCE_CONFIG"));
    }
}
