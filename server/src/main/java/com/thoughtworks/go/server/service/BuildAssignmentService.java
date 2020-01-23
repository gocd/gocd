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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.*;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.server.materials.StaleMaterialsOnBuildCause;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.JobStatusTopic;
import com.thoughtworks.go.server.service.builders.BuilderFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.forAllDo;


/**
 * @understands how to assign work to agents
 */
@Service
public class BuildAssignmentService implements ConfigChangedListener {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BuildAssignmentService.class.getName());
    public static final NoWork NO_WORK = new NoWork();
    public static final String GO_PIPELINE_GROUP_NAME = "GO_PIPELINE_GROUP_NAME";
    public static final String GO_AGENT_RESOURCES = "GO_AGENT_RESOURCES";

    private GoConfigService goConfigService;
    private JobInstanceService jobInstanceService;
    private ScheduleService scheduleService;
    private AgentService agentService;
    private EnvironmentConfigService environmentConfigService;
    private TransactionTemplate transactionTemplate;
    private final ScheduledPipelineLoader scheduledPipelineLoader;

    private List<JobPlan> jobPlans = new ArrayList<>();
    private final UpstreamPipelineResolver resolver;
    private final BuilderFactory builderFactory;
    private MaintenanceModeService maintenanceModeService;
    private final ElasticAgentPluginService elasticAgentPluginService;
    private final SystemEnvironment systemEnvironment;
    private SecretParamResolver secretParamResolver;
    private JobStatusTopic jobStatusTopic;
    private ConsoleService consoleService;

    @Autowired
    public BuildAssignmentService(GoConfigService goConfigService, JobInstanceService jobInstanceService,
                                  ScheduleService scheduleService, AgentService agentService,
                                  EnvironmentConfigService environmentConfigService, TransactionTemplate transactionTemplate,
                                  ScheduledPipelineLoader scheduledPipelineLoader, PipelineService pipelineService,
                                  BuilderFactory builderFactory,
                                  MaintenanceModeService maintenanceModeService, ElasticAgentPluginService elasticAgentPluginService,
                                  SystemEnvironment systemEnvironment, SecretParamResolver secretParamResolver, JobStatusTopic jobStatusTopic,
                                  ConsoleService consoleService) {
        this.goConfigService = goConfigService;
        this.jobInstanceService = jobInstanceService;
        this.scheduleService = scheduleService;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.transactionTemplate = transactionTemplate;
        this.scheduledPipelineLoader = scheduledPipelineLoader;
        this.resolver = pipelineService;
        this.builderFactory = builderFactory;
        this.maintenanceModeService = maintenanceModeService;
        this.elasticAgentPluginService = elasticAgentPluginService;
        this.systemEnvironment = systemEnvironment;
        this.secretParamResolver = secretParamResolver;
        this.jobStatusTopic = jobStatusTopic;
        this.consoleService = consoleService;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }


    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                LOGGER.info("[Configuration Changed] Removing deleted jobs for pipeline {}.", pipelineConfig.name());

                synchronized (BuildAssignmentService.this) {
                    List<JobPlan> jobsToRemove;
                    if (goConfigService.hasPipelineNamed(pipelineConfig.name())) {
                        jobsToRemove = getMismatchingJobPlansFromUpdatedPipeline(pipelineConfig, jobPlans);
                    } else {
                        jobsToRemove = getAllJobPlansFromDeletedPipeline(pipelineConfig, jobPlans);
                    }

                    IterableUtils.forEach(jobsToRemove, o -> removeJob(o));
                }
            }
        };
    }

    private List<JobPlan> getMismatchingJobPlansFromUpdatedPipeline(PipelineConfig pipelineConfig, List<JobPlan> allJobPlans) {
        List<JobPlan> jobsToRemove = new ArrayList<>();

        for (JobPlan jobPlan : allJobPlans) {
            if (pipelineConfig.name().equals(new CaseInsensitiveString(jobPlan.getPipelineName()))) {
                StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(jobPlan.getStageName()));
                if (stageConfig != null) {
                    JobConfig jobConfig = stageConfig.jobConfigByConfigName(new CaseInsensitiveString(jobPlan.getName()));
                    if (jobConfig == null) {
                        jobsToRemove.add(jobPlan);
                    }
                } else {
                    jobsToRemove.add(jobPlan);
                }
            }
        }

        return jobsToRemove;
    }

    private List<JobPlan> getAllJobPlansFromDeletedPipeline(PipelineConfig pipelineConfig, List<JobPlan> allJobPlans) {
        return allJobPlans.stream()
                .filter(jobPlan -> new CaseInsensitiveString(jobPlan.getPipelineName()).equals(pipelineConfig.name()))
                .collect(toList());
    }

    public Work assignWorkToAgent(AgentIdentifier agent) {
        return assignWorkToAgent(agentService.findAgentAndRefreshStatus(agent.getUuid()));
    }

    Work assignWorkToAgent(final AgentInstance agent) {
        if (!agent.isRegistered()) {
            return new UnregisteredAgentWork(agent.getUuid());
        }

        if (agent.isDisabled()) {
            return new DeniedAgentWork(agent.getUuid());
        }

        synchronized (this) {
//          check to ensure agent is not disabled after entering the synchronized block
            if (agent.isDisabled()) {
                return new DeniedAgentWork(agent.getUuid());
            }
            //check if agent already has assigned build, if so, reschedule it
            scheduleService.rescheduleAbandonedBuildIfNecessary(agent.getAgentIdentifier());
            final JobPlan job = findMatchingJob(agent);
            if (job != null) {
                Work buildWork = createWork(agent, job);
                AgentBuildingInfo buildingInfo = new AgentBuildingInfo(job.getIdentifier().buildLocatorForDisplay(),
                        job.getIdentifier().buildLocator());
                agentService.building(agent.getUuid(), buildingInfo);
                LOGGER.info("[Agent Assignment] Assigned job [{}] to agent [{}]", job.getIdentifier(), agent.getAgent().getAgentIdentifier());

                return buildWork;
            }
        }
        return NO_WORK;
    }

    JobPlan findMatchingJob(AgentInstance agent) {
        List<JobPlan> filteredJobPlans = environmentConfigService.filterJobsByAgent(jobPlans, agent.getUuid());
        JobPlan match = null;
        if (!agent.isElastic()) {
            match = agent.firstMatching(filteredJobPlans);
        } else {
            for (JobPlan jobPlan : filteredJobPlans) {
                if (jobPlan.requiresElasticAgent() && elasticAgentPluginService.shouldAssignWork(agent.elasticAgentMetadata(), environmentConfigService.envForPipeline(jobPlan.getPipelineName()), jobPlan.getElasticProfile(), jobPlan.getClusterProfile(), jobPlan.getIdentifier())) {
                    match = jobPlan;
                    break;
                }
            }
        }
        if (match != null) {
            jobPlans.remove(match);
        }
        return match;
    }

    public void onTimer() {
        if (maintenanceModeService.isMaintenanceMode()) {
            LOGGER.debug("[Maintenance Mode] GoCD server is in 'maintenance' mode, skip checking build assignments");
            return;
        }

        reloadJobPlans();
    }

    private void reloadJobPlans() {
        synchronized (this) {
            if (jobPlans == null) {
                jobPlans = jobInstanceService.orderedScheduledBuilds();
                elasticAgentPluginService.createAgentsFor(jobPlans, new ArrayList<>());
            } else {
                List<JobPlan> old = jobPlans;
                List<JobPlan> newPlan = jobInstanceService.orderedScheduledBuilds();
                jobPlans = newPlan;
                elasticAgentPluginService.createAgentsFor(old, newPlan);
            }
        }
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        LOGGER.info("[Configuration Changed] Removing jobs for pipelines that no longer exist in configuration.");
        synchronized (this) {
            List<JobPlan> jobsToRemove = new ArrayList<>();
            for (JobPlan jobPlan : jobPlans) {
                if (!newCruiseConfig.hasBuildPlan(new CaseInsensitiveString(jobPlan.getPipelineName()), new CaseInsensitiveString(jobPlan.getStageName()), jobPlan.getName(), true)) {
                    jobsToRemove.add(jobPlan);
                }
            }
            forAllDo(jobsToRemove, o -> removeJob((JobPlan) o));
        }
    }

    private void removeJobIfNotPresentInCruiseConfig(CruiseConfig newCruiseConfig, JobPlan jobPlan) {
        if (!newCruiseConfig.hasBuildPlan(new CaseInsensitiveString(jobPlan.getPipelineName()), new CaseInsensitiveString(jobPlan.getStageName()), jobPlan.getName(), true)) {
            removeJob(jobPlan);
        }
    }

    private void removeJob(JobPlan jobPlan) {
        try {
            jobPlans.remove(jobPlan);
            LOGGER.info("Removing job plan {} that no longer exists in the config", jobPlan);
            JobInstance instance = jobInstanceService.buildByIdWithTransitions(jobPlan.getJobId());
            //#2846 - remove this hack
            instance.setIdentifier(jobPlan.getIdentifier());

            scheduleService.cancelJob(instance);
            LOGGER.info("Successfully removed job plan {} that no longer exists in the config", jobPlan);
        } catch (Exception e) {
            LOGGER.warn("Unable to remove plan {} from queue that no longer exists in the config", jobPlan);
        }
    }

    private Work createWork(final AgentInstance agent, final JobPlan job) {
        try {
            return (Work) transactionTemplate.transactionSurrounding(() -> {
                final String agentUuid = agent.getUuid();

                //TODO: Use fullPipeline and get the Stage from it?
                final Pipeline pipeline;
                try {
                    pipeline = scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(job.getJobId());
                } catch (StaleMaterialsOnBuildCause e) {
                    // Detailed error msg is part of the exception object and it would be logged. Hence not adding msg while logging.
                    LOGGER.error("", e);
                    return NO_WORK;
                }

                List<Task> tasks = goConfigService.tasksForJob(pipeline.getName(), job.getIdentifier().getStageName(), job.getName());
                final List<Builder> builders = builderFactory.buildersForTasks(pipeline, tasks, resolver);

                return transactionTemplate.execute(status -> {
                    if (scheduleService.updateAssignedInfo(agentUuid, job)) {
                        return NO_WORK;
                    }

                    final EnvironmentVariableContext environmentVariableContext = buildEnvVarContext(job.getIdentifier().getPipelineName());

                    // Agent may have a NULL "resources"
                    if (CollectionUtils.isNotEmpty(agent.getResourceConfigs())) {
                        // Users relying on this env. var. can test for its existence rather than checking for an empty string
                        environmentVariableContext.setProperty(GO_AGENT_RESOURCES, agent.getResourceConfigs().getCommaSeparatedResourceNames(), false);
                    }

                    final ArtifactStores requiredArtifactStores = goConfigService.artifactStores().getArtifactStores(getArtifactStoreIdsRequiredByArtifactPlans(job.getArtifactPlans()));
                    BuildAssignment buildAssignment = BuildAssignment.create(job, pipeline.getBuildCause(), builders, pipeline.defaultWorkingFolder(), environmentVariableContext, requiredArtifactStores);

                    secretParamResolver.resolve(buildAssignment);

                    return new BuildWork(buildAssignment, systemEnvironment.consoleLogCharset());
                });
            });
        } catch (RecordNotFoundException e) {
            removeJobIfNotPresentInCruiseConfig(goConfigService.getCurrentConfig(), job);
            throw e;
        } catch (SecretResolutionFailureException e) {
            JobInstance instance = jobInstanceService.buildById(job.getJobId());
            logSecretsResolutionFailure(job.getIdentifier(), e);
            scheduleService.failJob(instance);
            jobStatusTopic.post(new JobStatusMessage(job.getIdentifier(), instance.getState(), agent.getUuid()));
            throw e;
        } catch (RulesViolationException e) {
            JobInstance instance = jobInstanceService.buildById(job.getJobId());
            logRulesViolation(job.getIdentifier(), e);
            scheduleService.failJob(instance);
            jobStatusTopic.post(new JobStatusMessage(job.getIdentifier(), instance.getState(), agent.getUuid()));
            throw e;
        }
    }

    EnvironmentVariableContext buildEnvVarContext(String pipelineName) {
        String pipelineGroupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext(GO_PIPELINE_GROUP_NAME, pipelineGroupName);

        EnvironmentConfig environmentForPipeline = environmentConfigService.environmentForPipeline(pipelineName);
        if (environmentForPipeline == null) {
            return environmentVariableContext;
        }

        secretParamResolver.resolve(environmentForPipeline);

        environmentVariableContext.setProperty(GO_ENVIRONMENT_NAME, CaseInsensitiveString.str(environmentForPipeline.name()), false);
        environmentForPipeline.getVariables().forEach(variable -> {
            environmentVariableContext.setProperty(variable.getName(), variable.valueForCommandline(), variable.isSecure() || variable.hasSecretParams());
        });
        return environmentVariableContext;
    }

    private void logSecretsResolutionFailure(JobIdentifier jobIdentifier, SecretResolutionFailureException e) {
        try {
            final String description = format("\nJob for pipeline '%s' failed due to errors while resolving secret params.", jobIdentifier.buildLocator());
            consoleService.appendToConsoleLog(jobIdentifier, description);
            consoleService.appendToConsoleLog(jobIdentifier, format("\nReason: %s\n", e.getMessage()));
        } catch (IllegalArtifactLocationException | IOException e1) {
            LOGGER.error(e1.getMessage(), e1);
        }
    }

    private void logRulesViolation(JobIdentifier jobIdentifier, RulesViolationException e) {
        try {
            final String description = format("\nJob for pipeline '%s' failed due to errors: %s", jobIdentifier.buildLocator(), e.getMessage());
            consoleService.appendToConsoleLog(jobIdentifier, description);
        } catch (IllegalArtifactLocationException | IOException e1) {
            LOGGER.error(e1.getMessage(), e1);
        }
    }

    private Set<String> getArtifactStoreIdsRequiredByArtifactPlans(List<ArtifactPlan> artifactPlans) {
        final Set<String> storeIds = new HashSet<>();
        for (ArtifactPlan artifactPlan : artifactPlans) {
            if (artifactPlan.getArtifactPlanType() == ArtifactPlanType.external) {
                storeIds.add((String) artifactPlan.getPluggableArtifactConfiguration().get("storeId"));
            }
        }
        return storeIds;
    }

    List<JobPlan> jobPlans() {
        return jobPlans;
    }
}
