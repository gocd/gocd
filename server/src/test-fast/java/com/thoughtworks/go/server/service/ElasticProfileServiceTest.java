/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.update.ElasticAgentProfileCreateCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileDeleteCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileUpdateCommand;
import com.thoughtworks.go.plugin.access.PluginNotFoundException;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ElasticProfileServiceTest {
    private GoConfigService goConfigService;
    private ElasticProfileService elasticProfileService;
    private ElasticAgentExtension elasticAgentExtension;

    @Before
    public void setUp() throws Exception {
        elasticAgentExtension = mock(ElasticAgentExtension.class);
        EntityHashingService hashingService = mock(EntityHashingService.class);
        goConfigService = mock(GoConfigService.class);
        elasticProfileService = new ElasticProfileService(goConfigService, hashingService, elasticAgentExtension);
    }

    @Test
    public void shouldReturnAnEmptyMapIfNoElasticProfiles() throws Exception {
        ElasticConfig elasticConfig = new ElasticConfig();

        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

        assertThat(elasticProfileService.listAll().isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAMapOfElasticProfiles() {
        ElasticConfig elasticConfig = new ElasticConfig();
        ElasticProfile elasticProfile = new ElasticProfile("ecs", "cd.go.elatic.ecs");
        elasticConfig.getProfiles().add(elasticProfile);

        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

        HashMap<String, ElasticProfile> expectedMap = new HashMap<>();
        expectedMap.put("ecs", elasticProfile);
        Map<String, ElasticProfile> elasticProfiles = elasticProfileService.listAll();
        assertThat(elasticProfiles.size(), is(1));
        assertThat(elasticProfiles, is(expectedMap));
    }

    @Test
    public void shouldReturnNullWhenProfileWithGivenIdDoesNotExist() throws Exception {
        when(goConfigService.getElasticConfig()).thenReturn(new ElasticConfig());

        assertNull(elasticProfileService.findProfile("non-existent-id"));
    }

    @Test
    public void shouldReturnElasticProfileWithGivenIdWhenPresent() throws Exception {
        ElasticConfig elasticConfig = new ElasticConfig();
        ElasticProfile elasticProfile = new ElasticProfile("ecs", "cd.go.elatic.ecs");
        elasticConfig.getProfiles().add(elasticProfile);

        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);

        assertThat(elasticProfileService.findProfile("ecs"), is(elasticProfile));
    }

    @Test
    public void shouldAddElasticProfileToConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap");

        Username username = new Username("username");
        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileCreateCommand.class), eq(username));
    }

    @Test
    public void shouldPerformPluginValidationsBeforeAddingElasticProfile() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap", create("key", false, "value"));

        Username username = new Username("username");
        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(elasticAgentExtension).validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true));
    }

    @Test
    public void shouldAddPluginNotFoundErrorOnConfigForANonExistentPluginIdWhileCreating() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("some-id", "non-existent-plugin", create("key", false, "value"));

        Username username = new Username("username");
        when(elasticAgentExtension.validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true))).thenThrow(new PluginNotFoundException("some error"));

        elasticProfileService.create(username, elasticProfile, new HttpLocalizedOperationResult());

        assertThat(elasticProfile.errors().isEmpty(), Matchers.is(false));
        assertThat(elasticProfile.errors().on("pluginId"), Matchers.is("Plugin with id `non-existent-plugin` is not found."));
    }

    @Test
    public void shouldUpdateExistingElasticProfileInConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap");

        Username username = new Username("username");
        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileUpdateCommand.class), eq(username));
    }

    @Test
    public void shouldPerformPluginValidationsBeforeUpdatingElasticProfile() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap", create("key", false, "value"));

        Username username = new Username("username");
        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        verify(elasticAgentExtension).validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true));
    }

    @Test
    public void shouldAddPluginNotFoundErrorOnConfigForANonExistentPluginIdWhileUpdating() throws Exception {
        ElasticProfile elasticProfile = new ElasticProfile("some-id", "non-existent-plugin", create("key", false, "value"));

        Username username = new Username("username");
        when(elasticAgentExtension.validate(elasticProfile.getPluginId(), elasticProfile.getConfigurationAsMap(true))).thenThrow(new PluginNotFoundException("some error"));

        elasticProfileService.update(username, "md5", elasticProfile, new HttpLocalizedOperationResult());

        assertThat(elasticProfile.errors().isEmpty(), Matchers.is(false));
        assertThat(elasticProfile.errors().on("pluginId"), Matchers.is("Plugin with id `non-existent-plugin` is not found."));
    }

    @Test
    public void shouldDeleteExistingElasticProfileInConfig() {
        ElasticProfile elasticProfile = new ElasticProfile("ldap", "cd.go.ldap");

        Username username = new Username("username");
        elasticProfileService.delete(username, elasticProfile, new HttpLocalizedOperationResult());

        verify(goConfigService).updateConfig(any(ElasticAgentProfileDeleteCommand.class), eq(username));
    }
}

