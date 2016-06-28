/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.dao;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.CacheEntry;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.DynamicReadWriteLock;
import com.thoughtworks.go.util.FuncVarArg;
import com.thoughtworks.go.util.IBatisUtil;
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

@Component
public class StageSqlMapDao extends SqlMapClientDaoSupport implements StageDao, StageStatusListener, JobStatusListener {
    private static final Logger LOGGER = Logger.getLogger(SqlMapClientDaoSupport.class);

    private TransactionTemplate transactionTemplate;
    private GoCache goCache;
    private JobInstanceSqlMapDao buildInstanceDao;
    private Cache cache;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private Cloner cloner = new Cloner();
    private DynamicReadWriteLock readWriteLock = new DynamicReadWriteLock();

    @Autowired
    public StageSqlMapDao(JobInstanceSqlMapDao buildInstanceDao, Cache cache, TransactionTemplate transactionTemplate, SqlMapClient sqlMapClient, GoCache goCache,
                          TransactionSynchronizationManager transactionSynchronizationManager, SystemEnvironment systemEnvironment, Database database) {
        super(goCache, sqlMapClient, systemEnvironment, database);
        this.buildInstanceDao = buildInstanceDao;
        this.cache = cache;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
    }

    public Stages scheduledStages() {
        Stages scheduledStages = new Stages(
                (List<Stage>) getSqlMapClientTemplate().queryForList("getScheduledStages"));
        return new Stages(scheduledStages);
    }

