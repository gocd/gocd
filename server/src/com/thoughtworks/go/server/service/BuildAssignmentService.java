/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.*;
import com.thoughtworks.go.server.domain.BuildComposer;
import com.thoughtworks.go.server.materials.StaleMaterialsOnBuildCause;
import com.thoughtworks.go.server.service.builders.BuilderFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.websocket.Agent;
import com.thoughtworks.go.server.websocket.AgentRemoteHandler;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.util.URLService;
import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;
import com.thoughtworks.go.websocket.MessageEncoding;
import org.apache.commons.collections.Closure;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileNameUrl;
import static org.apache.commons.collections.CollectionUtils.forAllDo;


/**
 * @understands how to assign work to agents
 */
@Service
public class BuildAssignmentService implements ConfigChangedListener {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BuildAssignmentService.class.getName());
    public static final NoWork NO_WORK = new NoWork();

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
    private AgentRemoteHandler agentRemoteHandler;
    private final ElasticAgentPluginService elasticAgentPluginService;
    private final TimeProvider timeProvider;

    @Autowired
    public BuildAssignmentService(GoConfigService goConfigService, JobInstanceService jobInstanceService, ScheduleService scheduleService,
                                  AgentService agentService, EnvironmentConfigService environmentConfigService,
                                  TransactionTemplate transactionTemplate, ScheduledPipelineLoader scheduledPipelineLoader, PipelineService pipelineService, BuilderFactory builderFactory,
                                  AgentRemoteHandler agentRemoteHandler,
                                  ElasticAgentPluginService elasticAgentPluginService, TimeProvider timeProvider) {
        this.goConfigService = goConfigService;
        this.jobInstanceService = jobInstanceService;
        this.scheduleService = scheduleService;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.transactionTemplate = transactionTemplate;
        this.scheduledPipelineLoader = scheduledPipelineLoader;
        this.resolver = pipelineService;
        this.builderFactory = builderFactory;
        this.agentRemoteHandler = agentRemoteHandler;
        this.elasticAgentPluginService = elasticAgentPluginService;
        this.timeProvider = timeProvider;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                LOGGER.info(String.format("[Configuration Changed] Removing deleted jobs for pipeline %s.", pipelineConfig.name()));

                synchronized (BuildAssignmentService.this) {
                    List<JobPlan> jobsToRemove = new ArrayList<>();
                    for (JobPlan jobPlan : jobPlans) {
                        if (pipelineConfig.name().equals(new CaseInsensitiveString(jobPlan.getPipelineName()))) {
                            StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(jobPlan.getStageName()));
                            if (stageConfig != null) {
                                JobConfig jobConfig = stageConfig.jobConfigByConfigName(new CaseInsensitiveString(jobPlan.getName()));
                                if(jobConfig == null){
                                    jobsToRemove.add(jobPlan);
                                }
                            } else {
                                jobsToRemove.add(jobPlan);
                            }
                        }
                    }
                    forAllDo(jobsToRemove, new Closure() {
                        @Override
                        public void execute(Object o) {
                            removeJob((JobPlan) o);
                        }
                    });
                }
            }
        };
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
            //check if agent already has assigned build, if so, reschedule it
            scheduleService.rescheduleAbandonedBuildIfNecessary(agent.getAgentIdentifier());
            final JobPlan job = findMatchingJob(agent);
            if (job != null) {
                Work buildWork = createWork(agent, job);
                AgentBuildingInfo buildingInfo = new AgentBuildingInfo(job.getIdentifier().buildLocatorForDisplay(),
                        job.getIdentifier().buildLocator());

                if (agent.isElastic()) {
                    if (!elasticAgentPluginService.shouldAssignWork(agent.elasticAgentMetadata(), environmentConfigService.envForPipeline(job.getPipelineName()), job.getElasticProfile())) {
                        return NO_WORK;
                    }
                }

                agentService.building(agent.getUuid(), buildingInfo);
                LOGGER.info("[Agent Assignment] Assigned job [{}] to agent [{}]", job.getIdentifier(), agent.agentConfig().getAgentIdentifier());
                return buildWork;
            }
        }

        return NO_WORK;
    }

    private JobPlan findMatchingJob(AgentInstance agent) {
        List<JobPlan> filteredJobPlans = environmentConfigService.filterJobsByAgent(jobPlans, agent.getUuid());
        JobPlan match = agent.firstMatching(filteredJobPlans);
        jobPlans.remove(match);
        return match;
    }

    public void onTimer() {
        reloadJobPlans();
        matchingJobForRegisteredAgents();
    }

    private void reloadJobPlans() {
        synchronized (this) {
            if (jobPlans == null) {
                jobPlans = jobInstanceService.orderedScheduledBuilds();
                elasticAgentPluginService.createAgentsFor(jobPlans, new ArrayList<JobPlan>());
            } else {
                List<JobPlan> old = jobPlans;
                List<JobPlan> newPlan = jobInstanceService.orderedScheduledBuilds();
                jobPlans = newPlan;
                elasticAgentPluginService.createAgentsFor(old, newPlan);
            }
        }
    }

    private void matchingJobForRegisteredAgents() {
        Map<String, Agent> agents = agentRemoteHandler.connectedAgents();
        if (agents.isEmpty()) {
            return;
        }
        Long start = System.currentTimeMillis();
        for (Map.Entry<String, Agent> entry : agents.entrySet()) {
            String agentUUId = entry.getKey();
            Agent agent = entry.getValue();
            AgentInstance agentInstance = agentService.findAgentAndRefreshStatus(agentUUId);
            if (!agentInstance.isRegistered()) {
                agent.send(new Message(Action.reregister));
                continue;
            }
            if (agentInstance.isDisabled() || !agentInstance.isIdle()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Ignore agent [{}] that is {}", agentInstance.getAgentIdentifier().toString(), agentInstance.getStatus());
                }
                continue;
            }
            Work work = assignWorkToAgent(agentInstance);
            if (work != NO_WORK) {
                if(agentInstance.getSupportsBuildCommandProtocol()) {
                    BuildSettings buildSettings = createBuildSettings(((BuildWork) work).getAssignment());
                    agent.send(new Message(Action.build, MessageEncoding.encodeData(buildSettings)));
                } else {
                    agent.send(new Message(Action.assignWork, MessageEncoding.encodeWork(work)));
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Matching {} agents with {} jobs took: {}ms", agents.size(), jobPlans.size(), System.currentTimeMillis() - start);
        }
    }

    private BuildSettings createBuildSettings(BuildAssignment assignment) {
        URLService urlService = new URLService(""); // generate path only url
        JobPlan plan = assignment.getPlan();
        JobIdentifier jobIdentifier = plan.getIdentifier();

        BuildSettings buildSettings = new BuildSettings();
        buildSettings.setBuildId(String.valueOf(jobIdentifier.getBuildId()));
        buildSettings.setBuildLocatorForDisplay(jobIdentifier.buildLocatorForDisplay());
        buildSettings.setBuildLocator(jobIdentifier.buildLocator());
        buildSettings.setBuildCommand(new BuildComposer(assignment).compose());
        buildSettings.setConsoleUrl(urlService.getUploadUrlOfAgent(plan.getIdentifier(), getConsoleOutputFolderAndFileNameUrl()));
        buildSettings.setArtifactUploadBaseUrl(urlService.getUploadBaseUrlOfAgent(plan.getIdentifier()));
        buildSettings.setPropertyBaseUrl(urlService.getPropertiesUrl(plan.getIdentifier(), ""));
        return buildSettings;
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        LOGGER.info("[Configuration Changed] Removing jobs for pipelines that no longer exist in configuration.");
        synchronized (this) {
            List<JobPlan> jobsToRemove = new ArrayList<>();
            for (JobPlan jobPlan : jobPlans) {
                if (!newCruiseConfig.hasBuildPlan(new CaseInsensitiveString(jobPlan.getPipelineName()), new CaseInsensitiveString(jobPlan.getStageName()), jobPlan.getName(), true)) {
                    jobsToRemove.add(jobPlan);
                }
            }
            forAllDo(jobsToRemove, new Closure() {
                @Override
                public void execute(Object o) {
                    removeJob((JobPlan) o);
                }
            });
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
            return (Work) transactionTemplate.transactionSurrounding(new TransactionTemplate.TransactionSurrounding<RuntimeException>() {
                public Object surrounding() {
                    final String agentUuid = agent.getUuid();

                    //TODO: Use fullPipeline and get the Stage from it?
                    final Pipeline pipeline;
                    try {
                        pipeline = scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(job.getJobId());
                    } catch (StaleMaterialsOnBuildCause e) {
                        return NO_WORK;
                    }

                    List<Task> tasks = goConfigService.tasksForJob(pipeline.getName(), job.getIdentifier().getStageName(), job.getName());
                    final List<Builder> builders = builderFactory.buildersForTasks(pipeline, tasks, resolver);

                    return transactionTemplate.execute(new TransactionCallback() {
                        public Object doInTransaction(TransactionStatus status) {
                            if (scheduleService.updateAssignedInfo(agentUuid, job)) {
                                return NO_WORK;
                            }

                            BuildAssignment buildAssignment = BuildAssignment.create(job, pipeline.getBuildCause(), builders, pipeline.defaultWorkingFolder());
                            environmentConfigService.enhanceEnvironmentVariables(buildAssignment);
                            return new BuildWork(buildAssignment);
                        }
                    });
                }
            });

        } catch (PipelineNotFoundException e) {
            removeJobIfNotPresentInCruiseConfig(goConfigService.getCurrentConfig(), job);
            throw e;
        }

    }
}
