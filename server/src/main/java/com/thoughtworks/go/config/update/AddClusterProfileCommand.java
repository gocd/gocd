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
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class AddClusterProfileCommand extends ClusterProfileCommand {
    public AddClusterProfileCommand(ElasticAgentExtension extension, GoConfigService goConfigService, ClusterProfile clusterProfile, Username username, HttpLocalizedOperationResult result) {
        super(extension, goConfigService, clusterProfile, username, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        ClusterProfiles clusterProfiles = getPluginProfiles(preprocessedConfig);
        clusterProfiles.add(profile);

        preprocessedConfig.getElasticConfig().setClusterProfiles(clusterProfiles);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForCreateOrUpdate(preprocessedConfig);
    }
}
