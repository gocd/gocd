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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class ElasticAgentProfileCreateCommand extends ElasticAgentProfileCommand {
    public ElasticAgentProfileCreateCommand(GoConfigService goConfigService, ElasticProfile elasticProfile, ElasticAgentExtension extension, Username currentUser, LocalizedOperationResult result) {
        super(goConfigService, elasticProfile, extension, currentUser, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        getPluginProfiles(preprocessedConfig).add(elasticProfile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForCreateOrUpdate(preprocessedConfig);
    }

    @Override
    public void encrypt(CruiseConfig preProcessedConfig) {
        elasticProfile.encryptSecureProperties(preProcessedConfig);
    }
}
