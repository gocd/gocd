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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.domain.ClusterProfilesChangedStatus;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.utils.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.thoughtworks.go.serverhealth.HealthStateScope.aboutPlugin;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static com.thoughtworks.go.serverhealth.ServerHealthState.error;
import static java.lang.String.format;

@Component
public class ClusterProfilesChangedPluginNotifier extends EntityConfigChangedListener<ClusterProfile> implements ConfigChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProfilesChangedPluginNotifier.class);
    private ClusterProfiles existingClusterProfiles;
    private ElasticAgentPluginRegistry registry;
    private final SecretParamResolver secretParamResolver;
    private final ServerHealthService serverHealthService;
    private GoConfigService goConfigService;

    @Autowired
    public ClusterProfilesChangedPluginNotifier(GoConfigService goConfigService, ElasticAgentPluginRegistry registry, SecretParamResolver secretParamResolver, ServerHealthService serverHealthService) {
        this.goConfigService = goConfigService;
        this.existingClusterProfiles = goConfigService.getElasticConfig().getClusterProfiles();
        this.registry = registry;
        this.secretParamResolver = secretParamResolver;
        this.serverHealthService = serverHealthService;
        goConfigService.register(this);
    }

    @Override
    public void onEntityConfigChange(ClusterProfile updatedClusterProfile) {
        try {
            LOGGER.debug("Resolving secrets for updated cluster profile: {}", updatedClusterProfile);
            secretParamResolver.resolve(updatedClusterProfile);
        } catch (RulesViolationException | SecretResolutionFailureException e) {
            logAndRaiseServerHealthMessage(updatedClusterProfile, e.getMessage());
            return;
        }
        Map<String, String> updatedClusterConfigMap = updatedClusterProfile.getConfigurationAsMap(true, true);
        if (goConfigService.getElasticConfig().getClusterProfiles().find(updatedClusterProfile.getId()) == null) {
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.DELETED, updatedClusterConfigMap, null);
            updateClusterProfilesCopy();
            return;
        }

        ClusterProfile oldClusterProfile = existingClusterProfiles.find(updatedClusterProfile.getId());
        if (oldClusterProfile == null) {
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.CREATED, null, updatedClusterConfigMap);
            updateClusterProfilesCopy();
            return;
        }

        try {
            LOGGER.debug("Resolving secrets for older cluster profile: {}", oldClusterProfile);
            secretParamResolver.resolve(oldClusterProfile);
        } catch (RulesViolationException | SecretResolutionFailureException e) {
            logAndRaiseServerHealthMessage(oldClusterProfile, e.getMessage());
            return;
        }
        //cluster profile has been updated without changing plugin id
        Map<String, String> olderClusterConfigMap = oldClusterProfile.getConfigurationAsMap(true, true);
        if (oldClusterProfile.getPluginId().equals(updatedClusterProfile.getPluginId())) {
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.UPDATED, olderClusterConfigMap, updatedClusterConfigMap);
            updateClusterProfilesCopy();
        } else {
            //cluster profile has been updated including changing plugin id.
            //this internally results in deletion of a profile belonging to old plugin id and creation of the profile belonging to new plugin id
            registry.notifyPluginAboutClusterProfileChanged(updatedClusterProfile.getPluginId(), ClusterProfilesChangedStatus.CREATED, null, updatedClusterConfigMap);
            registry.notifyPluginAboutClusterProfileChanged(oldClusterProfile.getPluginId(), ClusterProfilesChangedStatus.DELETED, olderClusterConfigMap, null);
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

    private void logAndRaiseServerHealthMessage(ClusterProfile clusterProfile, String errorMessage) {
        String description = format("[ Cluster Profile Changed Notification ] Secrets resolution failed for cluster profile [%s] associated with plugin [%s]. Plugin might have not been notified about this change.\n Messages: %s ", clusterProfile, clusterProfile.getPluginId(), errorMessage);
        ServerHealthState healthState = error("Secret Resolution Failure", description, general(scope(clusterProfile.getPluginId())));
        healthState.setTimeout(Timeout.FIVE_MINUTES);
        serverHealthService.update(healthState);
        LOGGER.error(description);
    }

    private HealthStateScope scope(String pluginId) {
        return aboutPlugin(pluginId, "clusterProfileChangedCall");
    }
}
