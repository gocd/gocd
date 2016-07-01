/*
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
 */

package com.thoughtworks.go.config.update;


import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class UpdateSCMConfigCommand extends SCMConfigCommand {

    public UpdateSCMConfigCommand(SCM globalScmConfig, PluggableScmService pluggableScmService, GoConfigService goConfigService, Username currentUser, LocalizedOperationResult result) {
        super(globalScmConfig, pluggableScmService, goConfigService, currentUser, result);
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        SCMs scms = modifiedConfig.getSCMs();
        SCM scm = scms.find(globalScmConfig.getSCMId());
        if (scm != null) {
            scm.setAutoUpdate(globalScmConfig.isAutoUpdate());
            scm.setConfiguration(globalScmConfig.getConfiguration());
        }
        else {
            result.notFound(LocalizedMessage.string("SCM_MATERIAL_NOT_FOUND"), HealthStateType.notFound());
            throw new NullPointerException(String.format("The pluggable scm material with id '%s' is not found.", globalScmConfig.getSCMId()));
        }
    }

}
