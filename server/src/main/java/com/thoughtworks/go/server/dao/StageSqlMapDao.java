/*
 * Copyright Thoughtworks, Inc.
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

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.CacheEntry;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.ClonerFactory;
import com.thoughtworks.go.util.DynamicReadWriteLock;
import com.thoughtworks.go.util.IBatisUtil;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jetbrains.annotations.TestOnly;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static java.lang.String.format;

@Component
public class StageSqlMapDao extends SqlMapClientDaoSupport implements StageDao, StageStatusListener, JobStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlSessionDaoSupport.class);
    private final CacheKeyGenerator cacheKeyGenerator;
    private final TransactionTemplate transactionTemplate;
    private final JobInstanceSqlMapDao buildInstanceDao;
    private final Cache cache;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    private final Cloner cloner = ClonerFactory.instance();
    private final DynamicReadWriteLock readWriteLock = new DynamicReadWriteLock();

    @Autowired
    public StageSqlMapDao(JobInstanceSqlMapDao buildInstanceDao,
                          Cache cache,
                          TransactionTemplate transactionTemplate,
                          SqlSessionFactory sqlSessionFactory,
                          GoCache goCache,
                          TransactionSynchronizationManager transactionSynchronizationManager) {
        super(goCache, sqlSessionFactory);
        this.buildInstanceDao = buildInstanceDao;
        this.cache = cache;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
    }

    @Override
    public Stage save(final Pipeline pipeline, final Stage stage) {
        return transactionTemplate.execute(status -> {
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
            goCache.remove(cacheKeyForStageHistoryViaCursor(pipelineName, stage.getName()));
        } finally {
            readWriteLock.releaseWriteLock(mutex);
        }
    }

    /**
     * Please call pipelineService.save(aPipeline) instead
     */
    @TestOnly
    @Override
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

    @Override
    public int getMaxStageCounter(long pipelineId, String stageName) {
        Map<String, Object> toGet = arguments("pipelineId", pipelineId).and("name", stageName).asMap();
        Integer maxCounter = getSqlMapClientTemplate().queryForObject("getMaxStageCounter", toGet);
        return maxCounter == null ? 0 : maxCounter;
    }

    @Override
    public int getCount(String pipelineName, String stageName) {
        Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
        String key = cacheKeyForStageCount(pipelineName, stageName);
        Integer count = goCache.get(key);
        if (count == null) {
            count = getSqlMapClientTemplate().queryForObject("getStageHistoryCount", toGet);
            goCache.put(key, count);
        }
        return count;
    }


    String cacheKeyForStageCount(String pipelineName, String stageName) {
        return cacheKeyGenerator.generate("numberOfStages", pipelineName, stageName);
    }

    @Override
    public Stages getStagesByPipelineId(long pipelineId) {
        Stages stageHistory = new Stages(
            getSqlMapClientTemplate().queryForList("getStagesByPipelineId", pipelineId));
        return new Stages(stageHistory);
    }

    @Override
    public int findLatestStageCounter(PipelineIdentifier pipeline, String stageName) {
        Map<String, Object> toGet = arguments("pipelineName", pipeline.getName()).and("pipelineCounter",
            pipeline.getCounter()).and(
            "stageName", stageName).asMap();
        Integer maxCounter = getSqlMapClientTemplate().queryForObject(
            "findMaxStageCounter", toGet);
        return maxCounter == null ? 0 : maxCounter;
    }

    @Override
    public Stage findStageWithIdentifier(StageIdentifier identifier) {
        String cacheKey = cacheKeyForStageIdentifier(identifier);
        String cacheKeyForIdentifiers = cacheKeyForListOfStageIdentifiers(identifier);
        synchronized (cacheKeyForIdentifiers) {
            Stage stage = (Stage) goCache.get(cacheKeyForIdentifiers, cacheKey);
            if (stage == null) {
                IBatisUtil.IBatisArgument argument = IBatisUtil.arguments("pipelineName", identifier.getPipelineName())
                    .and("pipelineCounter", identifier.getPipelineCounter())
                    .and("stageName", identifier.getStageName())
                    .and("stageCounter", Integer.parseInt(identifier.getStageCounter()));
                if (identifier.getPipelineLabel() != null) {
                    argument = argument.and("pipelineLabel", identifier.getPipelineLabel());
                }
                Map<String, Object> arguments = argument.asMap();
                stage = getSqlMapClientTemplate().queryForObject("findStageWithJobsByIdentifier", arguments);
                if (stage == null) {
                    return new NullStage(identifier.getStageName());
                }
                goCache.put(cacheKeyForIdentifiers, cacheKey, stage);
            }
            return cloner.deepClone(stage);
        }
    }

    String cacheKeyForListOfStageIdentifiers(StageIdentifier stageIdentifier) {
        return cacheKeyGenerator.generate("stageRunIdentifier", stageIdentifier.getPipelineName(), stageIdentifier.getPipelineCounter(),
            stageIdentifier.getStageName());
    }

    String cacheKeyForStageIdentifier(StageIdentifier stageIdentifier) {
        return cacheKeyGenerator.generate("stageIdentifier", stageIdentifier.getPipelineName(), stageIdentifier.getPipelineCounter(),
            stageIdentifier.getStageName(), stageIdentifier.getStageCounter());
    }

    @Override
    public Duration getExpectedDuration(String pipelineName, String stageName, JobInstance job) {
        Long durationSecs = getDurationOfLastSuccessfulOnAgent(pipelineName, stageName, job);
        return durationSecs == null ? Duration.ZERO : Duration.ofSeconds(durationSecs);
    }


    // TODO: (ketan) do we really care about duration on this agent, can't we just average the last few runs?
    // TODO: this performans (miserably) on an environment with elastic agents, and there is no result or ETA
    // TODO: to show because of this implementation
    @Override
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
                    LOGGER.warn("refresh cancelled for {}", key);
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
            getSqlMapClientTemplate().queryForObject("getLastSuccessfulBuildIdOnAgent", toGet);
        if (buildId == null) {
            return null;
        }
        JobInstance mostRecent = buildInstanceDao.buildByIdWithTransitions(buildId);
        return mostRecent.durationOfCompletedBuildInSeconds();
    }

    @Override
    public int getMaxStageOrder(long pipelineId) {
        Integer order = getSqlMapClientTemplate().queryForObject("getMaxStageOrder", pipelineId);
        return (order == null ? 0 : order);
    }

    @Override
    public Integer getStageOrderInPipeline(long pipelineId, String stageName) {
        Map<String, Object> params = arguments("pipelineId", pipelineId).and("stageName", stageName).asMap();
        return getSqlMapClientTemplate().queryForObject("getStageOrderInPipeline", params);
    }

    @Override
    public void updateResult(final Stage stage, final StageResult result, String username) {
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
            .and("cancelledBy", username)
            .and("completedByTransitionId", stage.getCompletedByTransitionId()).asMap());

        updateLastTransitionedTime(stage);
    }

    private void updateLastTransitionedTime(Stage stage) {
        Timestamp lastTransitionedTime = getSqlMapClientTemplate().queryForObject("getLastTransitionedTimeByStageId", stage.getId());
        stage.setLastTransitionedTime(lastTransitionedTime);
    }

    @Override
    public Stage mostRecentCompleted(StageConfigIdentifier identifier) {
        Map<String, Object> toGet = arguments("pipelineName", identifier.getPipelineName())
            .and("stageName", identifier.getStageName()).asMap();
        return getSqlMapClientTemplate().queryForObject("getMostRecentCompletedStage", toGet);
    }

    @Override
    public Stage mostRecentStage(StageConfigIdentifier identifier) {
        Map<String, Object> toGet = arguments("pipelineName", identifier.getPipelineName())
            .and("stageName", identifier.getStageName()).asMap();
        return getSqlMapClientTemplate().queryForObject("getMostRecentStage", toGet);
    }

    @Override
    public List<JobInstance> mostRecentJobsForStage(String pipelineName, String stageName) {
        Long mostRecentId = mostRecentId(pipelineName, stageName);

        Stage stage = stageByIdWithBuildsWithNoAssociations(mostRecentId);

        return new ArrayList<>(stage.getJobInstances());
    }


    @Override
    public Stages getPassedStagesByName(String pipelineName, String stageName, int limit, int offset) {
        Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName)
            .and("limit", limit).and("offset", offset).asMap();
        return new Stages(getSqlMapClientTemplate().queryForList("allPassedStagesByName", toGet));
    }

    @Override
    public List<StageAsDMR> getPassedStagesAfter(StageIdentifier stageIdentifier, int limit, int offset) {
        Stage laterThan = findStageWithIdentifier(stageIdentifier);

        Map<String, Object> toGet = arguments("pipelineName", stageIdentifier.getPipelineName()).and("stageName", stageIdentifier.getStageName())
            .and("laterThan", laterThan.getId()).and("limit", limit).and("offset", offset).asMap();

        return getSqlMapClientTemplate().queryForList("allPassedStageAsDMRsAfter", toGet);
    }

    @Override
    public Stages getAllRunsOfStageForPipelineInstance(String pipelineName, Integer pipelineCounter, String stageName) {
        String cacheKeyForAllStages = cacheKeyForAllStageOfPipeline(pipelineName, pipelineCounter, stageName);
        synchronized (cacheKeyForAllStages) {
            List<Stage> stages = goCache.get(cacheKeyForAllStages);
            if (stages == null) {
                Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).and("stageName", stageName).asMap();
                stages = getSqlMapClientTemplate().queryForList("getAllRunsOfStageForPipelineInstance", toGet);
                goCache.put(cacheKeyForAllStages, stages);
            }
            return new Stages(cloner.deepClone(stages));
        }
    }

    String cacheKeyForAllStageOfPipeline(String pipelineName, Integer pipelineCounter, String stageName) {
        return cacheKeyGenerator.generate("allStageOfPipeline", new CaseInsensitiveString(pipelineName), pipelineCounter, new CaseInsensitiveString(stageName));
    }

    @Override
    public List<Stage> findStageHistoryForChart(String pipelineName, String stageName, int pageSize, int offset) {
        Map<String, Object> args =
            arguments("pipelineName", pipelineName).
                and("stageName", stageName).
                and("offset", offset).
                and("limit", pageSize).asMap();
        return new Stages(getSqlMapClientTemplate().queryForList("findStageHistoryForChartPerPipeline", args));
    }

    @Override
    public int getTotalStageCountForChart(String pipelineName, String stageName) {
        String key = cacheKeyForStageCountForGraph(pipelineName, stageName);
        Integer total = goCache.get(key);
        if (total == null) {
            synchronized (key) {
                Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
                total = getSqlMapClientTemplate().queryForObject("getTotalStageCountForChart", toGet);
                goCache.put(key, total);
            }
        }
        return total;
    }

    @Override
    public List<StageIdentity> findLatestStageInstances() {
        String key = cacheKeyForLatestStageInstances();
        List<StageIdentity> stageIdentities = goCache.get(key);
        if (stageIdentities == null) {
            synchronized (key) {
                stageIdentities = goCache.get(key);
                if (stageIdentities == null) {
                    stageIdentities = getSqlMapClientTemplate().queryForList("latestStageInstances");
                    goCache.put(key, stageIdentities);
                }
            }
        }
        return stageIdentities;
    }


    @Override
    public StageHistoryPage findStageHistoryPageByNumber(final String pipelineName,
                                                         final String stageName,
                                                         final int pageNumber,
                                                         final int pageSize) {
        return findStageHistoryPage(pipelineName, stageName, () -> {
            int total = getCount(pipelineName, stageName);
            return Pagination.pageByNumber(pageNumber, total, pageSize);
        });
    }

    @Override
    public StageInstanceModels findDetailedStageHistoryViaCursor(String pipelineName, String stageName, FeedModifier feedModifier, long cursor, Integer pageSize) {
        String mutex = mutexForStageHistory(pipelineName, stageName);
        readWriteLock.acquireReadLock(mutex);
        try {
            String subKey = format("%s-%s-%s", cursor, pageSize, feedModifier.suffix());
            String key = cacheKeyForStageHistoryViaCursor(pipelineName, stageName);
            StageInstanceModels stageInstanceModels = (StageInstanceModels) goCache.get(key, subKey);
            if (stageInstanceModels == null) {
                stageInstanceModels = new StageInstanceModels();
                Map<String, Object> args = arguments("pipelineName", pipelineName)
                    .and("stageName", stageName)
                    .and("suffix", feedModifier.suffix())
                    .and("cursor", cursor)
                    .and("limit", pageSize).asMap();
                List<StageInstanceModel> detailedStageHistory = getSqlMapClientTemplate().queryForList("getStageHistoryViaCursor", args);
                stageInstanceModels.addAll(detailedStageHistory);
                goCache.put(key, subKey, stageInstanceModels);
            }
            return cloner.deepClone(stageInstanceModels);
        } finally {
            readWriteLock.releaseReadLock(mutex);
        }
    }

    @Override
    public PipelineRunIdInfo getOldestAndLatestStageInstanceId(String pipelineName, String stageName) {
        Map<String, Object> params = arguments("pipelineName", pipelineName)
            .and("stageName", stageName).asMap();
        return getSqlMapClientTemplate().queryForObject("getOldestAndLatestStageInstanceRun", params);
    }

    @Override
    public StageHistoryPage findStageHistoryPage(final Stage stage, final int pageSize) {
        final StageIdentifier id = stage.getIdentifier();
        return findStageHistoryPage(id.getPipelineName(), id.getStageName(), () -> {
            int total = getCount(id.getPipelineName(), id.getStageName());
            int offset = findOffsetForStage(stage);
            return Pagination.pageFor(offset, total, pageSize);
        });
    }

    public StageHistoryPage findStageHistoryPage(String pipelineName,
                                                 String stageName,
                                                 Supplier<Pagination> function) {
        //IMPORTANT: wire cache clearing on job-state-change for me, the day StageHistoryEntry gets jobs - Sachin & JJ
        String mutex = mutexForStageHistory(pipelineName, stageName);
        readWriteLock.acquireReadLock(mutex);
        try {
            Pagination pagination = function.get();
            String subKey = format("%s-%s", pagination.getCurrentPage(), pagination.getPageSize());
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
        return getSqlMapClientTemplate().queryForObject("findStageHistoryEntryBefore", args);
    }

    String mutexForStageHistory(String pipelineName, String stageName) {
        return format("%s_stageHistoryMutex_%s_<>_%s", getClass().getName(), pipelineName, stageName).intern();
    }

    String cacheKeyForStageHistories(String pipelineName, String stageName) {
        return cacheKeyGenerator.generate("stageHistories", new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    String cacheKeyForStageHistoryViaCursor(String pipelineName, String stageName) {
        return cacheKeyGenerator.generate("findDetailedStageHistoryViaCursor", pipelineName, stageName);
    }

    @Override
    public List<StageIdentifier> findFailedStagesBetween(String pipelineName,
                                                         String stageName,
                                                         double fromNaturalOrder,
                                                         double toNaturalOrder) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).
            and("stageName", stageName).
            and("fromNaturalOrder", fromNaturalOrder).
            and("toNaturalOrder", toNaturalOrder).asMap();
        return getSqlMapClientTemplate().queryForList("findFailedStagesBetween", args);
    }

    List<StageHistoryEntry> findStages(Pagination pagination, String pipelineName, String stageName) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).
            and("stageName", stageName).
            and("limit", pagination.getPageSize()).
            and("offset", pagination.getOffset()).asMap();
        return getSqlMapClientTemplate().queryForList("findStageHistoryPage", args);
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
            offset = getSqlMapClientTemplate().queryForObject("findOffsetForStage", args);
            goCache.put(key, String.valueOf(stage.getId()), offset);
        }

        return offset;
    }

    String cacheKeyForStageOffset(Stage stage) {
        return cacheKeyGenerator.generate("stageOffsetMap", stage.getIdentifier().getPipelineName(), stage.getIdentifier().getStageName()).intern();
    }

    @Override
    public List<StageFeedEntry> findAllCompletedStages(FeedModifier modifier, long id, int pageSize) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", id);
        parameters.put("pageLimit", pageSize);
        return getSqlMapClientTemplate().queryForList("allCompletedStages" + modifier.suffix(), parameters);
    }

    @Override
    public List<StageFeedEntry> findStageFeedBy(String pipelineName,
                                                Integer pipelineCounter,
                                                FeedModifier feedModifier,
                                                long pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("pipelineName", pipelineName);
        params.put("pipelineCounter", pipelineCounter);
        params.put("pageLimit", pageSize);
        params.put("feedModifier", feedModifier != null ? feedModifier.suffix() : null);
        return getSqlMapClientTemplate().queryForList("stageFeedByPipelineNameAndCounter", params);
    }

    @Override
    public List<StageFeedEntry> findCompletedStagesFor(String pipelineName,
                                                       FeedModifier feedModifier,
                                                       long transitionId,
                                                       long pageSize) {
        return _findCompletedStagesFor(pipelineName, feedModifier, transitionId, pageSize);
    }

    private List<StageFeedEntry> _findCompletedStagesFor(String pipelineName,
                                                         FeedModifier feedModifier,
                                                         long transitionId,
                                                         long pageSize) {
        //IMPORTANT: wire cache clearing on job-state-change for me, the day FeedEntry gets jobs - Sachin & JJ
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", transitionId);
        parameters.put("pageLimit", pageSize);
        parameters.put("pipelineName", pipelineName);
        return getSqlMapClientTemplate().queryForList("allCompletedStagesForPipeline" + feedModifier.suffix(), parameters);
    }

    @Override
    public Stage mostRecentWithBuilds(String pipelineName, StageConfig stageConfig) {
        Long mostRecentId = mostRecentId(pipelineName, CaseInsensitiveString.str(stageConfig.name()));
        return mostRecentId == null ? NullStage.createNullStage(stageConfig) : stageByIdWithBuildsWithNoAssociations(mostRecentId);
    }

    Long mostRecentId(String pipelineName, String stageName) {
        String key = cacheKeyForMostRecentId(pipelineName, stageName);
        Long id = goCache.get(key);
        if (id != null) {
            return id;
        }
        synchronized (key) {
            id = goCache.get(key);
            if (id != null) {
                return id;
            }
            Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
            id = getSqlMapClientTemplate().queryForObject("getMostRecentId", toGet);
            goCache.put(key, id);
        }
        return id;
    }

    @Override
    public void stageStatusChanged(Stage stage) {
        String pipelineName = stage.getIdentifier().getPipelineName();
        String stageName = stage.getName();
        removeFromCache(cacheKeyForMostRecentId(pipelineName, stageName));
        removeFromCache(cacheKeyForPipelineAndStage(pipelineName, stageName));
        removeFromCache(cacheKeyForPipelineAndCounter(pipelineName, stage.getIdentifier().getPipelineCounter()));
        removeFromCache(cacheKeyForStageById(stage.getId()));
        removeFromCache(cacheKeyForLatestStageInstances());
    }


    @Override
    public Stage stageById(long id) {
        String key = cacheKeyForStageById(id);
        Stage stage = goCache.get(key);

        if (stage == null) {
            synchronized (key) {
                stage = goCache.get(key);
                if (stage == null) {
                    stage = getSqlMapClientTemplate().queryForObject("getStageById", id);
                    if (stage == null) {
                        throw new DataRetrievalFailureException("Unable to load related stage data for id " + id);
                    }
                    goCache.put(key, stage);
                }
            }
        }
        return cloner.deepClone(stage);
    }

    String cacheKeyForStageById(long id) {
        return cacheKeyGenerator.generate("stageById", id);
    }

    @Override
    public Stage mostRecentPassed(String pipelineName, String stageName) {
        Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
        Long mostRecentId = getSqlMapClientTemplate().queryForObject("getMostRecentPassedStageId", toGet);
        return mostRecentId == null ? null : stageById(mostRecentId);
    }

    @Override
    public boolean isStageActive(String pipelineName, String stageName) {
        String cacheKey = cacheKeyForPipelineAndStage(pipelineName, stageName);
        synchronized (cacheKey) {
            Boolean isActive = goCache.get(cacheKey);
            if (isActive == null) {
                final Map<String, Object> toGet = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
                isActive = !getSqlMapClientTemplate().queryForObject("isStageActive", toGet).equals(0);
                goCache.put(cacheKey, isActive);
            }
            return isActive;
        }
    }

    @Override
    public Stage getStageByBuild(long buildId) {
        Stage stage = getSqlMapClientTemplate().queryForObject("getStageByBuildId", buildId);
        if (stage == null) {
            throw new DataRetrievalFailureException("Unable to load related stage data for buildId " + buildId);
        }
        return stage;
    }

    @Override
    public void clearCachedStage(StageIdentifier identifier) {
        removeFromCache(cacheKeyForListOfStageIdentifiers(identifier));
    }

    @Override
    public void clearCachedAllStages(String pipelineName, int pipelineCounter, String stageName) {
        String cacheForAllStagesOfPipeline = cacheKeyForAllStageOfPipeline(pipelineName, pipelineCounter, stageName);
        removeFromCache(cacheForAllStagesOfPipeline);
    }

    String cacheKeyForPipelineAndStage(String pipelineName, String stageName) {
        return cacheKeyGenerator.generate("isStageActive", pipelineName.toLowerCase(), stageName.toLowerCase());
    }

    @Override
    public Stages findAllStagesFor(String pipelineName, int counter) {
        String key = cacheKeyForPipelineAndCounter(pipelineName, counter);
        List<Stage> stages = goCache.get(key);
        if (stages == null) {
            synchronized (key) {
                stages = goCache.get(key);
                if (stages == null) {
                    Map<String, Object> params = arguments("pipelineName", pipelineName).and("pipelineCounter", counter).asMap();
                    stages = getSqlMapClientTemplate().queryForList("getStagesByPipelineNameAndCounter", params);
                    goCache.put(key, stages);
                }
            }
        }
        return new Stages(stages);
    }

    @Override
    public List<Stage> oldestStagesHavingArtifacts() {
        return getSqlMapClientTemplate().queryForList("oldestStagesHavingArtifacts");
    }

    @Override
    public void markArtifactsDeletedFor(Stage stage) {
        getSqlMapClientTemplate().update("markStageArtifactDeleted", arguments("stageId", stage.getId()).asMap());
    }

    String cacheKeyForPipelineAndCounter(String pipelineName, int counter) {
        return cacheKeyGenerator.generate("allStagesOfPipelineInstance", pipelineName, counter);
    }

    @Override
    public void jobStatusChanged(JobInstance job) {
        clearJobStatusDependentCaches(job.getStageId(), job.getIdentifier().getStageIdentifier());
    }

    private void clearJobStatusDependentCaches(long stageId, StageIdentifier stageIdentifier) {
        removeFromCache(cacheKeyForStageById(stageId));
        clearCachedStage(stageIdentifier);
        clearCachedAllStages(stageIdentifier.getPipelineName(), stageIdentifier.getPipelineCounter(), stageIdentifier.getStageName());
    }


    String cacheKeyForLatestStageInstances() {
        return cacheKeyGenerator.generate("latestStageInstances");
    }

    String cacheKeyForStageCountForGraph(String pipelineName, String stageName) {
        return cacheKeyGenerator.generate("totalStageCountForChart", pipelineName, stageName);
    }

    private void removeFromCache(String key) {
        synchronized (key) {
            goCache.remove(key);
        }
    }


    String cacheKeyForMostRecentId(String pipelineName, String stageName) {
        return cacheKeyGenerator.generate("mostRecentId", pipelineName, stageName);
    }

    private Stage stageByIdWithBuildsWithNoAssociations(long id) {
        return stageById(id);
    }

}
