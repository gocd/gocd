/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class UpdateEnvironmentCommand extends EnvironmentCommand implements EntityConfigUpdateCommand<EnvironmentConfig> {

    private GoConfigService goConfigService;
    private final EnvironmentConfig oldEnvironmentConfig;
    private final EnvironmentConfig newEnvironmentConfig;
    private final Username username;
    private String md5;
    private EntityHashingService hashingService;
    private final HttpLocalizedOperationResult result;

    public UpdateEnvironmentCommand(GoConfigService goConfigService, EnvironmentConfig oldEnvironmentConfig, EnvironmentConfig newEnvironmentConfig, Username username, Localizable.CurryableLocalizable actionFailed, String md5, EntityHashingService hashingService, HttpLocalizedOperationResult result) {
        super(actionFailed, newEnvironmentConfig, result);
        this.goConfigService = goConfigService;
        this.oldEnvironmentConfig = oldEnvironmentConfig;
        this.newEnvironmentConfig = newEnvironmentConfig;
        this.username = username;
        this.md5 = md5;
        this.hashingService = hashingService;
        this.result = result;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        EnvironmentsConfig environments = preprocessedConfig.getEnvironments();
        int index = environments.indexOf(oldEnvironmentConfig);
        environments.remove(index);
        environments.add(index, newEnvironmentConfig);
    }

    @Override
    public void clearErrors() {
        BasicCruiseConfig.clearErrors(oldEnvironmentConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return isAuthorized(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        EnvironmentConfig config = cruiseConfig.getEnvironments().find(oldEnvironmentConfig.name());
        boolean freshRequest =  hashingService.md5ForEntity(config).equals(md5);
        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Environment", oldEnvironmentConfig.name()));
        }
        return freshRequest;
    }

    private boolean isAuthorized(CruiseConfig cruiseConfig) {
        if (!goConfigService.isAdministrator(username.getUsername())) {
            Localizable noPermission = LocalizedMessage.string("NO_PERMISSION_TO_UPDATE_ENVIRONMENT", oldEnvironmentConfig.name().toString(), username.getDisplayName());
            result.unauthorized(noPermission, HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}

