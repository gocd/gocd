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

package com.thoughtworks.go.server.dao;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.opensymphony.oscache.base.Cache;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPropertiesGenerator;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.persistence.ArtifactPlanRepository;
import com.thoughtworks.go.server.persistence.ArtifactPropertiesGeneratorRepository;
import com.thoughtworks.go.server.persistence.ResourceRepository;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.SortOrder;
import com.thoughtworks.go.server.util.SqlUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.IBatisUtil.arguments;

@SuppressWarnings("unchecked")
@Component
public class JobInstanceSqlMapDao extends SqlMapClientDaoSupport implements JobInstanceDao, JobStatusListener {
    private static final Logger LOG = Logger.getLogger(JobInstanceSqlMapDao.class);
    private Cache cache;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private GoCache goCache;
    private TransactionTemplate transactionTemplate;
    private EnvironmentVariableDao environmentVariableDao;
    private JobAgentMetadataDao jobAgentMetadataDao;
    private Cloner cloner = new Cloner();
    private ResourceRepository resourceRepository;
    private ArtifactPlanRepository artifactPlanRepository;
    private ArtifactPropertiesGeneratorRepository artifactPropertiesGeneratorRepository;

    @Autowired
    public JobInstanceSqlMapDao(EnvironmentVariableDao environmentVariableDao, GoCache goCache, TransactionTemplate transactionTemplate, SqlMapClient sqlMapClient, Cache cache,
                                TransactionSynchronizationManager transactionSynchronizationManager, SystemEnvironment systemEnvironment, Database database,
                                ResourceRepository resourceRepository, ArtifactPlanRepository artifactPlanRepository, ArtifactPropertiesGeneratorRepository artifactPropertiesGeneratorRepository,
                                JobAgentMetadataDao jobAgentMetadataDao) {
        super(goCache, sqlMapClient, systemEnvironment, database);
        this.environmentVariableDao = environmentVariableDao;
        this.goCache = goCache;
        this.transactionTemplate = transactionTemplate;
        this.cache = cache;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.resourceRepository = resourceRepository;
        this.artifactPlanRepository = artifactPlanRepository;
        this.artifactPropertiesGeneratorRepository = artifactPropertiesGeneratorRepository;
        this.jobAgentMetadataDao = jobAgentMetadataDao;
    }

    public JobInstance buildByIdWithTransitions(long buildInstanceId) {
        String cacheKey = cacheKeyforJobInstanceWithTransitions(buildInstanceId);
        synchronized (cacheKey) {
            JobInstance instance = (JobInstance) goCache.get(cacheKey);
            if (instance == null) {
                instance = job(buildInstanceId, "buildByIdWithTransitions");
                goCache.put(cacheKey, instance);
            }
            return cloner.deepClone(instance);
        }
    }

    private String cacheKeyforJobInstanceWithTransitions(long jobId) {
        return (getClass().getName() + "_jobInstanceWithTranistionIds_" + jobId).intern();
    }

    public JobInstance buildById(long buildId) {
        return job(buildId, "buildById");
    }

    private JobInstance job(long buildId, String queryName) {
        JobInstance instance = (JobInstance) getSqlMapClientTemplate().queryForObject(queryName, buildId);
        if (instance == null) {
            throw new DataRetrievalFailureException("Could not load build with id " + buildId);
        }
        if (instance.getIdentifier() == null) {
            throw new RuntimeException("Identifier must not be null!");
        }
        return instance;
    }

    public List<ActiveJob> activeJobs() {
        return getActiveJobs(getActiveJobIds());
    }

    private List<ActiveJob> getActiveJobs(List<Long> activeJobIds) {
        List<ActiveJob> activeJobs = new ArrayList<>();
        for (Long activeJobId : activeJobIds) {
            ActiveJob job = getActiveJob(activeJobId);
            if (job != null) {
                activeJobs.add(job);
            }
        }
        return activeJobs;
    }

    private ActiveJob getActiveJob(Long activeJobId) {
        String activeJobKey = cacheKeyForActiveJob(activeJobId);
        ActiveJob activeJob = (ActiveJob) goCache.get(activeJobKey);
        if (activeJob == null) {
            synchronized (activeJobKey) {
                activeJob = (ActiveJob) goCache.get(activeJobKey);
                if (activeJob == null) {
                    activeJob = _getActiveJob(activeJobId);
                    if (activeJob != null) { // could have changed to not active and consquently no match found
                        cacheActiveJob(activeJob);
                    }
                }
            }
        }
        return activeJob;//TODO: clone it, caller may mutate
    }

