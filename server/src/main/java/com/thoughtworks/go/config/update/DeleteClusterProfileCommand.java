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
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class DeleteClusterProfileCommand extends ClusterProfileCommand {
    public DeleteClusterProfileCommand(GoConfigService goConfigService, ClusterProfile clusterProfile, Username currentUser, HttpLocalizedOperationResult result) {
        super(goConfigService, clusterProfile, currentUser, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        ClusterProfiles clusterProfiles = getPluginProfiles(preprocessedConfig);
        clusterProfiles.remove(profile);

        preprocessedConfig.getElasticConfig().setClusterProfiles(clusterProfiles);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        //todo: Validate if the cluster is associated with a profile
        boolean isValid = super.isValid(preprocessedConfig);
        if (isValid) {
            result.setMessage(LocalizedMessage.resourceDeleteSuccessful("Cluster Profile", profile.getId()));
        }

        return isValid;
    }
}
