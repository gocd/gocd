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
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.SecretConfigUsage;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.go.i18n.LocalizedMessage.cannotDeleteResourceBecauseOfDependentPipelines;

public class SecretConfigDeleteCommand extends SecretConfigCommand {
    private final Set<SecretConfigUsage> usageInformation;

    public SecretConfigDeleteCommand(GoConfigService goConfigService, SecretConfig secretConfig, Set<SecretConfigUsage> usageInformation, SecretsExtension extension, Username currentUser, LocalizedOperationResult result) {
        super(goConfigService, secretConfig, extension, currentUser, result);
        this.usageInformation = usageInformation;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        SecretConfig configToBeDeleted = findExistingProfile(preprocessedConfig);
        getPluginProfiles(preprocessedConfig).remove(configToBeDeleted);

    }

    @Override
    public SecretConfig getPreprocessedEntityConfig() {
        return profile;
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        if (usageInformation != null && !usageInformation.isEmpty()) {
            List<String> pipelineNames = usageInformation.stream().map(usage -> usage.getPipelineName()).collect(Collectors.toList());
            String message = cannotDeleteResourceBecauseOfDependentPipelines(getObjectDescriptor().getEntityNameLowerCase(), profile.getId(), pipelineNames);
            result.unprocessableEntity(message);
            throw new GoConfigInvalidException(preprocessedConfig, String.format("The %s '%s' is being referenced by pipeline(s): %s.", getObjectDescriptor().getEntityNameLowerCase(), profile.getId(), StringUtils.join(pipelineNames, ", ")));
        }

        return true;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig);
    }
}
