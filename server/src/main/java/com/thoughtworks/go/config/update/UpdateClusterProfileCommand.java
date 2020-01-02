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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ErrorCollector;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

import java.util.List;

public class UpdateClusterProfileCommand extends ClusterProfileCommand {
    public UpdateClusterProfileCommand(ElasticAgentExtension extension, GoConfigService goConfigService, ClusterProfile clusterProfile, Username username, HttpLocalizedOperationResult result) {
        super(extension, goConfigService, clusterProfile, username, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        ClusterProfile existingClusterProfile = findExistingProfile(preprocessedConfig);
        ClusterProfiles clusterProfiles = getPluginProfiles(preprocessedConfig);

        clusterProfiles.set(clusterProfiles.indexOf(existingClusterProfile), profile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        ElasticProfiles allElasticAgentProfiles = preprocessedConfig.getElasticConfig().getProfiles();
        allElasticAgentProfiles.validateTree(new ConfigSaveValidationContext(preprocessedConfig));
        List<ConfigErrors> allErrors = ErrorCollector.getAllErrors(allElasticAgentProfiles);
        if (!allErrors.isEmpty()) {
            throw new GoConfigInvalidException(preprocessedConfig, allErrors.get(0).firstError());
        }

        return isValidForCreateOrUpdate(preprocessedConfig);
    }
}
