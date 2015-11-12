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
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.listener.PipelineConfigChangedListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.*;
import com.thoughtworks.go.server.materials.StaleMaterialsOnBuildCause;
import com.thoughtworks.go.server.service.builders.BuilderFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.collections.Closure;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.forAllDo;


/**
 * @understands how to assign work to agents
 */
@Service
public class BuildAssignmentService implements PipelineConfigChangedListener {
    private static final Logger LOGGER = Logger.getLogger(BuildAssignmentService.class);
    public static final NoWork NO_WORK = new NoWork();

    private GoConfigService goConfigService;
    private JobInstanceService jobInstanceService;
    private ScheduleService scheduleService;
    private AgentService agentService;
    private EnvironmentConfigService environmentConfigService;
    private TimeProvider timeProvider;
    private TransactionTemplate transactionTemplate;
    private final ScheduledPipelineLoader scheduledPipelineLoader;

    private List<JobPlan> jobPlans = new ArrayList<JobPlan>();
    private final UpstreamPipelineResolver resolver;
    private final BuilderFactory builderFactory;

    @Autowired
    public BuildAssignmentService(GoConfigService goConfigService, JobInstanceService jobInstanceService, ScheduleService scheduleService,
                                  AgentService agentService, EnvironmentConfigService environmentConfigService, TimeProvider timeProvider,
                                  TransactionTemplate transactionTemplate, ScheduledPipelineLoader scheduledPipelineLoader, PipelineService pipelineService, BuilderFactory builderFactory) {
        this.goConfigService = goConfigService;
        this.jobInstanceService = jobInstanceService;
        this.scheduleService = scheduleService;
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.timeProvider = timeProvider;
        this.transactionTemplate = transactionTemplate;
        this.scheduledPipelineLoader = scheduledPipelineLoader;
        this.resolver = pipelineService;
        this.builderFactory = builderFactory;
    }

    public void initialize() {
        goConfigService.register(this);
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
                agentService.building(agent.getUuid(), buildingInfo);
                LOGGER.info(format("[Agent Assignment] Assigned job [%s] to agent [%s]", job.getIdentifier(), agent.agentConfig().getAgentIdentifier()));
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
    }

    private void reloadJobPlans() {
        synchronized (this) {
            jobPlans = jobInstanceService.orderedScheduledBuilds();
        }
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        LOGGER.info(String.format("[Configuration Changed] Removing jobs for pipelines that no longer exist in configuration."));
        synchronized (this) {
            List<JobPlan> jobsToRemove = new ArrayList<JobPlan>();
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

    @Override
    public void onPipelineConfigChange(PipelineConfig pipelineConfig, String group) {
        LOGGER.info(String.format("[Configuration Changed] Removing deleted jobs for pipeline %s.", pipelineConfig.name()));

        synchronized (this) {
            List<JobPlan> jobsToRemove = new ArrayList<JobPlan>();
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

    private void removeJobIfNotPresentInCruiseConfig(CruiseConfig newCruiseConfig, JobPlan jobPlan) {
        if (!newCruiseConfig.hasBuildPlan(new CaseInsensitiveString(jobPlan.getPipelineName()), new CaseInsensitiveString(jobPlan.getStageName()), jobPlan.getName(), true)) {
            removeJob(jobPlan);
        }
    }

    private void removeJob(JobPlan jobPlan) {
        try {
            jobPlans.remove(jobPlan);
            LOGGER.info(String.format("Removing job plan %s that no longer exists in the config", jobPlan));
            JobInstance instance = jobInstanceService.buildByIdWithTransitions(jobPlan.getJobId());
            //#2846 - remove this hack
            instance.setIdentifier(jobPlan.getIdentifier());

            scheduleService.cancelJob(instance);
            LOGGER.info(String.format("Successfully removed job plan %s that no longer exists in the config", jobPlan));
        } catch (Exception e) {
            LOGGER.warn(String.format("Unable to remove plan %s from queue that no longer exists in the config", jobPlan));
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