    private List<Long> getActiveJobIds() {
        String idsCacheKey = cacheKeyForActiveJobIds();
        List<Long> activeJobIds = (List<Long>) goCache.get(idsCacheKey);

        synchronized (idsCacheKey) {
            if (activeJobIds == null) {
                activeJobIds = getSqlMapClientTemplate().queryForList("getActiveJobIds");
                goCache.put(idsCacheKey, activeJobIds);
            }
        }
        return activeJobIds;
    }

    private ActiveJob _getActiveJob(Long id) {
        return (ActiveJob) getSqlMapClientTemplate().queryForObject("getActiveJobById", arguments("id", id).asMap());
    }

    private void cacheActiveJob(ActiveJob activeJob) {
        goCache.put(cacheKeyForActiveJob(activeJob.getId()), cloner.deepClone(activeJob));//TODO: we should clone while serving the object out, and not while adding it to cache
    }

    public JobInstance mostRecentJobWithTransitions(JobIdentifier job) {
        Long buildId = findOriginalJobIdentifier(job.getStageIdentifier(), job.getBuildName()).getBuildId();
        return buildByIdWithTransitions(buildId);
    }

    public JobInstance save(long stageId, JobInstance jobInstance) {
        jobInstance.setStageId(stageId);
        getSqlMapClientTemplate().insert("insertBuild", jobInstance);
        updateStateAndResult(jobInstance);

        JobPlan plan = jobInstance.getPlan();
        if (plan != null) {
            save(jobInstance.getId(), plan);
        }
        return jobInstance;
    }

    public void save(long jobId, JobPlan plan) {
        for (Resource resource : plan.getResources()) {
            resourceRepository.saveCopyOf(jobId, resource);
        }
        for (ArtifactPropertiesGenerator generator : plan.getPropertyGenerators()) {
            artifactPropertiesGeneratorRepository.saveCopyOf(jobId, generator);
        }
        for (ArtifactPlan artifactPlan : plan.getArtifactPlans()) {
            artifactPlanRepository.saveCopyOf(jobId, artifactPlan);
        }
        environmentVariableDao.save(jobId, EnvironmentVariableSqlMapDao.EnvironmentVariableType.Job, plan.getVariables());

        if (plan.requiresElasticAgent()){
            jobAgentMetadataDao.save(new JobAgentMetadata(jobId, plan.getElasticProfile()));
        }
    }

    public JobPlan loadPlan(long jobId) {
        DefaultJobPlan plan = (DefaultJobPlan) getSqlMapClientTemplate().queryForObject("select-job-plan", jobId);
        loadJobPlanAssociatedEntities(plan);
        return plan;
    }

    private void loadJobPlanAssociatedEntities(DefaultJobPlan plan) {
        plan.setPlans(artifactPlanRepository.findByBuildId(plan.getJobId()));
        plan.setGenerators(artifactPropertiesGeneratorRepository.findByBuildId(plan.getJobId()));
        plan.setResources(resourceRepository.findByBuildId(plan.getJobId()));
        plan.setVariables(environmentVariableDao.load(plan.getJobId(), EnvironmentVariableSqlMapDao.EnvironmentVariableType.Job));
        plan.setTriggerVariables(environmentVariableDao.load(plan.getPipelineId(), EnvironmentVariableSqlMapDao.EnvironmentVariableType.Trigger));
        JobAgentMetadata jobAgentMetadata = jobAgentMetadataDao.load(plan.getJobId());
        if (jobAgentMetadata != null){
            plan.setElasticProfile(jobAgentMetadata.elasticProfile());
        }
    }

    public JobIdentifier findOriginalJobIdentifier(StageIdentifier stageIdentifier, String jobName) {
        String key = cacheKeyForOriginalJobIdentifier(stageIdentifier, jobName);

        JobIdentifier jobIdentifier = (JobIdentifier) goCache.get(key);
        if (jobIdentifier == null) {
            synchronized (key) {
                jobIdentifier = (JobIdentifier) goCache.get(key);
                if (jobIdentifier == null) {
                    Map params = arguments("pipelineName", stageIdentifier.getPipelineName()).
                            and("pipelineCounter", stageIdentifier.getPipelineCounter()).
                            and("pipelineLabel", stageIdentifier.getPipelineLabel()).
                            and("stageName", stageIdentifier.getStageName()).
                            and("stageCounter", Integer.parseInt(stageIdentifier.getStageCounter())).
                            and("jobName", jobName).asMap();

                    jobIdentifier = (JobIdentifier) getSqlMapClientTemplate().queryForObject("findJobId", params);

                    goCache.put(key, jobIdentifier);
                }
            }
        }

        return cloner.deepClone(jobIdentifier);
    }

