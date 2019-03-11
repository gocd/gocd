/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import java.util.Map;

public abstract class ClusterProfileCommand extends PluginProfileCommand<ClusterProfile, ClusterProfiles> {
    private final ElasticAgentExtension extension;

    public ClusterProfileCommand(ElasticAgentExtension extension, GoConfigService goConfigService, ClusterProfile clusterProfile, Username username, HttpLocalizedOperationResult result) {
        super(goConfigService, clusterProfile, username, result);
        this.extension = extension;
    }

    @Override
    protected ClusterProfiles getPluginProfiles(CruiseConfig preprocessedConfig) {
        return preprocessedConfig.getElasticConfig().getClusterProfiles();
    }

    @Override
    public ValidationResult validateUsingExtension(String pluginId, Map<String, String> configuration) {
        return extension.validateClusterProfile(pluginId, configuration);
    }

    @Override
    protected String getObjectDescriptor() {
        return "Cluster Profile";
    }

    public abstract void update(CruiseConfig preprocessedConfig) throws Exception;

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return true;
    }

    @Override
    public boolean isAuthorized() {
        if (!goConfigService.isAdministrator(currentUser.getUsername())) {
            result.forbidden(LocalizedMessage.forbiddenToEditResource("Cluster Profile", profile.getId(), currentUser.getDisplayName()), HealthStateType.forbidden());
            return false;
        }

        return true;
    }
}
