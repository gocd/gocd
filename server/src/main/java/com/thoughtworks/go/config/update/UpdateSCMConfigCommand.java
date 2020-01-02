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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class UpdateSCMConfigCommand extends SCMConfigCommand {

    private String md5;
    private EntityHashingService entityHashingService;

    public UpdateSCMConfigCommand(SCM globalScmConfig, PluggableScmService pluggableScmService, GoConfigService goConfigService, Username currentUser, LocalizedOperationResult result, String md5, EntityHashingService entityHashingService) {
        super(globalScmConfig, pluggableScmService, goConfigService, currentUser, result);
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        SCM scm = findSCM(modifiedConfig);
        scm.setAutoUpdate(globalScmConfig.isAutoUpdate());
        scm.setConfiguration(globalScmConfig.getConfiguration());

    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        SCM existingSCM = findSCM(cruiseConfig);
        boolean freshRequest =  entityHashingService.md5ForEntity(existingSCM).equals(md5);
        if (!freshRequest) {
            result.stale(EntityType.SCM.staleConfig(globalScmConfig.getName()));
        }

        return freshRequest;
    }
}
