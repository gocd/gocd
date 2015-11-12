/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.PipelineConfigChangedListener;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.presentation.CanDeleteResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * @understands providing services around a pipeline configuration
 */
@Service
public class PipelineConfigService implements PipelineConfigChangedListener, Initializer {
    private final GoConfigService goConfigService;
    private static final String GO_PIPELINE_CONFIGS_ETAGS_CACHE = "GO_PIPELINE_CONFIGS_ETAGS_CACHE".intern();
    private GoCache goCache;
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(PipelineConfigService.class);

    @Autowired
    public PipelineConfigService(GoConfigService goConfigService, GoCache goCache) {
        this.goConfigService = goConfigService;
        this.goCache = goCache;
    }

    public void initialize() {
        goConfigService.register(this);
    }

    public Map<CaseInsensitiveString, CanDeleteResult> canDeletePipelines() {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        Map<CaseInsensitiveString, CanDeleteResult> nameToCanDeleteIt = new HashMap<CaseInsensitiveString, CanDeleteResult>();
        Hashtable<CaseInsensitiveString, Node> hashtable = cruiseConfig.getDependencyTable();
        List<CaseInsensitiveString> pipelineNames = cruiseConfig.getAllPipelineNames();

        for (CaseInsensitiveString pipelineName : pipelineNames) {
            CaseInsensitiveString envName = environmentUsedIn(cruiseConfig, pipelineName);
            if (envName != null) {
                nameToCanDeleteIt.put(pipelineName, new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT", pipelineName, envName)));
            } else {
                CaseInsensitiveString downStream = downstreamOf(hashtable, pipelineName);
                if (downStream != null) {
                    nameToCanDeleteIt.put(pipelineName, new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", pipelineName, downStream)));
                } else {
                    nameToCanDeleteIt.put(pipelineName, new CanDeleteResult(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")));
                }
            }
        }
        return nameToCanDeleteIt;
    }

    private CaseInsensitiveString downstreamOf(Hashtable<CaseInsensitiveString, Node> pipelineToUpstream, final CaseInsensitiveString pipelineName) {
        for (Map.Entry<CaseInsensitiveString, Node> entry : pipelineToUpstream.entrySet()) {
            if (entry.getValue().hasDependency(pipelineName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private CaseInsensitiveString environmentUsedIn(CruiseConfig cruiseConfig, final CaseInsensitiveString pipelineName) {
        return cruiseConfig.getEnvironments().findEnvironmentNameForPipeline(pipelineName);
    }

    public PipelineConfig getPipelineConfig(String pipelineName) {
        return goConfigService.getConfigForEditing().getPipelineConfigByName(new CaseInsensitiveString(pipelineName));
    }

    public void updatePipelineConfig(final Username currentUser, final PipelineConfig pipelineConfig, final LocalizedOperationResult result) {
        try {
            goConfigService.updatePipeline(pipelineConfig, currentUser, result, new SaveCommand<PipelineConfig>() {
                public String group;

                @Override
                public boolean isValid(CruiseConfig modifiedConfig, PipelineConfig preprocessedConfig) {
                    boolean isValid = preprocessedConfig.validateTree(PipelineConfigSaveValidationContext.forChain(false, getPipelineGroup(), modifiedConfig, preprocessedConfig));
                    if (!isValid) BasicCruiseConfig.copyErrors(preprocessedConfig, pipelineConfig);
                    return isValid;
                }

                @Override
                public boolean hasWritePermissions() {
                    return goConfigService.canEditPipeline(pipelineConfig.name().toString(), currentUser, result, getPipelineGroup());
                }

                @Override
                public String getPipelineGroup() {
                    if(group == null){
                        this.group = goConfigService.findGroupNameByPipeline(pipelineConfig.name());
                    }
                    return group;
                }

                @Override
                public void updateConfig(CruiseConfig configForEdit, PipelineConfig pipelineConfig) {
                    configForEdit.update(getPipelineGroup(), pipelineConfig.name().toString(), pipelineConfig);
                }

            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getMessage()));
        }
    }

    public void createPipelineConfig(final Username currentUser, final PipelineConfig pipelineConfig, final HttpLocalizedOperationResult result, final String groupName) {
        try {
            goConfigService.updatePipeline(pipelineConfig, currentUser, result, new SaveCommand<PipelineConfig>() {
                @Override
                public boolean isValid(CruiseConfig preprocessedConfig, PipelineConfig preprocessedPipelineConfig) {
                    boolean isValid = preprocessedPipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, groupName, preprocessedConfig, preprocessedPipelineConfig));
                    if (!isValid) BasicCruiseConfig.copyErrors(preprocessedPipelineConfig, pipelineConfig);
                    return isValid;
                }

                @Override
                public boolean hasWritePermissions() {
                    if (goConfigService.groups().hasGroup(groupName) && !goConfigService.isUserAdminOfGroup(currentUser.getUsername(), groupName)) {
                        result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_GROUP", groupName), HealthStateType.unauthorised());
                        return false;
                    }
                    return true;
                }

                @Override
                public String getPipelineGroup() {
                    return groupName;
                }

                @Override
                public void updateConfig(CruiseConfig configForEdit, PipelineConfig pipelineConfig) {
                    configForEdit.addPipelineWithoutValidation(groupName, pipelineConfig);
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getMessage()));
        }
    }

    public interface SaveCommand<T extends Validatable>{
        boolean isValid(CruiseConfig modifiedConfig, T object);
        boolean hasWritePermissions();
        String getPipelineGroup();
        void updateConfig(CruiseConfig configForEdit, T configItemBeingSaved);
    }


    @Override
    public void onPipelineConfigChange(PipelineConfig pipelineConfig, String group) {
        PipelineConfigurationCache.getInstance().onPipelineConfigChange(pipelineConfig);
        if (goCache.get(GO_PIPELINE_CONFIGS_ETAGS_CACHE, pipelineConfig.name().toLower()) != null) {
            goCache.remove(GO_PIPELINE_CONFIGS_ETAGS_CACHE, pipelineConfig.name().toLower());
        }
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        PipelineConfigurationCache.getInstance().onConfigChange(goConfigService.getConfigForEditing());
        if (goCache.get(GO_PIPELINE_CONFIGS_ETAGS_CACHE) != null) {
            goCache.remove(GO_PIPELINE_CONFIGS_ETAGS_CACHE);
        }
    }
}