    private String cacheKeyForOriginalJobIdentifier(StageIdentifier stageIdentifier, String jobName) {
        return (getClass().getName() + "_originalJobIdentifier_" + StringUtil.escapeAndJoinStrings(
                stageIdentifier.getPipelineName(),
                stageIdentifier.getPipelineLabel(),
                String.valueOf(stageIdentifier.getPipelineCounter()),
                stageIdentifier.getStageName(),
                stageIdentifier.getStageCounter()) + "_job_" + jobName.toLowerCase()).intern();
    }

    public List<JobIdentifier> getBuildingJobs() {
        return buildingJobs(getActiveJobIds());
    }

    public List<JobInstance> completedJobsOnAgent(String uuid, JobInstanceService.JobHistoryColumns jobHistoryColumns, SortOrder order, int offset, int limit) {
        Map params = arguments("uuid", uuid).
                and("offset", offset).
                and("limit", limit).
                and("column", jobHistoryColumns.getColumnName()).
                and("order", order.toString()).asMap();
        return (List<JobInstance>) getSqlMapClientTemplate().queryForList("completedJobsOnAgent", params);
    }

    public int totalCompletedJobsOnAgent(String uuid) {
        return (Integer) getSqlMapClientTemplate().queryForObject("totalCompletedJobsOnAgent", arguments("uuid", uuid).asMap());
    }

    private List<JobIdentifier> buildingJobs(List<Long> activeJobIds) {
        List<JobIdentifier> buildingJobs = new ArrayList<>();
        for (Long activeJobId : activeJobIds) {
            JobIdentifier jobIdentifier = (JobIdentifier) getSqlMapClientTemplate().queryForObject("getBuildingJobIdentifier", activeJobId);
            if (jobIdentifier != null) {
                buildingJobs.add(jobIdentifier);
            }
        }
        return buildingJobs;
    }

