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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.config.update.CreatePipelineConfigCommand;
import com.thoughtworks.go.config.update.DeletePipelineConfigCommand;
import com.thoughtworks.go.config.update.UpdatePipelineConfigCommand;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.presentation.CanDeleteResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.tasks.PluggableTaskService;
import com.thoughtworks.go.util.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @understands providing services around a pipeline configuration
 */
@Service
public class PipelineConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private final PluggableTaskService pluggableTaskService;
    private final EntityHashingService entityHashingService;
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(PipelineConfigService.class);

    @Autowired
    public PipelineConfigService(GoConfigService goConfigService, SecurityService securityService, PluggableTaskService pluggableTaskService, EntityHashingService entityHashingService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.pluggableTaskService = pluggableTaskService;
        this.entityHashingService = entityHashingService;
    }

    public Map<CaseInsensitiveString, CanDeleteResult> canDeletePipelines() {
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        Map<CaseInsensitiveString, CanDeleteResult> nameToCanDeleteIt = new HashMap<>();
        Hashtable<CaseInsensitiveString, Node> hashtable = cruiseConfig.getDependencyTable();
        List<CaseInsensitiveString> pipelineNames = cruiseConfig.getAllPipelineNames();

        for (CaseInsensitiveString pipelineName : pipelineNames) {
            ConfigOrigin origin = pipelineConfigOrigin(cruiseConfig,pipelineName);
            if(origin != null && !origin.isLocal())
            {
                nameToCanDeleteIt.put(pipelineName, new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_REMOTE_PIPELINE", pipelineName, origin.displayName())));
            }
            else {
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
        }
        return nameToCanDeleteIt;
    }

    private ConfigOrigin pipelineConfigOrigin(CruiseConfig cruiseConfig,final CaseInsensitiveString pipelineName) {
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(pipelineName);
        if(pipelineConfig == null)
            return null;
        return pipelineConfig.getOrigin();
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

    private void update(Username currentUser, PipelineConfig pipelineConfig, LocalizedOperationResult result, EntityConfigUpdateCommand command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                if(!result.hasMessage()){
                    result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", pipelineConfig.getClass().getAnnotation(ConfigTag.class).value(), CaseInsensitiveString.str(pipelineConfig.name()), e.getMessage()));
                }
            } else if (!(e instanceof ConfigUpdateCheckFailedException)) {
                LOGGER.error(e.getMessage(), e);
                result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getMessage()));
            }
        }
    }


    public void updatePipelineConfig(final Username currentUser, final PipelineConfig pipelineConfig, final String md5, final LocalizedOperationResult result) {
        validatePluggableTasks(pipelineConfig);
        UpdatePipelineConfigCommand updatePipelineConfigCommand = new UpdatePipelineConfigCommand(goConfigService, entityHashingService, pipelineConfig, currentUser, md5, result);
        update(currentUser, pipelineConfig, result, updatePipelineConfigCommand);
    }

    public List<PipelineConfigs> viewableGroupsFor(Username username) {
        ArrayList<PipelineConfigs> list = new ArrayList<>();
        for (PipelineConfigs pipelineConfigs : goConfigService.cruiseConfig().getGroups()) {
            if (securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), pipelineConfigs.getGroup())) {
                list.add(pipelineConfigs);
            }
        }
        return list;
    }

    public List<PipelineConfigs> viewableOrOperatableGroupsFor(Username username) {
        ArrayList<PipelineConfigs> list = new ArrayList<>();
        for (PipelineConfigs pipelineConfigs : goConfigService.cruiseConfig().getGroups()) {
            if(hasViewOrOperatePermissionForGroup(username, pipelineConfigs.getGroup())) {
                list.add(pipelineConfigs);
            }
        }
        return list;
    }

        public void createPipelineConfig(final Username currentUser, final PipelineConfig pipelineConfig, final LocalizedOperationResult result, final String groupName) {
        validatePluggableTasks(pipelineConfig);
        CreatePipelineConfigCommand createPipelineConfigCommand = new CreatePipelineConfigCommand(goConfigService, pipelineConfig, currentUser, result, groupName);
        update(currentUser, pipelineConfig, result, createPipelineConfigCommand);
    }

    public void deletePipelineConfig(final Username currentUser, final PipelineConfig pipelineConfig, final LocalizedOperationResult result) {
        DeletePipelineConfigCommand deletePipelineConfigCommand = new DeletePipelineConfigCommand(goConfigService, pipelineConfig, currentUser, result);
        update(currentUser, pipelineConfig, result, deletePipelineConfigCommand);
        if(result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("PIPELINE_DELETE_SUCCESSFUL", pipelineConfig.name()));
        }
    }

    private boolean hasViewOrOperatePermissionForGroup(Username username, String group) {
        return securityService.hasViewPermissionForGroup(CaseInsensitiveString.str(username.getUsername()), group) ||
                securityService.hasOperatePermissionForGroup(username.getUsername(), group);
    }

    private void validatePluggableTasks(PipelineConfig config) {
        for(PluggableTask task: pluggableTask(config)) {
            pluggableTaskService.isValid(task);
        }
    }

    private List<PluggableTask> pluggableTask(PipelineConfig config) {
        List<PluggableTask> tasks = new ArrayList<>();
        for(StageConfig stageConfig: config.getStages()) {
            for(JobConfig jobConfig: stageConfig.getJobs()) {
                for(Task task: jobConfig.getTasks()) {
                    if(task instanceof PluggableTask) {
                        tasks.add((PluggableTask) task);
                    }
                }
            }
        }
        return tasks;
    }
}
