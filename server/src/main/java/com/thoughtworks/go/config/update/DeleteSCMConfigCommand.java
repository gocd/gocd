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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.CaseInsensitiveString.toStringList;
import static com.thoughtworks.go.i18n.LocalizedMessage.cannotDeleteResourceBecauseOfDependentPipelines;
import static java.util.stream.Collectors.toList;

public class DeleteSCMConfigCommand extends SCMConfigCommand {

    public DeleteSCMConfigCommand(SCM globalScmConfig, PluggableScmService pluggableScmService, LocalizedOperationResult result, Username currentUser, GoConfigService goConfigService) {
        super(globalScmConfig, pluggableScmService, goConfigService, currentUser, result);
    }

    @Override
    public void update(CruiseConfig modifiedConfig) {
        SCM scm = findSCM(modifiedConfig);
        SCMs scms = modifiedConfig.getSCMs();
        scms.removeSCM(scm.getId());
        modifiedConfig.setSCMs(scms);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedGlobalScmConfig = globalScmConfig;
        if (!preprocessedConfig.canDeletePluggableSCMMaterial(globalScmConfig)) {
            List<PipelineConfig> pipelinesWithPluggableScm = preprocessedConfig.pipelinesAssociatedWithPluggableSCM(globalScmConfig);
            List<CaseInsensitiveString> pipelines = pipelinesWithPluggableScm.stream().map(PipelineConfig::name).collect(toList());
            result.unprocessableEntity(cannotDeleteResourceBecauseOfDependentPipelines("pluggable_scm", globalScmConfig.getName(), toStringList(pipelines)));
            throw new GoConfigInvalidException(preprocessedConfig, String.format("The scm '%s' is being referenced by pipeline(s): %s", globalScmConfig.getName(), pipelines));
        }

        return true;
    }
}
