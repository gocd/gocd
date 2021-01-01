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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.i18n.LocalizedMessage.cannotDeleteResourceBecauseOfDependentResources;

public class DeleteClusterProfileCommand extends ClusterProfileCommand {
    public DeleteClusterProfileCommand(ElasticAgentExtension extension, GoConfigService goConfigService, ClusterProfile clusterProfile, Username currentUser, HttpLocalizedOperationResult result) {
        super(extension, goConfigService, clusterProfile, currentUser, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        ClusterProfiles clusterProfiles = getPluginProfiles(preprocessedConfig);
        clusterProfiles.remove(profile);
        preprocessedConfig.getElasticConfig().setClusterProfiles(clusterProfiles);
        this.preprocessedProfile = profile;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        List<String> usedByElasticProfiles = preprocessedConfig.getElasticConfig().getProfiles().stream()
                .filter(profile -> profile.getClusterProfileId().equals(this.profile.getId()))
                .map(ElasticProfile::getId)
                .collect(Collectors.toList());

        boolean isValid = usedByElasticProfiles.isEmpty();

        if (!isValid) {
            String message = cannotDeleteResourceBecauseOfDependentResources(getObjectDescriptor().getEntityNameLowerCase(), profile.getId(), EntityType.ElasticProfile.getEntityNameLowerCase(), usedByElasticProfiles);
            result.unprocessableEntity(message);
            throw new GoConfigInvalidException(preprocessedConfig, message);
        } else {
            result.setMessage(LocalizedMessage.resourceDeleteSuccessful("Cluster Profile", profile.getId()));
        }

        return isValid;
    }
}
