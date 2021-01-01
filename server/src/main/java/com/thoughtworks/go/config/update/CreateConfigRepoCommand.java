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
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class CreateConfigRepoCommand extends ConfigRepoCommand {
    private final ConfigRepoConfig configRepo;

    public CreateConfigRepoCommand(SecurityService securityService, ConfigRepoConfig configRepo, Username username,
                                   HttpLocalizedOperationResult result, ConfigRepoExtension configRepoExtension) {
        super(securityService, configRepo, username, result, configRepoExtension);
        this.configRepo = configRepo;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedConfig.getConfigRepos().add(configRepo);
    }
}
