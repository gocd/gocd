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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.config.update.AddEnvironmentCommand;
import com.thoughtworks.go.config.update.DeleteEnvironmentCommand;
import com.thoughtworks.go.config.update.UpdateEnvironmentCommand;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @understands grouping of agents and pipelines within an environment
 */
@Service
public class EnvironmentConfigService implements ConfigChangedListener {

    public final GoConfigService goConfigService;
    private final SecurityService securityService;

    private EnvironmentsConfig environments;
    private EnvironmentPipelineMatchers matchers;
    private static final Cloner cloner = new Cloner();

    @Autowired
    public EnvironmentConfigService(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(new EntityConfigChangedListener<Agents>() {
            @Override
            public void onEntityConfigChange(Agents entity) {
                sync(goConfigService.getEnvironments());
            }
        });

        goConfigService.register(new EntityConfigChangedListener<EnvironmentConfig>() {
            @Override
            public void onEntityConfigChange(EnvironmentConfig entity) {
                sync(goConfigService.getEnvironments());
            }
        });
    }

    public void sync(EnvironmentsConfig environments) {
        matchers = environments.matchers();
        this.environments = environments;
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        sync(newCruiseConfig.getEnvironments());
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

    public String envForJob(String jobPipelineName) {
        for (EnvironmentPipelineMatcher matcher : matchers) {
            if (matcher.hasPipeline(jobPipelineName)) {
                return CaseInsensitiveString.str(matcher.name());
            }
        }
        return null;
    }

    public void enhanceEnvironmentVariables(BuildAssignment assignment) {
        EnvironmentConfig environment = environments.findEnvironmentForPipeline(new CaseInsensitiveString(assignment.getPlan().getPipelineName()));
        if (environment != null) {
            assignment.enhanceEnvironmentVariables(environment.createEnvironmentContext());
        }
    }

    public Agents agentsForPipeline(final CaseInsensitiveString pipelineName) {
        Agents configs = new Agents();
        if (environments.isPipelineAssociatedWithAnyEnvironment(pipelineName)) {
            EnvironmentConfig forPipeline = environments.findEnvironmentForPipeline(pipelineName);
            for (EnvironmentAgentConfig environmentAgentConfig : forPipeline.getAgents()) {
                configs.add(goConfigService.agentByUuid(environmentAgentConfig.getUuid()));
            }

        } else {
            for (AgentConfig agentConfig : goConfigService.agents()) {
                if (!environments.isAgentUnderEnvironment(agentConfig.getUuid())) {
                    configs.add(agentConfig);
                }
            }
        }
        return configs;
    }

    public List<CaseInsensitiveString> pipelinesFor(final CaseInsensitiveString environmentName) throws NoSuchEnvironmentException {
        return environments.named(environmentName).getPipelineNames();
    }

    public List<CaseInsensitiveString> environmentNames() {
        return environments.names();
    }

    public Set<EnvironmentConfig> getEnvironments() {
        Set<EnvironmentConfig> environmentConfigs = new HashSet<>();
        environmentConfigs.addAll(environments);
        return environmentConfigs;
    }

    public Set<String> environmentsFor(String uuid) {
        return environments.environmentsForAgent(uuid);
    }

    public void modifyEnvironments(List<AgentInstance> agents, List<TriStateSelection> selections) {
        goConfigService.modifyEnvironments(agents, selections);
    }

    public EnvironmentConfig named(String environmentName) throws NoSuchEnvironmentException {
        return environments.named(new CaseInsensitiveString(environmentName));
    }

    public EnvironmentConfig getEnvironmentConfig(String environmentName) throws NoSuchEnvironmentException {
        return environments.named(new CaseInsensitiveString(environmentName));
    }

    public ConfigElementForEdit<EnvironmentConfig> forEdit(String environmentName, HttpLocalizedOperationResult result) {
        ConfigElementForEdit<EnvironmentConfig> edit = null;
        try {
            CruiseConfig config = goConfigService.getMergedConfigForEditing();
            EnvironmentConfig env = config.getEnvironments().named(new CaseInsensitiveString(environmentName));
            edit = new ConfigElementForEdit<>(cloner.deepClone(env), config.getMd5());
        } catch (NoSuchEnvironmentException e) {
            result.badRequest(LocalizedMessage.string("ENV_NOT_FOUND", environmentName));
        }
        return edit;
    }

    public void createEnvironment(final BasicEnvironmentConfig environmentConfig, final Username user, final HttpLocalizedOperationResult result) {
        Localizable.CurryableLocalizable actionFailed = LocalizedMessage.string("ENV_ADD_FAILED");
        AddEnvironmentCommand addEnvironmentCommand = new AddEnvironmentCommand(goConfigService, environmentConfig, user, actionFailed, result);
        update(addEnvironmentCommand, environmentConfig, user, result, actionFailed);
    }

    @Deprecated
    private void editEnvironments(EditEnvironments edit, Username user, LocalizedOperationResult result,
                                  Localizable noPermissionMessage, Localizable.CurryableLocalizable actionFailedMessage) {
        if (!securityService.isUserAdmin(user)) {
            result.unauthorized(noPermissionMessage, HealthStateType.unauthorised());
            return;
        }
        try {
            edit.performEdit();
        } catch (Exception e) {
            result.badRequest(actionFailedMessage.addParam(e.getMessage()));
        }
    }

    public List<EnvironmentPipelineModel> getAllLocalPipelinesForUser(Username user) {
        List<PipelineConfig> pipelineConfigs = goConfigService.getAllLocalPipelineConfigs();
        return getAllPipelinesForUser(user, pipelineConfigs);
    }

    public List<EnvironmentPipelineModel> getAllRemotePipelinesForUserInEnvironment(Username user, EnvironmentConfig environment) {
        List<EnvironmentPipelineModel> pipelines = new ArrayList<>();
        for (EnvironmentPipelineConfig pipelineConfig : environment.getRemotePipelines()) {
            String pipelineName = CaseInsensitiveString.str(pipelineConfig.getName());
            if (securityService.hasViewPermissionForPipeline(user, pipelineName)) {
                pipelines.add(new EnvironmentPipelineModel(pipelineName, CaseInsensitiveString.str(environment.name())));
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

    @Deprecated //No need of md5 (used in environments-edit page)
    public HttpLocalizedOperationResult updateEnvironment(final String named, final EnvironmentConfig newDefinition, final Username username, final String md5) {
        final HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Localizable noPermission = LocalizedMessage.string("NO_PERMISSION_TO_UPDATE_ENVIRONMENT", named, username.getDisplayName());
        Localizable.CurryableLocalizable actionFailed = LocalizedMessage.string("ENV_UPDATE_FAILED", named);
        editEnvironments(new EditEnvironments() {
            public void performEdit() {
                ConfigSaveState configSaveState = goConfigService.updateEnvironment(named, newDefinition, username, md5);
                if (ConfigSaveState.MERGED.equals(configSaveState)) {
                    result.setMessage(LocalizedMessage.composite(LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS", named), LocalizedMessage.string("CONFIG_MERGED")));
                } else if (ConfigSaveState.UPDATED.equals(configSaveState)) {
                    result.setMessage(LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS", named));
                }
            }
        }, username, result, noPermission, actionFailed);
        return result;
    }

    public void updateEnvironment(final EnvironmentConfig oldEnvironmentConfig, final EnvironmentConfig newEnvironmentConfig, final Username username, final HttpLocalizedOperationResult result) {
        Localizable.CurryableLocalizable actionFailed = LocalizedMessage.string("ENV_UPDATE_FAILED", oldEnvironmentConfig.name());
        UpdateEnvironmentCommand updateEnvironmentCommand = new UpdateEnvironmentCommand(goConfigService, oldEnvironmentConfig, newEnvironmentConfig, username, actionFailed, result);
        update(updateEnvironmentCommand, oldEnvironmentConfig, username, result, actionFailed);
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS", oldEnvironmentConfig.name()));
        }
    }

    public void deleteEnvironment(final EnvironmentConfig environmentConfig, final Username username, final HttpLocalizedOperationResult result) {
        String environmentName = environmentConfig.name().toString();
        Localizable.CurryableLocalizable actionFailed = LocalizedMessage.string("ENV_DELETE_FAILED", environmentName);
        DeleteEnvironmentCommand deleteEnvironmentCommand = new DeleteEnvironmentCommand(goConfigService, environmentConfig, username, actionFailed, result);
        update(deleteEnvironmentCommand, environmentConfig, username, result, actionFailed);
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("ENVIRONMENT_DELETE_SUCCESSFUL", environmentName));
        }
    }

    @Deprecated
    public interface EditEnvironments {
        void performEdit();
    }

    private void update(EntityConfigUpdateCommand command, EnvironmentConfig config, Username currentUser, HttpLocalizedOperationResult result, Localizable.CurryableLocalizable actionFailed) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if ((e instanceof GoConfigInvalidException) && !result.hasMessage()) {
                result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", config.getClass().getAnnotation(ConfigTag.class).value(), config.name(), e.getMessage()));
            } else if (!result.hasMessage()) {
                result.badRequest(actionFailed.addParam(e.getMessage()));
            }
        }
    }
}
