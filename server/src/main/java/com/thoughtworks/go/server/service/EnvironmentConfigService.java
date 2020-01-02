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
package com.thoughtworks.go.server.service;

import com.google.common.collect.Sets;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.update.AddEnvironmentCommand;
import com.thoughtworks.go.config.update.DeleteEnvironmentCommand;
import com.thoughtworks.go.config.update.PatchEnvironmentCommand;
import com.thoughtworks.go.config.update.UpdateEnvironmentCommand;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.ConfigElementForEdit;
import com.thoughtworks.go.domain.EnvironmentPipelineMatchers;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.ui.EnvironmentViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toList;

/**
 * @understands grouping of agents and pipelines within an environment
 */
@Service
public class EnvironmentConfigService implements ConfigChangedListener, AgentChangeListener {
    public GoConfigService goConfigService;
    private SecurityService securityService;
    private EntityHashingService entityHashingService;
    private AgentService agentService;

    private EnvironmentsConfig environments;
    private EnvironmentPipelineMatchers matchers;
    private static final Cloner cloner = new Cloner();

    public EnvironmentConfigService() {
    }

    @Autowired
    public EnvironmentConfigService(GoConfigService goConfigService, SecurityService securityService,
                                    EntityHashingService entityHashingService, AgentService agentService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.entityHashingService = entityHashingService;
        this.agentService = agentService;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(new EntityConfigChangedListener<EnvironmentConfig>() {
            @Override
            public void onEntityConfigChange(EnvironmentConfig entity) {
                syncEnvironments(goConfigService.getEnvironments());
            }
        });

        goConfigService.register(new EntityConfigChangedListener<ConfigRepoConfig>() {
            @Override
            public void onEntityConfigChange(ConfigRepoConfig entity) {
                if(!goConfigService.getCurrentConfig().getConfigRepos().hasConfigRepo(entity.getId())) {
                    syncEnvironments(goConfigService.getEnvironments());
                }
            }
        });

        agentService.registerAgentChangeListeners(this);
    }

    List<JobPlan> filterJobsByAgent(List<JobPlan> jobPlans, String agentUuid) {
        return jobPlans.stream().filter(jobPlan -> matchers.match(jobPlan.getPipelineName(), agentUuid)).collect(toList());
    }

    String envForPipeline(String pipelineName) {
        return matchers.stream()
                .filter(matcher -> matcher.hasPipeline(pipelineName))
                .map(matcher -> str(matcher.name()))
                .findFirst()
                .orElse(null);
    }

    public EnvironmentConfig environmentForPipeline(String pipelineName) {
        return environments.findEnvironmentForPipeline(new CaseInsensitiveString(pipelineName));
    }

    public Agents agentsForPipeline(final CaseInsensitiveString pipelineName) {
        Agents agents = new Agents();

        if (environments.isPipelineAssociatedWithAnyEnvironment(pipelineName)) {
            EnvironmentConfig pipelineEnvConfig = environments.findEnvironmentForPipeline(pipelineName);
            for (EnvironmentAgentConfig envAgentConfig : pipelineEnvConfig.getAgents()) {
                agents.add(agentService.getAgentByUUID(envAgentConfig.getUuid()));
            }

        } else {
            for (Agent agent : agentService.agents()) {
                if (!environments.isAgentAssociatedWithEnvironment(agent.getUuid())) {
                    agents.add(agent);
                }
            }
        }

        return agents;
    }

    public List<String> getEnvironmentNames() {
        return environments.names().stream().map(CaseInsensitiveString::toString).collect(toList());
    }

    public Set<EnvironmentConfig> getEnvironments() {
        return new HashSet<>(environments);
    }

    public Set<String> getAgentEnvironmentNames(String uuid) {
        return environments.getAgentEnvironmentNames(uuid);
    }

    public Set<EnvironmentConfig> getAgentEnvironments(String uuid) {
        return environments.getAgentEnvironments(uuid);
    }

    public EnvironmentConfig getEnvironmentConfig(String envName) {
        return environments.named(new CaseInsensitiveString(envName));
    }

    public EnvironmentConfig find(String envName) {
        return environments.find(new CaseInsensitiveString((envName)));
    }

    public EnvironmentConfig getEnvironmentForEdit(String envName) {
        return cloner.deepClone(goConfigService.getConfigForEditing().getEnvironments().find(new CaseInsensitiveString(envName)));
    }

