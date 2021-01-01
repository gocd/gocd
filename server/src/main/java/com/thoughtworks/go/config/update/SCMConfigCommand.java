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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;

public abstract class SCMConfigCommand implements EntityConfigUpdateCommand<SCM> {
    protected final SCM globalScmConfig;
    protected final LocalizedOperationResult result;
    private final PluggableScmService pluggableScmService;
    protected SCM preprocessedGlobalScmConfig;
    protected final GoConfigService goConfigService;
    protected final Username currentUser;

    public SCMConfigCommand(SCM globalScmConfig, PluggableScmService pluggableScmService, GoConfigService goConfigService, Username currentUser, LocalizedOperationResult result) {
        this.globalScmConfig = globalScmConfig;
        this.pluggableScmService = pluggableScmService;
        this.goConfigService = goConfigService;
        this.currentUser = currentUser;
        this.result = result;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        SCMs scms = preprocessedConfig.getSCMs();
        preprocessedGlobalScmConfig = scms.find(globalScmConfig.getSCMId());
        preprocessedGlobalScmConfig.validate(null);
        if (preprocessedGlobalScmConfig.getAllErrors().isEmpty()) {
            scms.validate(null);
            boolean isValid = pluggableScmService.isValid(preprocessedGlobalScmConfig);
            BasicCruiseConfig.copyErrors(preprocessedGlobalScmConfig, globalScmConfig);
            return preprocessedGlobalScmConfig.getAllErrors().isEmpty() && scms.errors().isEmpty() && isValid;
        }
        BasicCruiseConfig.copyErrors(preprocessedGlobalScmConfig, globalScmConfig);
        return false;
    }

    @Override
    public SCM getPreprocessedEntityConfig() {
        return preprocessedGlobalScmConfig;
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(globalScmConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized();
    }

    private boolean isAuthorized() {
        if (!(goConfigService.isUserAdmin(currentUser) || goConfigService.isGroupAdministrator(currentUser.getUsername()))) {
            result.forbidden(EntityType.SCM.forbiddenToEdit(globalScmConfig.getId(), currentUser.getUsername()), forbidden());
            return false;
        }
        return true;
    }

    SCM findSCM(CruiseConfig cruiseConfig) {
        SCMs scms = cruiseConfig.getSCMs();
        SCM existingSCM = scms.find(globalScmConfig.getSCMId());
        if (existingSCM == null) {
            result.notFound(EntityType.SCM.notFoundMessage(globalScmConfig.getSCMId()), HealthStateType.notFound());
            throw new NullPointerException(String.format("The pluggable scm material with id '%s' is not found.", globalScmConfig.getSCMId()));
        } else {
            return existingSCM;
        }
    }
}
