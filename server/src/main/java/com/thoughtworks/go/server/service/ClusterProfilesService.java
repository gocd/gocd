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

import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.update.AddClusterProfileCommand;
import com.thoughtworks.go.config.update.DeleteClusterProfileCommand;
import com.thoughtworks.go.config.update.PluginProfileCommand;
import com.thoughtworks.go.config.update.UpdateClusterProfileCommand;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.GoPluginFrameworkException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class ClusterProfilesService extends PluginProfilesService<ClusterProfile> {
    private final ElasticAgentExtension extension;
    private final SecretParamResolver secretParamResolver;

    @Autowired
    public ClusterProfilesService(GoConfigService goConfigService, EntityHashingService hashingService, ElasticAgentExtension extension, SecretParamResolver secretParamResolver) {
        super(goConfigService, hashingService);
        this.extension = extension;
        this.secretParamResolver = secretParamResolver;
    }

    @Override
    public PluginProfiles<ClusterProfile> getPluginProfiles() {
        return goConfigService.getElasticConfig().getClusterProfiles();
    }

    public ClusterProfile create(ClusterProfile clusterProfile, Username currentUser, HttpLocalizedOperationResult result) {
        AddClusterProfileCommand addClusterProfileCommand = new AddClusterProfileCommand(extension, goConfigService, clusterProfile, currentUser, result);
        update(currentUser, clusterProfile, result, addClusterProfileCommand, true);
        return clusterProfile;
    }

    public void delete(ClusterProfile clusterProfile, Username currentUser, HttpLocalizedOperationResult result) {
        DeleteClusterProfileCommand deleteClusterProfileCommand = new DeleteClusterProfileCommand(extension, goConfigService, clusterProfile, currentUser, result);
        update(currentUser, clusterProfile, result, deleteClusterProfileCommand, false);
    }

    public ClusterProfile update(ClusterProfile newClusterProfile, Username currentUser, HttpLocalizedOperationResult result) {
        UpdateClusterProfileCommand updateClusterProfileCommand = new UpdateClusterProfileCommand(extension, goConfigService, newClusterProfile, currentUser, result);
        update(currentUser, newClusterProfile, result, updateClusterProfileCommand, true);
        return newClusterProfile;
    }

    @Override
    void validatePluginProperties(PluginProfileCommand command, ClusterProfile clusterProfile) {
        secretParamResolver.resolve(clusterProfile);
        try {
            ValidationResult result = command.validateUsingExtension(clusterProfile.getPluginId(), clusterProfile.getConfigurationAsMap(true, true));
            addErrorsToConfiguration(result, clusterProfile);
        } catch (RecordNotFoundException e) {
            clusterProfile.addError("pluginId", format("Plugin with id `%s` is not found.", clusterProfile.getPluginId()));
        } catch (GoPluginFrameworkException e) {
            clusterProfile.addError("pluginId", e.getMessage());
        } catch (Exception e) {
            //Ignore - it will be the invalid cipher text exception for an encrypted value. This will be validated later during entity update
        }
    }
}