    List<EnvironmentConfig> getAllLocalEnvironments() {
        return getEnvironmentNames().stream()
                .map(this::getEnvironmentForEdit)
                .collect(toList());
    }

    public List<EnvironmentConfig> getAllMergedEnvironments() {
        return getEnvironmentNames().stream()
                .map(env -> getMergedEnvironmentforDisplay(env, new HttpLocalizedOperationResult()).getConfigElement())
                .collect(toList());
    }

    // don't remove - used in rails
    public List<EnvironmentViewModel> listAllMergedEnvironments() {
        return getAllMergedEnvironments().stream()
                .map(EnvironmentViewModel::new)
                .collect(Collectors.toList());
    }

    public ConfigElementForEdit<EnvironmentConfig> getMergedEnvironmentforDisplay(String envName, HttpLocalizedOperationResult result) {
        ConfigElementForEdit<EnvironmentConfig> configElmForEdit = null;
        try {
            CruiseConfig cruiseConfig = goConfigService.getMergedConfigForEditing();
            EnvironmentConfig envConfig = environments.named(new CaseInsensitiveString(envName));
            configElmForEdit = new ConfigElementForEdit<>(cloner.deepClone(envConfig), cruiseConfig.getMd5());
        } catch (RecordNotFoundException e) {
            result.badRequest(EntityType.Environment.notFoundMessage(envName));
        }
        return configElmForEdit;
    }

    public void createEnvironment(final BasicEnvironmentConfig envConfig, final Username user, final HttpLocalizedOperationResult result) {
        String actionFailed = "Failed to add environment '" + envConfig.name() + "'.";
        AddEnvironmentCommand addEnvCmd = new AddEnvironmentCommand(goConfigService, envConfig, user, actionFailed, result);
        update(addEnvCmd, envConfig, user, result, actionFailed);
    }

    public List<EnvironmentPipelineModel> getAllLocalPipelinesForUser(Username user) {
        List<PipelineConfig> pipelineConfigs = goConfigService.getAllLocalPipelineConfigs();
        return getAllPipelinesForUser(user, pipelineConfigs);
    }

    public List<EnvironmentPipelineModel> getAllRemotePipelinesForUserInEnvironment(Username user, EnvironmentConfig envConfig) {
        List<EnvironmentPipelineModel> envPipelineModelList = new ArrayList<>();

        for (EnvironmentPipelineConfig envPipeline : envConfig.getRemotePipelines()) {
            String pipelineName = str(envPipeline.getName());
            if (securityService.hasViewPermissionForPipeline(user, pipelineName)) {
                String envName = str(envConfig.name());
                envPipelineModelList.add(new EnvironmentPipelineModel(pipelineName, envName));
            }
        }
        sort(envPipelineModelList);

        return envPipelineModelList;
    }

    public void updateEnvironment(final String oldEnvName, final EnvironmentConfig envConfig, final Username username,
                                  String md5, final HttpLocalizedOperationResult result) {
        String failureMsg = "Failed to update environment '" + oldEnvName + "'.";
        UpdateEnvironmentCommand updateEnvCmd = new UpdateEnvironmentCommand(goConfigService, oldEnvName, envConfig, username, failureMsg, md5, entityHashingService, result);
        update(updateEnvCmd, envConfig, username, result, failureMsg);

        if (result.isSuccessful()) {
            result.setMessage("Updated environment '" + oldEnvName + "'.");
        }
    }

    public void patchEnvironment(final EnvironmentConfig envConfig, List<String> pipelinesToAdd, List<String> pipelinesToRemove,
                                 List<EnvironmentVariableConfig> envVarsToAdd,
                                 List<String> envVarsToRemove, final Username username, final HttpLocalizedOperationResult result) {
        String failedActionErrMsg = "Failed to update environment '" + envConfig.name() + "'.";
        PatchEnvironmentCommand patchEnvCmd = new PatchEnvironmentCommand(goConfigService, envConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, username, failedActionErrMsg, result);
        update(patchEnvCmd, envConfig, username, result, failedActionErrMsg);
        if (result.isSuccessful()) {
            result.setMessage("Updated environment '" + envConfig.name() + "'.");
        }
    }