    public JobInstance updateAssignedInfo(final JobInstance jobInstance) {
        return (JobInstance) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                getSqlMapClientTemplate().update("updateAssignedInfo", jobInstance);
                updateStateAndResult(jobInstance);
                return jobInstance;
            }
        });
    }

    public JobInstance updateStateAndResult(final JobInstance jobInstance) {
        return (JobInstance) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        // Methods not extracted in order to make synchronization visible.
                        synchronized (cacheKeyForJobPlan(jobInstance.getId())) {
                            removeCachedJobPlan(jobInstance);
                        }
                        synchronized (cacheKeyForActiveJobIds()) {
                            goCache.remove(cacheKeyForActiveJobIds());
                        }
                        String activeJobKey = cacheKeyForActiveJob(jobInstance.getId());
                        synchronized (activeJobKey) {
                            goCache.remove(activeJobKey);
                        }
                        removeCachedJobInstance(jobInstance);
                    }
                });
                logIfJobIsCompleted(jobInstance);
                updateStatus(jobInstance);
                updateResult(jobInstance);
                return jobInstance;
            }
        });

    }

    private void removeCachedJobInstance(JobInstance jobInstance) {
        String cacheKeyOfJob = cacheKeyforJobInstanceWithTransitions(jobInstance.getId());
        synchronized (cacheKeyOfJob) {
            goCache.remove(cacheKeyOfJob);
        }
    }

    private void removeCachedJobPlan(JobInstance jobInstance) {
        goCache.remove(cacheKeyForJobPlan(jobInstance.getId()));
    }

    private void logIfJobIsCompleted(JobInstance jobInstance) {
        JobState currentState = getCurrentState(jobInstance.getId());
        if(currentState.isCompleted()) {
            String message = String.format(
                    "State change for a completed Job is not allowed. Job %s is currently State=%s, Result=%s",
                    jobInstance.getIdentifier(), jobInstance.getState(), jobInstance.getResult());
            LOG.warn(message, new Exception().fillInStackTrace());
        }
    }

    private JobState getCurrentState(long jobId) {
        String state = (String) getSqlMapClientTemplate().queryForObject("currentJobState", jobId);
        if (state==null) {
            return JobState.Unknown;
        }
        return JobState.valueOf(state);
    }

    private void updateStatus(JobInstance jobInstance) {
        getSqlMapClientTemplate().update("updateStatus", jobInstance);
        saveTransitions(jobInstance);
    }

    private void updateResult(JobInstance job) {
        getSqlMapClientTemplate().update("updateResult", job);
    }

    public boolean isValid(String pipelineName, String stageName, String buildName) {
        return (Boolean) getSqlMapClientTemplate().queryForObject("isValid",
                arguments("pipelineName", pipelineName)
                        .and("stageName", stageName)
                        .and("buildName", buildName).asMap()
        );
    }

    public void ignore(JobInstance job) {
        getSqlMapClientTemplate().update("ignoreBuildById", job.getId());
    }

    public JobInstances latestCompletedJobs(String pipelineName, String stageName, String jobConfigName, int count) {
        Map params = new HashMap();
        params.put("pipelineName", pipelineName);
        params.put("stageName", stageName);
        params.put("jobConfigName", jobConfigName);
        params.put("count", count);
        List<JobInstance> results =
                (List<JobInstance>) getSqlMapClientTemplate().queryForList("latestCompletedJobs", params);

        return new JobInstances(results);
    }

	public int getJobHistoryCount(String pipelineName, String stageName, String jobName) {
		Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).and("jobConfigName", jobName).asMap();
		Integer count = (Integer) getSqlMapClientTemplate().queryForObject("getJobHistoryCount", toGet);
		return count;
	}

	public JobInstances findJobHistoryPage(String pipelineName, String stageName, String jobConfigName, int count, int offset) {
		Map params = new HashMap();
		params.put("pipelineName", pipelineName);
		params.put("stageName", stageName);
		params.put("jobConfigName", jobConfigName);
		params.put("count", count);
		params.put("offset", offset);

		List<JobInstance> results = (List<JobInstance>) getSqlMapClientTemplate().queryForList("findJobHistoryPage", params);

		return new JobInstances(results);
	}

    public List<JobPlan> orderedScheduledBuilds() {
        List<Long> jobIds = (List<Long>) getSqlMapClientTemplate().queryForList("scheduledPlanIds");

        List<JobPlan> plans = new ArrayList<>();
        for (Long jobId : jobIds) {
            String cacheKey = cacheKeyForJobPlan(jobId);
            synchronized (cacheKey) {
                JobPlan jobPlan = (JobPlan) goCache.get(cacheKey);
                if (jobPlan == null) {
                    jobPlan = _loadJobPlan(jobId);
                }
                if (jobPlan != null) {
                    jobPlan = cloner.deepClone(jobPlan);
                    goCache.put(cacheKey, jobPlan);
                    plans.add(jobPlan);
                }
            }
        }
        return plans;
    }

    private JobPlan _loadJobPlan(Long jobId) {
        DefaultJobPlan jobPlan = (DefaultJobPlan) getSqlMapClientTemplate().queryForObject("scheduledPlan", arguments("id", jobId).asMap());
        if (jobPlan == null) {
            return null;
        }
        loadJobPlanAssociatedEntities(jobPlan);
        return jobPlan;
    }

    private String cacheKeyForJobPlan(Long jobId) {
        return (getClass().getName() + "_jobPlan_" + jobId).intern();
    }

    private String cacheKeyForActiveJob(Long jobId) {
        return (getClass().getName() + "_activeJob_" + jobId).intern();
    }

    private String cacheKeyForActiveJobIds() {
        return (getClass().getName() + "_activeJobIds").intern();
    }

    public JobInstance getLatestInProgressBuildByAgentUuid(String uuid) {
        JobInstance job = (JobInstance) getSqlMapClientTemplate().queryForObject("getLatestInProgressBuildOnAgent",
                uuid);
        return job;
    }

    public JobInstances findHungJobs(List<String> liveAgentIdList) {
        String sqlValue = SqlUtil.joinWithQuotesForSql(liveAgentIdList.toArray());
        List<JobInstance> list = getSqlMapClientTemplate().queryForList("getHungJobs",
                arguments("liveAgentIdList", sqlValue).asMap());
        return new JobInstances(list);
    }

    public int getNumberOfActiveBuildsOnRemoteAgent(List<String> localAgentIds) {
        String sqlValue = SqlUtil.joinWithQuotesForSql(localAgentIds.toArray());
        Integer count = (Integer) getSqlMapClientTemplate().queryForObject("getNumberOfActiveBuildsOnRemoteAgent",
                arguments("localAgentIds", sqlValue).asMap());
        return count;
    }

    private void saveTransitions(JobInstance jobInstance) {
        for (JobStateTransition transition : jobInstance.getTransitions()) {
            if (!transition.hasId()) {
                saveTransition(jobInstance, transition);
            }
        }
        if (jobInstance.getIdentifier() != null) {
            String pipelineName = jobInstance.getIdentifier().getPipelineName();
            String stageName = jobInstance.getIdentifier().getStageName();
            cache.flushEntry(jobInstance.getBuildDurationKey(pipelineName, stageName));
        }
    }

    private void saveTransition(JobInstance jobInstance, JobStateTransition transition) {
        transition.setJobId(jobInstance.getId());
        transition.setStageId(jobInstance.getStageId());
        getSqlMapClientTemplate().insert("insertTransition", transition);
    }

    @Override
    public void jobStatusChanged(final JobInstance job) {
        if(job.isRescheduled()) {
            goCache.remove(cacheKeyForOriginalJobIdentifier(job.getIdentifier().getStageIdentifier(), job.getName()));
        }
    }
}
