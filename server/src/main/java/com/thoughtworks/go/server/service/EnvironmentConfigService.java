/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.ConfigElementForEdit;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.domain.EnvironmentPipelineMatchers;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.ui.EnvironmentViewModel;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;

/**
 * @understands grouping of agents and pipelines within an environment
 */
@Service
public class EnvironmentConfigService implements ConfigChangedListener, AgentChangeListener {
    public final GoConfigService goConfigService;
    private final SecurityService securityService;
    private EntityHashingService entityHashingService;
    private AgentService agentService;

    private EnvironmentsConfig environments;
    private EnvironmentPipelineMatchers matchers;
    private static final Cloner cloner = new Cloner();

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
                syncEnvironmentsFromConfig(goConfigService.getEnvironments());
            }
        });

        goConfigService.register(new EntityConfigChangedListener<ConfigRepoConfig>() {
            @Override
            public void onEntityConfigChange(ConfigRepoConfig entity) {
                if(!goConfigService.getCurrentConfig().getConfigRepos().hasConfigRepo(entity.getId())) {
                    syncEnvironmentsFromConfig(goConfigService.getEnvironments());
                }
            }
        });

        agentService.registerAgentChangeListeners(this);
    }

    public List<JobPlan> filterJobsByAgent(List<JobPlan> jobPlans, String agentUuid) {
        ArrayList<JobPlan> plans = new ArrayList<>();
        for (JobPlan jobPlan : jobPlans) {
            if (matchers.match(jobPlan.getPipelineName(), agentUuid)) {
                plans.add(jobPlan);
            }
        }
        return plans;
    }

    public String envForPipeline(String pipelineName) {
        for (EnvironmentPipelineMatcher matcher : matchers) {
            if (matcher.hasPipeline(pipelineName)) {
                return CaseInsensitiveString.str(matcher.name());
            }
        }
        return null;
    }

    public EnvironmentConfig environmentForPipeline(String pipelineName) {
        return environments.findEnvironmentForPipeline(new CaseInsensitiveString(pipelineName));
    }

    public Agents agentsForPipeline(final CaseInsensitiveString pipelineName) {
        Agents agents = new Agents();

        if (environments.isPipelineAssociatedWithAnyEnvironment(pipelineName)) {
            EnvironmentConfig pipelineEnvConfig = environments.findEnvironmentForPipeline(pipelineName);
            for (EnvironmentAgentConfig envAgentConfig : pipelineEnvConfig.getAgents()) {
                agents.add(agentService.agentByUuid(envAgentConfig.getUuid()));
            }

        } else {
            for (Agent agent : agentService.agents()) {
                if (!environments.isAgentUnderEnvironment(agent.getUuid())) {
                    agents.add(agent);
                }
            }
        }

        return agents;
    }

    public List<CaseInsensitiveString> pipelinesFor(final CaseInsensitiveString environmentName) {
        return environments.named(environmentName).getPipelineNames();
    }

    public List<CaseInsensitiveString> environmentNames() {
        return environments.names();
    }

    public Set<EnvironmentConfig> getEnvironments() {
        return new HashSet<>(environments);
    }

    public Set<String> environmentsFor(String uuid) {
        return environments.environmentsForAgent(uuid);
    }

    public Set<EnvironmentConfig> environmentConfigsFor(String uuid) {
        return environments.environmentConfigsForAgent(uuid);
    }

    public EnvironmentConfig named(String envName) {
        return environments.named(new CaseInsensitiveString(envName));
    }

    public EnvironmentConfig getEnvironmentConfig(String envName) {
        return environments.named(new CaseInsensitiveString(envName));
    }

    public EnvironmentConfig getEnvironmentForEdit(String envName) {
        return cloner.deepClone(goConfigService.getConfigForEditing().getEnvironments().find(new CaseInsensitiveString(envName)));
    }

    public List<EnvironmentConfig> getAllLocalEnvironments() {
        return environmentNames().stream()
                                 .map(environmentName -> EnvironmentConfigService.this.getEnvironmentForEdit(environmentName.toString()))
                                 .collect(Collectors.toList());
    }

    public List<EnvironmentConfig> getAllMergedEnvironments() {
        return environmentNames().stream()
                                 .map(env -> EnvironmentConfigService.this.getMergedEnvironmentforDisplay(env.toString(), new HttpLocalizedOperationResult()).getConfigElement())
                                 .collect(Collectors.toList());
    }

    public List<EnvironmentViewModel> listAllMergedEnvironments() {
        ArrayList<EnvironmentViewModel> environmentViewModels = new ArrayList<>();
        List<EnvironmentConfig> allMergedEnvironments = getAllMergedEnvironments();
        for (EnvironmentConfig environmentConfig : allMergedEnvironments) {
            environmentViewModels.add(new EnvironmentViewModel(environmentConfig));
        }
        return environmentViewModels;
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
        List<EnvironmentPipelineModel> pipelines = new ArrayList<>();
        for (EnvironmentPipelineConfig pipelineConfig : envConfig.getRemotePipelines()) {
            String pipelineName = CaseInsensitiveString.str(pipelineConfig.getName());
            if (securityService.hasViewPermissionForPipeline(user, pipelineName)) {
                pipelines.add(new EnvironmentPipelineModel(pipelineName, CaseInsensitiveString.str(envConfig.name())));
            }
        }
        Collections.sort(pipelines);
        return pipelines;
    }

    private List<EnvironmentPipelineModel> getAllPipelinesForUser(Username user, List<PipelineConfig> pipelineConfigs) {
        List<EnvironmentPipelineModel> pipelines = new ArrayList<>();
        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
            if (securityService.hasViewPermissionForPipeline(user, pipelineName)) {
                EnvironmentConfig environment = environments.findEnvironmentForPipeline(new CaseInsensitiveString(pipelineName));
                if (environment != null) {
                    pipelines.add(new EnvironmentPipelineModel(pipelineName, CaseInsensitiveString.str(environment.name())));
                } else {
                    pipelines.add(new EnvironmentPipelineModel(pipelineName));
                }
            }
        }
        Collections.sort(pipelines);
        return pipelines;
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
                                 List<String> agentsToAdd, List<String> agentsToRemove, List<EnvironmentVariableConfig> envVarsToAdd,
                                 List<String> envVarsToRemove, final Username username, final HttpLocalizedOperationResult result) {
        String failedActionErrMsg = "Failed to update environment '" + envConfig.name() + "'.";
        PatchEnvironmentCommand patchEnvCmd = new PatchEnvironmentCommand(goConfigService, envConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, envVarsToAdd, envVarsToRemove, username, failedActionErrMsg, result);
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

    @Override
    public void agentChanged(AgentChangedEvent event) {
        Agent oldAgent = event.getOldAgent();
        Agent newAgent = event.getNewAgent();

        List<String> oldEnvs = oldAgent.getEnvironmentsAsList();
        List<String> newEnvs = newAgent.getEnvironmentsAsList();

        List<String> removedEnvs = oldEnvs.stream().filter(env -> !newEnvs.contains(env)).collect(Collectors.toList());
        List<String> addedEnvs = newEnvs.stream().filter(env -> !oldEnvs.contains(env)).collect(Collectors.toList());

        removeAgentFromEnvs(oldAgent, removedEnvs);
        addAgentToEnvs(newAgent.getUuid(), addedEnvs);

        matchers = this.environments.matchers();
    }

    @Override
    public void agentDeleted(Agent agent) {
        List<String> envs = agent.getEnvironmentsAsList();
        removeAgentFromEnvs(agent, envs);
        matchers = this.environments.matchers();
    }

    public void syncEnvironmentsFromConfig(EnvironmentsConfig environments){
        this.environments = environments;
        matchers = this.environments.matchers();
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        syncEnvironmentsFromConfig(newCruiseConfig.getEnvironments());
        syncAgentsFromDB();
    }

    public void syncAgentsFromDB() {
        agentService.agents().forEach(agent -> {
            String agentEnvironments = agent.getEnvironments();
            if (agentEnvironments != null) {
                Arrays.stream(agentEnvironments.split(",")).forEach(env -> {
                    EnvironmentConfig environmentConfig = this.environments.find(new CaseInsensitiveString(env));
                    if (environmentConfig != null && !environmentConfig.hasAgent(agent.getUuid())) {
                        environmentConfig.addAgent(agent.getUuid());
                    }
                });
            }

        });
        matchers = this.environments.matchers();
    }

    private void removeAgentFromEnvs(Agent agent, List<String> envs) {
        envs.forEach(env -> {
            String uuid = agent.getUuid();
            EnvironmentConfig envConfig = this.environments.find(new CaseInsensitiveString(env));
            if(envConfig != null && envConfig.hasAgent(uuid)
                                 && !envConfig.containsAgentRemotely(uuid)){
                envConfig.removeAgent(uuid);
            }
        });
    }

    private void removeAgentFromNonExcludedEnvs(String uuid, List<String> envsToExclude) {
        this.environments.stream().filter(env -> !(envsToExclude.contains(env.name().toString())))
                .forEach(env -> {
                    if(env.hasAgent(uuid)){
                        env.removeAgent(uuid);
                    }
                });
    }

    private void addAgentToEnvs(String agentUuid, List<String> envs) {
        envs.forEach(env -> {
            EnvironmentConfig envConfig = this.environments.find(new CaseInsensitiveString(env));
            if (envConfig != null && !envConfig.hasAgent(agentUuid)) {
                envConfig.addAgent(agentUuid);
            }
        });
    }

    public void syncAssociatedAgentsFromDB(){
        agentService.agents()
                .forEach(agent -> {
                    List<String> associatedEnvNames = agent.getEnvironmentsAsList();
                    addAgentToEnvs(agent.getUuid(), associatedEnvNames);
                    removeAgentFromNonExcludedEnvs(agent.getUuid(), associatedEnvNames);
                });
        matchers = this.environments.matchers();
    }

}
