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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClusterProfilesChangedPluginNotifier extends EntityConfigChangedListener<ClusterProfile> implements ConfigChangedListener {
    private ClusterProfiles existingClusterProfiles;
    private ElasticAgentPluginRegistry registry;
    private GoConfigService goConfigService;

    @Autowired
    public ClusterProfilesChangedPluginNotifier(GoConfigService goConfigService, ElasticAgentPluginRegistry registry) {
        this.goConfigService = goConfigService;
        this.existingClusterProfiles = goConfigService.getElasticConfig().getClusterProfiles();
        this.registry = registry;
        goConfigService.register(this);
    }

    @Override
    public void onEntityConfigChange(ClusterProfile updatedClusterProfile) {
        if (goConfigService.getElasticConfig().getClusterProfiles().find(updatedClusterProfile.getId()) == null) {
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.DELETED, updatedClusterProfile.getConfigurationAsMap(true), null);
            updateClusterProfilesCopy();
            return;
        }

        ClusterProfile oldClusterProfile = existingClusterProfiles.find(updatedClusterProfile.getId());
        if (oldClusterProfile == null) {
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.CREATED, null, updatedClusterProfile.getConfigurationAsMap(true));
            updateClusterProfilesCopy();
            return;
        }

        //cluster profile has been updated without changing plugin id
        if (oldClusterProfile.getPluginId().equals(updatedClusterProfile.getPluginId())) {
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.UPDATED, oldClusterProfile.getConfigurationAsMap(true), updatedClusterProfile.getConfigurationAsMap(true));
            updateClusterProfilesCopy();
        } else {
            //cluster profile has been updated including changing plugin id.
            //this internally results in deletion of a profile belonging to old plugin id and creation of the profile belonging to new plugin id
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.CREATED, null, updatedClusterProfile.getConfigurationAsMap(true));
            registry.notifyPluginAboutClusterProfileChanged(oldClusterProfile.getPluginId(), ClusterProfilesChangedStatus.DELETED, oldClusterProfile.getConfigurationAsMap(true), null);
            updateClusterProfilesCopy();
        }

    }

    private void updateClusterProfilesCopy() {
        this.existingClusterProfiles = goConfigService.getElasticConfig().getClusterProfiles();
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        this.existingClusterProfiles = newCruiseConfig.getElasticConfig().getClusterProfiles();
    }

    @Deprecated
        //used only for tests
    ClusterProfiles getExistingClusterProfiles() {
        return existingClusterProfiles;
    }
}
