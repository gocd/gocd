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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class ClusterProfilesChangedPluginNotifierTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private ElasticAgentPluginRegistry registry;

    private ClusterProfilesChangedPluginNotifier notifier;
    private ClusterProfile newClusterProfile;
    private String pluginId;
    private ClusterProfile oldClusterProfile;
    private ArrayList<ConfigurationProperty> properties;

    @BeforeEach
    void setUp() {
        initMocks(this);
        pluginId = "plugin-id";
        when(goConfigService.getElasticConfig()).thenReturn(new ElasticConfig());
        properties = new ArrayList<>();
        properties.add(new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1")));
        oldClusterProfile = new ClusterProfile("profile1", pluginId, properties);
        newClusterProfile = new ClusterProfile("profile1", pluginId, properties);

        notifier = new ClusterProfilesChangedPluginNotifier(goConfigService, registry);
    }

    @Test
    void shouldNotifyPluginRegistryWhenANewClusterProfileIsCreated() {
        reset(goConfigService);
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.getClusterProfiles().add(newClusterProfile);
        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);
        notifier.onEntityConfigChange(newClusterProfile);
        verify(registry, times(1)).notifyPluginAboutClusterProfileChanged(pluginId, ClusterProfilesChangedStatus.CREATED, null, newClusterProfile.getConfigurationAsMap(true));
        verifyNoMoreInteractions(registry);
        verify(goConfigService, times(2)).getElasticConfig();
    }

    @Test
    void shouldNotifyPluginRegistryWhenAClusterProfileIsDeleted() {
        reset(goConfigService);
        when(goConfigService.getElasticConfig()).thenReturn(new ElasticConfig());
        notifier.onEntityConfigChange(oldClusterProfile);
        verify(registry, times(1)).notifyPluginAboutClusterProfileChanged(pluginId, ClusterProfilesChangedStatus.DELETED, oldClusterProfile.getConfigurationAsMap(true), null);
        verifyNoMoreInteractions(registry);
        verify(goConfigService, times(2)).getElasticConfig();
    }

    @Test
    void shouldNotifyPluginRegistryWhenAClusterProfileIsUpdated() {
        reset(goConfigService);
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.getClusterProfiles().add(oldClusterProfile);
        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);
        notifier = new ClusterProfilesChangedPluginNotifier(goConfigService, registry);

        notifier.onEntityConfigChange(newClusterProfile);
        verify(registry, times(1)).notifyPluginAboutClusterProfileChanged(pluginId, ClusterProfilesChangedStatus.UPDATED, oldClusterProfile.getConfigurationAsMap(true), newClusterProfile.getConfigurationAsMap(true));
        verifyNoMoreInteractions(registry);
        verify(goConfigService, times(3)).getElasticConfig();
    }

    @Test
    void shouldNotifyPluginRegistryWhenAClusterProfileIsUpdated_WithAChangeInPluginId() {
        String newPluginId = "updated-plugin-id";
        newClusterProfile = new ClusterProfile("profile1", newPluginId, properties);

        reset(goConfigService);
        ElasticConfig elasticConfig = new ElasticConfig();
        elasticConfig.getClusterProfiles().add(oldClusterProfile);
        when(goConfigService.getElasticConfig()).thenReturn(elasticConfig);
        notifier = new ClusterProfilesChangedPluginNotifier(goConfigService, registry);

        notifier.onEntityConfigChange(newClusterProfile);

        verify(registry, times(1)).notifyPluginAboutClusterProfileChanged(this.pluginId, ClusterProfilesChangedStatus.DELETED, oldClusterProfile.getConfigurationAsMap(true), null);
        verify(registry, times(1)).notifyPluginAboutClusterProfileChanged(newPluginId, ClusterProfilesChangedStatus.CREATED, null, newClusterProfile.getConfigurationAsMap(true));
        verifyNoMoreInteractions(registry);
        verify(goConfigService, times(3)).getElasticConfig();
    }

    @Test
    void onConfigChangeItShouldUpdateLocalClusterProfilesCopy() {
        BasicCruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.getElasticConfig().getClusterProfiles().add(newClusterProfile);
        newCruiseConfig.getElasticConfig().getClusterProfiles().add(oldClusterProfile);

        assertThat(notifier.getExistingClusterProfiles()).isEqualTo(new ClusterProfiles());
        notifier.onConfigChange(newCruiseConfig);
        assertThat(notifier.getExistingClusterProfiles()).isEqualTo(new ClusterProfiles(newClusterProfile, oldClusterProfile));
        verifyNoMoreInteractions(registry);
    }
}
