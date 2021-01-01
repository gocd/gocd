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
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

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
        cruiseConfig.getElasticConfig().getProfiles().clear();
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(null, new ElasticProfile("foo", "prod-cluster"), null, null, new HttpLocalizedOperationResult(), null, null);
        thrown.expect(RecordNotFoundException.class);
        command.update(cruiseConfig);
    }

    @Test
    public void shouldUpdateExistingProfile() throws Exception {
        ElasticProfile oldProfile = new ElasticProfile("foo", "prod-cluster");
        ElasticProfile newProfile = new ElasticProfile("foo", "prod-cluster");

        cruiseConfig.getElasticConfig().getProfiles().add(oldProfile);
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(null, newProfile, null, null, null, null, null);
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getElasticConfig().getProfiles().find("foo"), is(equalTo(newProfile)));
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfRequestIsNotFresh() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);

        ElasticProfile oldProfile = new ElasticProfile("foo", "prod-cluster");
        ElasticProfile newProfile = new ElasticProfile("foo", "prod-cluster");

        cruiseConfig.getElasticConfig().getProfiles().add(oldProfile);

        EntityHashingService entityHashingService = mock(EntityHashingService.class);

        when(entityHashingService.hashForEntity(oldProfile)).thenReturn("digest");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(goConfigService, newProfile, null, currentUser, result, entityHashingService, "bad-digest");

        assertThat(command.canContinue(cruiseConfig), is(false));
        assertThat(result.toString(), containsString("Someone has modified the configuration for"));
    }

    @Test
    public void shouldEncryptSecurePluginProperties() {
        ElasticProfile elasticProfile = mock(ElasticProfile.class);
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(null, elasticProfile, null, null, null, null, null);

        BasicCruiseConfig preProcessedConfig = new BasicCruiseConfig();
        command.encrypt(preProcessedConfig);

        verify(elasticProfile, times(1)).encryptSecureProperties(preProcessedConfig);
    }
}