    public void deleteEnvironment(final EnvironmentConfig envConfig, final Username username, final HttpLocalizedOperationResult result) {
        String environmentName = envConfig.name().toString();
        String actionFailed = "Failed to delete environment '" + envConfig.name() + "'.";
        DeleteEnvironmentCommand deleteEnvironmentCommand = new DeleteEnvironmentCommand(goConfigService, envConfig, username, actionFailed, result);
        update(deleteEnvironmentCommand, envConfig, username, result, actionFailed);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.Environment.deleteSuccessful(environmentName));
        }
    }

    @Override
    public void agentChanged(Agent agent) {
        String uuid = agent.getUuid();

        Set<String> originalEnvNames = getAgentEnvironmentNames(uuid);
        HashSet<String> newEnvNames = new HashSet<>(agent.getEnvironmentsAsList());

        Set<String> envsToRemove = Sets.difference(originalEnvNames, newEnvNames);
        Set<String> envToAdd = Sets.difference(newEnvNames, originalEnvNames);

        removeAgentFromCurrentlyAssociatedEnvironments(uuid, new ArrayList<>(envsToRemove));
        addAgentToNewlyAssociatedEnvironments(uuid, new ArrayList<>(envToAdd));

        matchers = environments.matchers();
    }

    @Override
    public void agentDeleted(Agent agent) {
        List<String> envNames = agent.getEnvironmentsAsList();
        removeAgentFromCurrentlyAssociatedEnvironments(agent.getUuid(), envNames);
        matchers = environments.matchers();
    }

    void syncEnvironments(EnvironmentsConfig envsConfig) {
        if (envsConfig != null) {
            environments = envsConfig;
            agentService.getAgentInstances().forEach(this::syncAssociatedAgentFromDB);
            matchers = envsConfig.matchers();
        }
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        syncEnvironments(newCruiseConfig.getEnvironments());
    }

    private void removeAgentFromCurrentlyAssociatedEnvironments(String uuid, List<String> envNames) {
        envNames.stream().map(this::find)
                .filter(envConfig -> isEnvironmentAssociatedWithAgentLocally(envConfig, uuid))
                .forEach(envConfig -> envConfig.removeAgent(uuid));
    }

    private void addAgentToNewlyAssociatedEnvironments(String uuid, List<String> envNames) {
        envNames.stream().map(this::find)
                .filter(envConfig -> isEnvironmentNotAssociatedWithAgent(envConfig, uuid))
                .forEach(envConfig -> envConfig.addAgentIfNew(uuid));
    }

    private void syncAssociatedAgentFromDB(AgentInstance agentInstance) {
        Agent agent = agentInstance.getAgent();
        String uuid = agent.getUuid();
        List<String> envNames = agent.getEnvironmentsAsList();

        addAgentToNewlyAssociatedEnvironments(uuid, envNames);
    }

    private boolean isEnvironmentAssociatedWithAgentLocally(EnvironmentConfig envConfig, String uuid) {
        return envConfig != null && envConfig.hasAgent(uuid) && !envConfig.containsAgentRemotely(uuid);
    }

    private boolean isEnvironmentNotAssociatedWithAgent(EnvironmentConfig envConfig, String uuid) {
        return envConfig != null && !envConfig.hasAgent(uuid);
    }

    private List<EnvironmentPipelineModel> getAllPipelinesForUser(Username user, List<PipelineConfig> pipelineConfigs) {
        List<EnvironmentPipelineModel> pipelines = new ArrayList<>();

        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            String pipelineName = str(pipelineConfig.name());
            if (securityService.hasViewPermissionForPipeline(user, pipelineName)) {
                EnvironmentConfig environment = environments.findEnvironmentForPipeline(new CaseInsensitiveString(pipelineName));
                if (environment != null) {
                    pipelines.add(new EnvironmentPipelineModel(pipelineName, str(environment.name())));
                } else {
                    pipelines.add(new EnvironmentPipelineModel(pipelineName));
                }
            }
        }
        sort(pipelines);

        return pipelines;
    }

    private void update(EntityConfigUpdateCommand updateEnvCmd, EnvironmentConfig config, Username currentUser,
                        HttpLocalizedOperationResult result, String actionFailed) {
        try {
            goConfigService.updateConfig(updateEnvCmd, currentUser);
        } catch (Exception e) {
            if ((e instanceof GoConfigInvalidException) && !result.hasMessage()) {
                result.unprocessableEntity(entityConfigValidationFailed(config.getClass().getAnnotation(ConfigTag.class).value(), config.name(), e.getMessage()));
            } else if (!result.hasMessage()) {
                result.badRequest(LocalizedMessage.composite(actionFailed, e.getMessage()));
            }
        }
    }
}