    public Stage save(final Pipeline pipeline, final Stage stage) {
        return (Stage) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        String pipelineName = pipeline.getName();
                        String stageName = stage.getName();

                        clearStageHistoryPageCaches(stage, pipelineName, false);
                        clearCachedStage(stage.getIdentifier());
                        clearCachedAllStages(pipelineName, pipeline.getCounter(), stageName);
                        removeFromCache(cacheKeyForStageCountForGraph(pipelineName, stageName));
                    }
                });
                stage.setPipelineId(pipeline.getId());
                int maxStageCounter = getMaxStageCounter(pipeline.getId(), stage.getName());
                stage.setCounter(maxStageCounter + 1);

                getSqlMapClientTemplate().update("markPreviousStageRunsAsNotLatest", arguments("stageName", stage.getName()).and("pipelineId", pipeline.getId()).asMap());
                getSqlMapClientTemplate().insert("insertStage", stage);

                stage.setIdentifier(new StageIdentifier(pipeline, stage));
                return stage;
            }
        });
    }

    private void clearStageHistoryPageCaches(Stage stage, String pipelineName, boolean clearOnlyHistoryPages) {
        String mutex = mutexForStageHistory(pipelineName, stage.getName());
        readWriteLock.acquireWriteLock(mutex);
        try {
            if (!clearOnlyHistoryPages) {
                goCache.remove(cacheKeyForStageCount(pipelineName, stage.getName()));
                goCache.remove(cacheKeyForStageOffset(stage));
            }
            goCache.remove(cacheKeyForStageHistories(pipelineName, stage.getName()));
            goCache.remove(cacheKeyForDetailedStageHistories(pipelineName, stage.getName()));
        } finally {
            readWriteLock.releaseWriteLock(mutex);
        }
    }

    @Deprecated
    // This is only used in test for legacy purpose.
    // Please call pipelineService.save(aPipeline) instead
    public Stage saveWithJobs(Pipeline pipeline, Stage stage) {
        if (stage.getState() == null) {
            stage.building();
        }
        stage = save(pipeline, stage);
        clearCachedStage(new StageIdentifier(pipeline, stage));//This is bad because it should be done in after-commit, but this is test-ONLY-code, so ignore!

        JobInstances jobInstances = stage.getJobInstances();
        for (JobInstance job : jobInstances) {
            buildInstanceDao.save(stage.getId(), job);
        }

        for (JobInstance jobInstance : jobInstances) {
            jobInstance.setIdentifier(new JobIdentifier(pipeline, stage, jobInstance));
        }
        return stage;
    }

    public int getMaxStageCounter(long pipelineId, String stageName) {
        Map<String, Object> toGet = arguments("pipelineId", pipelineId).and("name", stageName).asMap();
        Integer maxCounter = (Integer) getSqlMapClientTemplate().queryForObject("getMaxStageCounter", toGet);
        return maxCounter == null ? 0 : maxCounter;
    }

    public int getCount(String pipelineName, String stageName) {
        Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
        String key = cacheKeyForStageCount(pipelineName, stageName);
        Integer count = (Integer) goCache.get(key);
        if (count == null) {
            count = (Integer) getSqlMapClientTemplate().queryForObject("getStageHistoryCount", toGet);
            goCache.put(key, count);
        }
        return count;
    }


    private String cacheKeyForStageCount(String pipelineName, String stageName) {
        return String.format("%s_numberOfStages_%s_<>_%s", getClass().getName(), pipelineName, stageName).intern();
    }

    public Stages getStagesByPipelineId(long pipelineId) {
        Stages stageHistory = new Stages(
                (List<Stage>) getSqlMapClientTemplate().queryForList("getStagesByPipelineId", pipelineId));
        return new Stages(stageHistory);
    }

    public int findLatestStageCounter(PipelineIdentifier pipeline, String stageName) {
        Map<String, Object> toGet = arguments("pipelineName", pipeline.getName()).and("pipelineCounter",
                pipeline.getCounter()).and("pipelineLabel", pipeline.getLabel()).and(
                "stageName", stageName).asMap();
        Integer maxCounter = (Integer) getSqlMapClientTemplate().queryForObject(
                "findMaxStageCounter", toGet);
        return maxCounter == null ? 0 : maxCounter;
    }

    public Stage findStageWithIdentifier(StageIdentifier identifier) {
        String cachekey = cacheKeyForStageIdentifier(identifier);
        String cacheKeyForIdentifiers = cacheKeyForListOfStageIdentifiers(identifier);
        synchronized (cacheKeyForIdentifiers) {
            Stage stage = (Stage) goCache.get(cacheKeyForIdentifiers, cachekey);
            if (stage == null) {
                IBatisUtil.IBatisArgument argument = IBatisUtil.arguments("pipelineName", identifier.getPipelineName())
                        .and("pipelineCounter", identifier.getPipelineCounter())
                        .and("stageName", identifier.getStageName())
                        .and("stageCounter", Integer.parseInt(identifier.getStageCounter()));
                if (identifier.getPipelineLabel() != null) {
                    argument = argument.and("pipelineLabel", identifier.getPipelineLabel());
                }
                Map arguments = argument.asMap();
                stage = (Stage) getSqlMapClientTemplate().queryForObject("findStageWithJobsByIdentifier", arguments);
                if (stage == null) {
                    return new NullStage(identifier.getStageName());
                }
                goCache.put(cacheKeyForIdentifiers, cachekey, stage);
            }
            return cloner.deepClone(stage);
        }
    }

    private String cacheKeyForListOfStageIdentifiers(StageIdentifier stageIdentifier) {
        return String.format("%s_stageRunIdentifier_%s_%s_%s", getClass().getName(), stageIdentifier.getPipelineName(), stageIdentifier.getPipelineCounter(),
                stageIdentifier.getStageName()).intern();
    }

    private String cacheKeyForStageIdentifier(StageIdentifier stageIdentifier) {
        return String.format("%s_stageIdentifier_%s_%s_%s_%s", getClass().getName(), stageIdentifier.getPipelineName(), stageIdentifier.getPipelineCounter(),
                stageIdentifier.getStageName(), stageIdentifier.getStageCounter()).intern();
    }

    public long getExpectedDurationMillis(String pipelineName, String stageName, JobInstance job) {
        Long duration = getDurationOfLastSuccessfulOnAgent(pipelineName, stageName, job);
        return duration == null ? 0L : duration * 1000L;
    }


    public Long getDurationOfLastSuccessfulOnAgent(String pipelineName, String stageName, JobInstance job) {
        String key = job.getBuildDurationKey(pipelineName, stageName);
        Long duration;
        try {
            duration = (Long) cache.getFromCache(key, CacheEntry.INDEFINITE_EXPIRY);
        } catch (NeedsRefreshException nre) {
            boolean updated = false;
            try {
                duration = recalculateBuildDuration(pipelineName, stageName, job);
                cache.putInCache(key, duration);
                updated = true;
            } finally {
                if (!updated) {
                    // It is essential that cancelUpdate is called if the
                    // cached content could not be rebuilt
                    cache.cancelUpdate(key);
                    LOGGER.warn("refresh cancelled for " + key);
                }
            }
        }
        return duration;
    }

    private Long recalculateBuildDuration(String pipelineName, String stageName, JobInstance job) {
        Map<String, Object> toGet =
                arguments("buildName", job.getName())
                        .and("agentUuid", job.getAgentUuid())
                        .and("stageName", stageName)
                        .and("pipelineName", pipelineName)
                        .asMap();

        // Return a list of job id's and stage names, entries sorted by id.
        Long buildId =
                (Long) getSqlMapClientTemplate().queryForObject("getLastSuccessfulBuildIdOnAgent", toGet);
        if (buildId == null) {
            return null;
        }
        JobInstance mostRecent = buildInstanceDao.buildByIdWithTransitions(buildId);
        return mostRecent.durationOfCompletedBuildInSeconds();
    }

    public int getMaxStageOrder(long pipelineId) {
        Integer order = (Integer) getSqlMapClientTemplate().queryForObject("getMaxStageOrder", pipelineId);
        return (order == null ? 0 : order);
    }

    public Integer getStageOrderInPipeline(long pipelineId, String stageName) {
        Map<String, Object> params = arguments("pipelineId", pipelineId).and("stageName", stageName).asMap();
        return (Integer) getSqlMapClientTemplate().queryForObject("getStageOrderInPipeline", params);
    }

    public void updateResult(final Stage stage, final StageResult result) {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                StageIdentifier identifier = stage.getIdentifier();
                clearStageHistoryPageCaches(stage, identifier.getPipelineName(), true);
                clearJobStatusDependentCaches(stage.getId(), identifier);
                removeFromCache(cacheKeyForStageCountForGraph(identifier.getPipelineName(), identifier.getStageName()));
            }
        });
        getSqlMapClientTemplate().update("updateStageStatus", arguments("stageId", stage.getId())
                .and("result", result.toString())
                .and("state", stage.getState())
                .and("completedByTransitionId", stage.getCompletedByTransitionId()).asMap());
    }

    public Stage mostRecentCompleted(StageConfigIdentifier identifier) {
        Map<String, Object> toGet = arguments("pipelineName", identifier.getPipelineName())
                .and("stageName", identifier.getStageName()).asMap();
        return (Stage) getSqlMapClientTemplate().queryForObject("getMostRecentCompletedStage", toGet);
    }

    public Stage mostRecentStage(StageConfigIdentifier identifier) {
        Map<String, Object> toGet = arguments("pipelineName", identifier.getPipelineName())
                .and("stageName", identifier.getStageName()).asMap();
        return (Stage) getSqlMapClientTemplate().queryForObject("getMostRecentStage", toGet);
    }

    public List<JobInstance> mostRecentJobsForStage(String pipelineName, String stageName) {
        Long mostRecentId = mostRecentId(pipelineName, stageName);

        Stage stage = stageByIdWithBuildsWithNoAssociations(mostRecentId);

        List<JobInstance> jobInstances = new ArrayList<>();
        jobInstances.addAll(stage.getJobInstances());
        return jobInstances;
    }


    public Stages getPassedStagesByName(String pipelineName, String stageName, int limit, int offset) {
        Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName)
                .and("limit", limit).and("offset", offset).asMap();
        return new Stages((List<Stage>) getSqlMapClientTemplate().queryForList("allPassedStagesByName", toGet));
    }

    public List<StageAsDMR> getPassedStagesAfter(StageIdentifier stageIdentifier, int limit, int offset) {
        Stage laterThan = findStageWithIdentifier(stageIdentifier);

        Map<String, Object> toGet = arguments("pipelineName", stageIdentifier.getPipelineName()).and("stageName", stageIdentifier.getStageName())
                .and("laterThan", laterThan.getId()).and("limit", limit).and("offset", offset).asMap();

        return (List<StageAsDMR>) getSqlMapClientTemplate().queryForList("allPassedStageAsDMRsAfter", toGet);
    }

    public Stages getAllRunsOfStageForPipelineInstance(String pipelineName, Integer pipelineCounter, String stageName) {
        String cacheKeyForAllStages = cacheKeyForAllStageOfPipeline(pipelineName, pipelineCounter, stageName);
        synchronized (cacheKeyForAllStages) {
            List<Stage> stages = (List<Stage>) goCache.get(cacheKeyForAllStages);
            if (stages == null) {
                Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).and("stageName", stageName).asMap();
                stages = (List<Stage>) getSqlMapClientTemplate().queryForList("getAllRunsOfStageForPipelineInstance", toGet);
                goCache.put(cacheKeyForAllStages, stages);
            }
            return new Stages(cloner.deepClone(stages));
        }
    }

    private String cacheKeyForAllStageOfPipeline(String pipelineName, Integer pipelineCounter, String stageName) {
        return String.format(getClass().getName() + "_allStageOfPipeline_%s_%s_%s", pipelineName, pipelineCounter, stageName).intern();
    }

    public List<Stage> findStageHistoryForChart(String pipelineName, String stageName, int pageSize, int offset) {
        Map<String, Object> args =
                arguments("pipelineName", pipelineName).
                        and("stageName", stageName).
                        and("offset", offset).
                        and("limit", pageSize).asMap();
        return new Stages((List<Stage>) getSqlMapClientTemplate().queryForList("findStageHistoryForChartPerPipeline", args));
    }

    public int getTotalStageCountForChart(String pipelineName, String stageName) {
        String key = cacheKeyForStageCountForGraph(pipelineName, stageName);
        Integer total = (Integer) goCache.get(key);
        if (total == null) {
            synchronized (key) {
                if (total == null) {
                    Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
                    total = (Integer) getSqlMapClientTemplate().queryForObject("getTotalStageCountForChart", toGet);
                    goCache.put(key, total);
                }
            }
        }
        return total;
    }

    @Override
    public List<StageIdentity> findLatestStageInstances() {
        String key = cacheKeyForLatestStageInstances();
        List<StageIdentity> stageIdentities = (List<StageIdentity>) goCache.get(key);
        if (stageIdentities == null) {
            synchronized (key) {
                stageIdentities = (List<StageIdentity>) goCache.get(key);
                if (stageIdentities == null) {
                    stageIdentities = (List<StageIdentity>) getSqlMapClientTemplate().queryForList("latestStageInstances");
                    goCache.put(key, stageIdentities);
                }
            }
        }
        return stageIdentities;
    }


    public StageHistoryPage findStageHistoryPageByNumber(final String pipelineName, final String stageName, final int pageNumber, final int pageSize) {
        return findStageHistoryPage(pipelineName, stageName, new FuncVarArg<Pagination, Object>() {
            public Pagination call(Object... ignore) {
                int total = getCount(pipelineName, stageName);
                return Pagination.pageByNumber(pageNumber, total, pageSize);
            }
        });
    }

    public StageInstanceModels findDetailedStageHistoryByOffset(String pipelineName, String stageName, final Pagination pagination) {
        String mutex = mutexForStageHistory(pipelineName, stageName);
        readWriteLock.acquireReadLock(mutex);
        try {
            String subKey = String.format("%s-%s", pagination.getOffset(), pagination.getPageSize());
            String key = cacheKeyForDetailedStageHistories(pipelineName, stageName);
            StageInstanceModels stageInstanceModels = (StageInstanceModels) goCache.get(key, subKey);
            if (stageInstanceModels == null) {
                stageInstanceModels = findDetailedStageHistory(pipelineName, stageName, pagination);
                goCache.put(key, subKey, stageInstanceModels);
            }
            return cloner.deepClone(stageInstanceModels);
        } finally {
            readWriteLock.releaseReadLock(mutex);
        }
    }

    public StageHistoryPage findStageHistoryPage(final Stage stage, final int pageSize) {
        final StageIdentifier id = stage.getIdentifier();
        return findStageHistoryPage(id.getPipelineName(), id.getStageName(), new FuncVarArg<Pagination, Object>() {
            public Pagination call(Object... ignore) {
                int total = getCount(id.getPipelineName(), id.getStageName());
                int offset = findOffsetForStage(stage);
                return Pagination.pageFor(offset, total, pageSize);
            }
        });
    }

    public StageHistoryPage findStageHistoryPage(String pipelineName, String stageName, FuncVarArg<Pagination, Object> function) {
        //IMPORTANT: wire cache clearing on job-state-change for me, the day StageHistoryEntry gets jobs - Sachin & JJ
        String mutex = mutexForStageHistory(pipelineName, stageName);
        readWriteLock.acquireReadLock(mutex);
        try {
            Pagination pagination = function.call();
            String subKey = String.format("%s-%s", pagination.getCurrentPage(), pagination.getPageSize());
            String key = cacheKeyForStageHistories(pipelineName, stageName);
            StageHistoryPage stageHistoryPage = (StageHistoryPage) goCache.get(key, subKey);
            if (stageHistoryPage == null) {
                List<StageHistoryEntry> stageHistoryEntries = findStages(pagination, pipelineName, stageName);
                stageHistoryPage = new StageHistoryPage(stageHistoryEntries, pagination, findImmediateChronologicallyForwardStageHistoryEntry(stageHistoryEntries.get(0)));
                goCache.put(key, subKey, stageHistoryPage);
            }
            return cloner.deepClone(stageHistoryPage);
        } finally {
            readWriteLock.releaseReadLock(mutex);
        }
    }

    public StageHistoryEntry findImmediateChronologicallyForwardStageHistoryEntry(StageHistoryEntry stageHistoryEntry) {
        StageIdentifier stageIdentifier = stageHistoryEntry.getIdentifier();
        Map<String, Object> args = arguments("pipelineName", stageIdentifier.getPipelineName()).
                and("stageName", stageIdentifier.getStageName()).
                and("id", stageHistoryEntry.getId()).
                and("limit", 1).asMap();
        return (StageHistoryEntry) getSqlMapClientTemplate().queryForObject("findStageHistoryEntryBefore", args);
    }

    private String mutexForStageHistory(String pipelineName, String stageName) {
        return String.format("%s_stageHistoryMutex_%s_<>_%s", getClass().getName(), pipelineName, stageName).intern();
    }

    private String cacheKeyForStageHistories(String pipelineName, String stageName) {
        return String.format("%s_stageHistories_%s_<>_%s", getClass().getName(), pipelineName, stageName).intern();
    }

    private String cacheKeyForDetailedStageHistories(String pipelineName, String stageName) {
        return String.format("%s_detailedStageHistories_%s_<>_%s", getClass().getName(), pipelineName, stageName).intern();
    }

    public Long findStageIdByPipelineAndStageNameAndCounter(long pipelineId, String name, String counter) {
        Map<String, Object> toGet = arguments("pipelineId", pipelineId)
                .and("stageName", name)
                .and("stageCounter", counter)
                .asMap();
        return (Long) getSqlMapClientTemplate().queryForObject("findStageIdByPipelineAndStageNameAndCounter", toGet);
    }

    public List<StageIdentifier> findFailedStagesBetween(String pipelineName, String stageName, double fromNaturalOrder, double toNaturalOrder) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).
                and("stageName", stageName).
                and("fromNaturalOrder", fromNaturalOrder).
                and("toNaturalOrder", toNaturalOrder).asMap();
        return (List<StageIdentifier>) getSqlMapClientTemplate().queryForList("findFailedStagesBetween", args);
    }

    List<StageHistoryEntry> findStages(Pagination pagination, String pipelineName, String stageName) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).
                and("stageName", stageName).
                and("limit", pagination.getPageSize()).
                and("offset", pagination.getOffset()).asMap();
        return (List<StageHistoryEntry>) getSqlMapClientTemplate().queryForList("findStageHistoryPage", args);
    }

    StageInstanceModels findDetailedStageHistory(String pipelineName, String stageName, Pagination pagination) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).
                and("stageName", stageName).
                and("limit", pagination.getPageSize()).
                and("offset", pagination.getOffset()).asMap();
        List<StageInstanceModel> detailedStageHistory = (List<StageInstanceModel>) getSqlMapClientTemplate().queryForList("getDetailedStageHistory", args);
        StageInstanceModels stageInstanceModels = new StageInstanceModels();
        stageInstanceModels.addAll(detailedStageHistory);
        return stageInstanceModels;
    }

    private int findOffsetForStage(Stage stage) {
        String key = cacheKeyForStageOffset(stage);
        Integer offset = (Integer) goCache.get(key, String.valueOf(stage.getId()));
        if (offset == null) {
            Map<String, Object> args =
                    arguments("stageId", stage.getId()).
                            and("stageName", stage.getIdentifier().getStageName()).
                            and("pipelineName", stage.getIdentifier().getPipelineName())
                            .asMap();
            offset = (Integer) getSqlMapClientTemplate().queryForObject("findOffsetForStage", args);
            goCache.put(key, String.valueOf(stage.getId()), offset);
        }

        return offset;
    }

    private String cacheKeyForStageOffset(Stage stage) {
        return String.format("%s_stageOffsetMap_%s_<>_%s", getClass().getName(), stage.getIdentifier().getPipelineName(), stage.getIdentifier().getStageName()).intern();
    }

    private List<StageFeedEntry> findForFeed(String baseQuery, FeedModifier modifier, long transitionId, int pageSize) {
        Map parameters = new HashMap();
        parameters.put("value", transitionId);
        parameters.put("pageLimit", pageSize);
        return getSqlMapClientTemplate().queryForList(baseQuery + modifier.suffix(), parameters);
    }

    public List<StageFeedEntry> findAllCompletedStages(FeedModifier modifier, long id, int pageSize) {
        return findForFeed("allCompletedStages", modifier, id, pageSize);
    }

    public List<StageFeedEntry> findCompletedStagesFor(String pipelineName, FeedModifier feedModifier, long transitionId, long pageSize) {
        return _findCompletedStagesFor(pipelineName, feedModifier, transitionId, pageSize);
    }

    private List<StageFeedEntry> _findCompletedStagesFor(String pipelineName, FeedModifier feedModifier, long transitionId, long pageSize) {
        //IMPORTANT: wire cache clearing on job-state-change for me, the day FeedEntry gets jobs - Sachin & JJ
        Map parameters = new HashMap();
        parameters.put("value", transitionId);
        parameters.put("pageLimit", pageSize);
        parameters.put("pipelineName", pipelineName);
        return (List<StageFeedEntry>) getSqlMapClientTemplate().queryForList("allCompletedStagesForPipeline" + feedModifier.suffix(), parameters);
    }

    public Stage mostRecentWithBuilds(String pipelineName, StageConfig stageConfig) {
        Long mostRecentId = mostRecentId(pipelineName, CaseInsensitiveString.str(stageConfig.name()));
        return mostRecentId == null ? NullStage.createNullStage(stageConfig) : stageByIdWithBuildsWithNoAssociations(mostRecentId);
    }

    Long mostRecentId(String pipelineName, String stageName) {
        String key = cacheKeyForMostRecentId(pipelineName, stageName);
        Long id = (Long) goCache.get(key);
        if (id != null) {
            return id;
        }
        synchronized (key) {
            id = (Long) goCache.get(key);
            if (id != null) {
                return id;
            }
            Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
            id = (Long) getSqlMapClientTemplate().queryForObject("getMostRecentId", toGet);
            goCache.put(key, id);
        }
        return id;
    }

    public void stageStatusChanged(Stage stage) {
        String pipelineName = stage.getIdentifier().getPipelineName();
        String stageName = stage.getName();
        removeFromCache(cacheKeyForMostRecentId(pipelineName, stageName));
        removeFromCache(cacheKeyForPipelineAndStage(pipelineName, stageName));
        removeFromCache(cacheKeyForPipelineAndCounter(pipelineName, stage.getIdentifier().getPipelineCounter()));
        removeFromCache(cacheKeyForStageById(stage.getId()));
        removeFromCache(cacheKeyForLatestStageInstances());
    }


    public Stage stageByIdWithBuilds(long id) {
        return stageById(id);
    }

    public Stage stageById(long id) {
        String key = cacheKeyForStageById(id);
        Stage stage = (Stage) goCache.get(key);

        if (stage == null) {
            synchronized (key) {
                stage = (Stage) goCache.get(key);
                if (stage == null) {
                    stage = (Stage) getSqlMapClientTemplate().queryForObject("getStageById", id);
                    if (stage == null) {
                        throw new DataRetrievalFailureException("Unable to load related stage data for id " + id);
                    }
                    goCache.put(key, stage);
                }
            }
        }
        return cloner.deepClone(stage);
    }

    private String cacheKeyForStageById(long id) {
        return String.format("%s_stageById_%s", getClass().getName(), id).intern();
    }

    public Stage mostRecentPassed(String pipelineName, String stageName) {
        Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
        Long mostRecentId = (Long) getSqlMapClientTemplate().queryForObject("getMostRecentPassedStageId", toGet);
        return mostRecentId == null ? null : stageByIdWithBuilds(mostRecentId);
    }

    public boolean isStageActive(String pipelineName, String stageName) {
        String cacheKey = cacheKeyForPipelineAndStage(pipelineName, stageName);
        synchronized (cacheKey) {
            Boolean isActive = (Boolean) goCache.get(cacheKey);
            if (isActive == null) {
                final Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
                isActive = !getSqlMapClientTemplate().queryForObject("isStageActive", toGet).equals(0);
                goCache.put(cacheKey, isActive);
            }
            return isActive;
        }
    }

    public Stage getStageByBuild(long buildId) {
        Stage stage = (Stage) getSqlMapClientTemplate().queryForObject("getStageByBuildId", buildId);
        if (stage == null) {
            throw new DataRetrievalFailureException("Unable to load related stage data for buildId " + buildId);
        }
        return stage;
    }

    public long getStageIdFromBuildId(long buildId) {
        Long stageId = (Long) getSqlMapClientTemplate().queryForObject("getStageIdFromBuildId", buildId);
        if (stageId == null) {
            throw new DataRetrievalFailureException("No Stage found containing buildId " + buildId);
        }
        return stageId;
    }

    public String stageNameByStageId(long stageId) {
        return getSqlMapClientTemplate().queryForObject("getStageNameByStageId", stageId).toString();
    }

    public void setBuildInstanceDao(JobInstanceSqlMapDao buildInstanceDao) {
        this.buildInstanceDao = buildInstanceDao;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public void clearCachedStage(StageIdentifier identifier) {
        removeFromCache(cacheKeyForListOfStageIdentifiers(identifier));
    }

    public void clearCachedAllStages(String pipelineName, int pipelineCounter, String stageName) {
        String cacheForAllStagesOfPipeline = cacheKeyForAllStageOfPipeline(pipelineName, pipelineCounter, stageName);
        removeFromCache(cacheForAllStagesOfPipeline);
    }

    private String cacheKeyForPipelineAndStage(String pipelineName, String stageName) {
        return String.format("%s_isStageActive_%s_%s", StageSqlMapDao.class.getName(), pipelineName, stageName).intern();
    }

    public Stages findAllStagesFor(String pipelineName, int counter) {
        String key = cacheKeyForPipelineAndCounter(pipelineName, counter);
        List<Stage> stages = (List<Stage>) goCache.get(key);
        if (stages == null) {
            synchronized (key) {
                stages = (List<Stage>) goCache.get(key);
                if (stages == null) {
                    Map<String, Object> params = arguments("pipelineName", pipelineName).and("pipelineCounter", counter).asMap();
                    stages = getSqlMapClientTemplate().queryForList("getStagesByPipelineNameAndCounter", params);
                    goCache.put(key, stages);
                }
            }
        }
        return new Stages(stages);
    }

    public List<Stage> oldestStagesHavingArtifacts() {
        return getSqlMapClientTemplate().queryForList("oldestStagesHavingArtifacts");
    }

    public void markArtifactsDeletedFor(Stage stage) {
        getSqlMapClientTemplate().update("markStageArtifactDeleted", arguments("stageId", stage.getId()).asMap());
    }

    private String cacheKeyForPipelineAndCounter(String pipelineName, int counter) {
        String key = StageSqlMapDao.class.getName() + "_allStagesOfPipelineInstance_" + pipelineName + "_" + counter;
        return key.intern();
    }

    public void jobStatusChanged(JobInstance job) {
        clearJobStatusDependentCaches(job.getStageId(), job.getIdentifier().getStageIdentifier());
    }

    private void clearJobStatusDependentCaches(long stageId, StageIdentifier stageIdentifier) {
        removeFromCache(cacheKeyForStageById(stageId));
        clearCachedStage(stageIdentifier);
        clearCachedAllStages(stageIdentifier.getPipelineName(), stageIdentifier.getPipelineCounter(), stageIdentifier.getStageName());
    }


    String cacheKeyForLatestStageInstances() {
        return String.format("%s_latestStageInstances", getClass().getName());
    }

    private String cacheKeyForStageCountForGraph(String pipelineName, String stageName) {
        return String.format("%s_totalStageCountForChart_%s_%s", getClass().getName(), pipelineName, stageName).intern();
    }

    private void removeFromCache(String key) {
        synchronized (key) {
            goCache.remove(key);
        }
    }


    String cacheKeyForMostRecentId(String pipelineName, String stageName) {
        return String.format("%s_mostRecentId_%s_%s", getClass().getName(), pipelineName, stageName).intern();
    }

    private Stage stageByIdWithBuildsWithNoAssociations(long id) {
        return stageById(id);
    }

}
