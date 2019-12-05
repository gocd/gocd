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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.JobResultMessage;
import com.thoughtworks.go.server.messaging.JobResultTopic;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.JobInstancesModel;
import com.thoughtworks.go.server.ui.SortOrder;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class JobInstanceService implements JobPlanLoader, ConfigChangedListener {
    private final JobInstanceDao jobInstanceDao;
    private final JobResultTopic jobResultTopic;
    private final JobStatusCache jobStatusCache;
    private final TransactionTemplate transactionTemplate;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    private final JobResolverService jobResolverService;
    private final EnvironmentConfigService environmentConfigService;
    private final GoConfigService goConfigService;
    private SecurityService securityService;
    private final ServerHealthService serverHealthService;
    private final List<JobStatusListener> listeners;
    private static final String NOT_AUTHORIZED_TO_VIEW_PIPELINE = "Not authorized to view pipeline";

    private static Logger LOGGER = LoggerFactory.getLogger(JobInstanceService.class);
    private static final Object LISTENERS_MODIFICATION_MUTEX = new Object();

    @Autowired
    public JobInstanceService(JobInstanceDao jobInstanceDao, JobResultTopic jobResultTopic, JobStatusCache jobStatusCache,
                              TransactionTemplate transactionTemplate, TransactionSynchronizationManager transactionSynchronizationManager, JobResolverService jobResolverService,
                              EnvironmentConfigService environmentConfigService, GoConfigService goConfigService,
                              SecurityService securityService, ServerHealthService serverHealthService, JobStatusListener... listener) {
        this.jobInstanceDao = jobInstanceDao;
        this.jobResultTopic = jobResultTopic;
        this.jobStatusCache = jobStatusCache;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.jobResolverService = jobResolverService;
        this.environmentConfigService = environmentConfigService;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.serverHealthService = serverHealthService;
        this.listeners = new ArrayList<>(Arrays.asList(listener));
        this.goConfigService.register(this);
        this.goConfigService.register(new PipelineConfigChangedListener());
    }

    public JobInstances latestCompletedJobs(String pipelineName, String stageName, String jobConfigName) {
        return jobInstanceDao.latestCompletedJobs(pipelineName, stageName, jobConfigName, 25);
    }

    public int getJobHistoryCount(String pipelineName, String stageName, String jobConfigName) {
        return jobInstanceDao.getJobHistoryCount(pipelineName, stageName, jobConfigName);
    }

    public JobInstances findJobHistoryPage(String pipelineName, String stageName, String jobConfigName, Pagination pagination, String username, OperationResult result) {
        if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound("Not Found", "Pipeline not found", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }
        if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
            result.forbidden("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return null;
        }

        return jobInstanceDao.findJobHistoryPage(pipelineName, stageName, jobConfigName, pagination.getPageSize(), pagination.getOffset());
    }

    public JobInstance findJobInstance(String pipelineName, String stageName, String jobName, Integer pipelineCounter, Integer stageCounter, String username) {
        if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            throw new RecordNotFoundException(EntityType.Pipeline, pipelineName);
        }
        if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
            throw new NotAuthorizedException(NOT_AUTHORIZED_TO_VIEW_PIPELINE);
        }

        return jobInstanceDao.findJobInstance(pipelineName, stageName, jobName, pipelineCounter, stageCounter);
    }

    public JobInstance buildByIdWithTransitions(long buildId) {
        return jobInstanceDao.buildByIdWithTransitions(buildId);
    }

    public JobInstance buildById(long buildId) {
        return jobInstanceDao.buildById(buildId);
    }

    public void updateAssignedInfo(JobInstance jobInstance) {
        jobInstanceDao.updateAssignedInfo(jobInstance);
        notifyJobStatusChangeListeners(jobInstance);
    }

    public void updateStateAndResult(final JobInstance job) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                internalUpdateJobStateAndResult(job);

                LOGGER.debug("job status updated [{}]", job);
                notifyJobStatusChangeListeners(job);
            }
        });
    }

    private void notifyJobStatusChangeListeners(final JobInstance job) {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                List<JobStatusListener> listeners1;
                synchronized (LISTENERS_MODIFICATION_MUTEX) {
                    listeners1 = new ArrayList<>(listeners);
                }
                for (JobStatusListener jobStatusListener : listeners1)
                    try {
                        jobStatusListener.jobStatusChanged(job);
                    } catch (Exception e) {
                        LOGGER.error("error notifying listener for job {}", job, e);
                    }
            }
        });
    }

    /**
     * This method exists only so that we can scope the transaction properly
     */
    private void internalUpdateJobStateAndResult(final JobInstance job) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobInstanceDao.updateStateAndResult(job);
            }
        });
    }

    @Override
    public JobPlan loadOriginalJobPlan(JobIdentifier jobId) {
        JobIdentifier actualId = jobResolverService.actualJobIdentifier(jobId);
        return jobInstanceDao.loadPlan(actualId.getBuildId());
    }

    public List<JobPlan> orderedScheduledBuilds() {
        return jobInstanceDao.orderedScheduledBuilds();
    }

    public List<WaitingJobPlan> waitingJobPlans() {
        List<JobPlan> jobPlans = orderedScheduledBuilds();
        List<WaitingJobPlan> waitingJobPlans = new ArrayList<>();
        for (JobPlan jobPlan : jobPlans) {
            String envForJob = environmentConfigService.envForPipeline(jobPlan.getPipelineName());
            waitingJobPlans.add(new WaitingJobPlan(jobPlan, envForJob));
        }
        return waitingJobPlans;
    }

    //TODO: Performance fix - we should be using CurrentActivity here
    public JobInstances findHungJobs(List<String> liveAgentIdList) {
        return jobInstanceDao.findHungJobs(liveAgentIdList);
    }

    public void cancelJob(final JobInstance job) {
        LOGGER.info("cancelling job [{}]", job);
        boolean cancelled = job.cancel();
        if (cancelled) {
            updateStateAndResult(job);
            notifyJobCancelled(job);
        }
    }

    private void notifyJobCancelled(final JobInstance instance) {
        if (instance.isAssignedToAgent()) {
            transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    jobResultTopic.post(new JobResultMessage(instance.getIdentifier(), instance.getResult(), instance.getAgentUuid()));
                }
            });
        }
    }

    public void save(StageIdentifier stageIdentifier, long stageId, final JobInstance job) {
        jobInstanceDao.save(stageId, job);
        job.setIdentifier(new JobIdentifier(stageIdentifier, job));

        notifyJobStatusChangeListeners(job);
    }

    public JobInstances currentJobsOfStage(String pipelineName, StageConfig stageConfig) {
        JobInstances jobs = new JobInstances();
        for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
            JobConfigIdentifier jobConfigIdentifier = new JobConfigIdentifier(pipelineName, CaseInsensitiveString.str(stageConfig.name()), CaseInsensitiveString.str(jobConfig.name()));
            List<JobInstance> found = jobStatusCache.currentJobs(jobConfigIdentifier);
            if (found.isEmpty()) {
                jobs.add(new NullJobInstance(CaseInsensitiveString.str(jobConfig.name())));
            } else {
                jobs.addAll(found);
            }
        }
        jobs.sortByName();
        return jobs;
    }

    public List<JobIdentifier> allBuildingJobs() {
        return jobInstanceDao.getBuildingJobs();
    }

    public List<JobInstance> allRunningJobs() {
        return jobInstanceDao.getRunningJobs();
    }

    public void registerJobStateChangeListener(JobStatusListener jobStatusListener) {
        synchronized (LISTENERS_MODIFICATION_MUTEX) {
            listeners.add(jobStatusListener);
        }
    }

    public void failJob(JobInstance jobInstance) {
        jobInstance.fail();
        if (jobInstance.isFailed()) {
            updateStateAndResult(jobInstance);
            notifyJobCancelled(jobInstance);
        }
    }

    public JobInstancesModel completedJobsOnAgent(String uuid, JobHistoryColumns columnName, SortOrder order, int pageNumber, int pageSize) {
        int total = totalCompletedJobsCountOn(uuid);
        Pagination pagination = Pagination.pageByNumber(pageNumber, total, pageSize);
        return completedJobsOnAgent(uuid, columnName, order, pagination);
    }

    public int totalCompletedJobsCountOn(String uuid) {
        return jobInstanceDao.totalCompletedJobsOnAgent(uuid);
    }

    public JobInstancesModel completedJobsOnAgent(String uuid, JobHistoryColumns columnName, SortOrder order, Pagination pagination) {
        List<JobInstance> jobInstances = jobInstanceDao.completedJobsOnAgent(uuid, columnName, order, pagination.getOffset(), pagination.getPageSize());
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        for (JobInstance jobInstance : jobInstances) {
            jobInstance.setPipelineStillConfigured(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(jobInstance.getPipelineName())));
        }

        return new JobInstancesModel(new JobInstances(jobInstances), pagination);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        for (ServerHealthState state : serverHealthService.logs()) {
            HealthStateScope currentScope = state.getType().getScope();
            if (currentScope.isForJob()) {
                serverHealthService.removeByScope(currentScope);
            }
        }
    }

    class PipelineConfigChangedListener extends EntityConfigChangedListener<PipelineConfig> {
        @Override
        public void onEntityConfigChange(PipelineConfig pipelineConfig) {
            for (ServerHealthState state : serverHealthService.logs()) {
                HealthStateScope currentScope = state.getType().getScope();
                if (currentScope.isForJob()) {
                    String[] split = currentScope.getScope().split("/");
                    if (split.length > 0 && new CaseInsensitiveString(split[0]).equals(pipelineConfig.name())) {
                        serverHealthService.removeByScope(currentScope);
                    }
                }
            }

        }
    }

    public enum JobHistoryColumns {
        pipeline("pipelineName"), stage("stageName"), job("name"),
        result("result"), completed("lastTransitionTime"), duration("duration");

        private final String columnName;

        JobHistoryColumns(String columnName) {
            this.columnName = columnName;
        }

        public String getColumnName() {
            return columnName;
        }
    }
}

