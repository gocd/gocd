/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;


public class CreateTemplateConfigCommand extends TemplateConfigCommand {

    private SecurityService securityService;

    public CreateTemplateConfigCommand(PipelineTemplateConfig templateConfig, Username currentUser, SecurityService securityService, LocalizedOperationResult result) {
        super(templateConfig, result, currentUser);
        this.securityService = securityService;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        if (securityService.isUserGroupAdmin(currentUser)) {
            templateConfig.setAuthorization(new Authorization(new AdminsConfig(new AdminUser(currentUser.getUsername()))));
        }
        modifiedConfig.addTemplate(templateConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return super.isValid(preprocessedConfig, true);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        if (!(securityService.isUserAdmin(currentUser) || securityService.isUserGroupAdmin(currentUser))) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }
}
