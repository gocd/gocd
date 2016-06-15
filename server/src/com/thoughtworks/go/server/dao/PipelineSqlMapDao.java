/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.dao;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineNotFoundException;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.server.util.SqlUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.thoughtworks.go.util.IBatisUtil.arguments;

@SuppressWarnings({"ALL"})
@Component
public class PipelineSqlMapDao extends SqlMapClientDaoSupport implements Initializer, PipelineDao, StageStatusListener {
    private static final Logger LOGGER = Logger.getLogger(PipelineSqlMapDao.class);
    private StageDao stageDao;
    private MaterialRepository materialRepository;
    private EnvironmentVariableDao environmentVariableDao;
    private GoCache goCache;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private final SystemEnvironment systemEnvironment;
    private final GoConfigDao configFileDao;
    private final Cloner cloner = new Cloner();
    private final ReadWriteLock activePipelineRWLock = new ReentrantReadWriteLock();
    private final Lock activePipelineReadLock = activePipelineRWLock.readLock();
    private final Lock activePipelineWriteLock = activePipelineRWLock.writeLock();

    @Autowired
    public PipelineSqlMapDao(StageDao stageDao, MaterialRepository materialRepository, GoCache goCache, EnvironmentVariableDao environmentVariableDao, TransactionTemplate transactionTemplate,
                             SqlMapClient sqlMapClient, TransactionSynchronizationManager transactionSynchronizationManager, SystemEnvironment systemEnvironment,
                             GoConfigDao configFileDao, Database database) {
        super(goCache, sqlMapClient, systemEnvironment, database);
        this.stageDao = stageDao;
        this.materialRepository = materialRepository;
        this.goCache = goCache;
        this.environmentVariableDao = environmentVariableDao;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.systemEnvironment = systemEnvironment;
        this.configFileDao = configFileDao;

    }

    @Override
    public void initialize() {
        try {
            LOGGER.info("Loading active pipelines into memory.");
            cacheActivePipelines();
            LOGGER.info("Done loading active pipelines into memory.");
        } catch (Exception e) {
            LOGGER.fatal(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Pipeline save(final Pipeline pipeline) {
        return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        goCache.remove(cacheKeyForLatestPipelineIdByPipelineName(pipeline.getName()));
                        invalidateCacheConditionallyForPipelineInstancesTriggeredWithDependencyMaterial(pipeline);
                    }
                });

                Long pipelineId = (Long) getSqlMapClientTemplate().insert("insertPipeline", pipeline);
                savePipelineMaterialRevisions(pipeline, pipelineId);
                environmentVariableDao.save(pipelineId, EnvironmentVariableSqlMapDao.EnvironmentVariableType.Trigger, pipeline.scheduleTimeVariables());
                return pipeline;
            }
        });
    }


    public Integer getCounterForPipeline(String name) {
        Integer counter = (Integer) getSqlMapClientTemplate().queryForObject("getCounterForPipeline", name);
        return counter == null ? 0 : counter;
    }

    public void insertOrUpdatePipelineCounter(Pipeline pipeline, Integer lastCount, Integer newCount) {
        Map<String, Object> args = arguments("pipelineName", pipeline.getName()).and("count", newCount).asMap();
        Integer hasPipelineRow = (Integer) getSqlMapClientTemplate().queryForObject("hasPipelineInfoRow", pipeline.getName());
        if (hasPipelineRow == 0) {
            getSqlMapClientTemplate().insert("insertPipelineLabelCounter", args);
        } else if (newCount > lastCount) {
            // Counter will not be updated when using other types of labels such as Date or Revision.
            getSqlMapClientTemplate().update("updatePipelineLabelCounter", args, 1);
        }
    }

    public Pipeline findPipelineByNameAndCounter(String name, int counter) {
        Map<String, Object> map = arguments("name", name).and("counter", counter).asMap();
        return (Pipeline) getSqlMapClientTemplate().queryForObject("findPipelineByNameAndCounter", map);
    }

    public BuildCause findBuildCauseOfPipelineByNameAndCounter(String name, int counter) {
        String cacheKey = cacheKeyForBuildCauseByNameAndCounter(name, counter);
        BuildCause buildCause = (BuildCause) goCache.get(cacheKey);
        if (buildCause == null) {
            synchronized (cacheKey) {
                buildCause = (BuildCause) goCache.get(cacheKey);
                if (buildCause == null) {
                    Pipeline pipeline = findPipelineByNameAndCounter(name, counter);
                    if (pipeline == null) {
                        throw new PipelineNotFoundException(String.format("Pipeline %s with counter %d was not found", name, counter));
                    }
                    loadMaterialRevisions(pipeline);
                    buildCause = pipeline.getBuildCause();
                    goCache.put(cacheKey, buildCause);
                }
            }
        }
        return buildCause;
    }

    private String cacheKeyForBuildCauseByNameAndCounter(String name, int counter) {
        return (PipelineSqlMapDao.class + "_buildCauseByNameAndCounter_" + name.toLowerCase() + "_and_" + counter).intern();
    }

    public Pipeline findPipelineByNameAndLabel(String name, String label) {
        Map<String, Object> map = arguments("name", name).and("label", label).asMap();
        return (Pipeline) getSqlMapClientTemplate().queryForObject("findPipelineByNameAndLabel", map);
    }

    public StageIdentifier findLastSuccessfulStageIdentifier(String pipelineName, String stageName) {
        String cacheKey = latestSuccessfulStageCacheKey(pipelineName, stageName);
        synchronized (cacheKey) {
            StageIdentifier lastStageIdentifier = (StageIdentifier) goCache.get(cacheKey);
            if (lastStageIdentifier == null) {
                lastStageIdentifier = _findLastSuccessfulStageIdentifier(pipelineName, stageName);
                goCache.put(cacheKey, lastStageIdentifier);
            }
            return lastStageIdentifier;
        }
    }

    private StageIdentifier _findLastSuccessfulStageIdentifier(String pipelineName, String stageName) {
        // This query returns a bare bones pipeline containing one bare bones stage.
        Map<String, Object> map = arguments("pipelineName", pipelineName).and("stageName", stageName).asMap();
        Pipeline lastSuccessful = (Pipeline) getSqlMapClientTemplate().queryForObject("getLastSuccessfulStageInPipeline", map);
        if (lastSuccessful == null) {
            return null;
        } else {
            Stage laststage = lastSuccessful.findStage(stageName);
            return new StageIdentifier(pipelineName, lastSuccessful.getCounter(), lastSuccessful.getLabel(), laststage.getName(), Integer.toString(laststage.getCounter()));
        }
    }

    protected void updateCachedLatestSuccessfulStage(Stage stage) {
        if (stage.passed()) {
            StageIdentifier identifier = stage.getIdentifier();
            String cacheKey = latestSuccessfulStageCacheKey(identifier.getPipelineName(), identifier.getStageName());
            synchronized (cacheKey) {
                goCache.put(cacheKey, identifier);
            }
        }
    }

    private String latestSuccessfulStageCacheKey(String pipelineName, String stageName) {
        return (PipelineSqlMapDao.class.getName() + "_latestSuccessfulStage_" + pipelineName + "-" + stageName).intern();
    }

    public void updatePauseInfo(Pipeline pipeline) {
        getSqlMapClientTemplate().update("updatePipelinePause", pipeline);
    }

    private void savePipelineMaterialRevisions(Pipeline pipeline, final Long pipelineId) {
        MaterialRevisions materialRevisions = pipeline.getBuildCause().getMaterialRevisions();
        materialRepository.createPipelineMaterialRevisions(pipeline, pipelineId, materialRevisions);
    }

    public Pipeline loadPipeline(long pipelineId) {
        Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("pipelineById", pipelineId);
        return loadAssociations(pipeline, pipeline == null ? "" : pipeline.getName());
    }

    public Pipeline mostRecentPipeline(String pipelineName) {
        Pipeline mostRecent = (Pipeline) getSqlMapClientTemplate().queryForObject("mostRecentPipeline", pipelineName);
        return loadAssociations(mostRecent, pipelineName);
    }

    public Pipeline pipelineWithMaterialsAndModsByBuildId(long buildId) {
        Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("getPipelineByBuildId", buildId);
        if (pipeline == null) {
            /* We throw this exception any time you issue a query for a specific id and it's not found */
            throw new DataRetrievalFailureException("Could not load pipeline from build with id " + buildId);
        }
        return loadMaterialRevisions(pipeline);
    }

    /**
     * This is only used in test for legacy purpose. Please call pipelineService.save(aPipeline) instead.
     * <p/>
     * This is particularly bad as it does NOT do the same as what the system does in the real code.
     * In particular it does not save the JobInstances correctly. We need to remove this method and make
     * sure that tests are using the same behaviour as the real code.
     *
     * @see { Mingle #2867}
     * @deprecated
     */
    public Pipeline saveWithStages(Pipeline pipeline) {
        updateCounter(pipeline);
        Pipeline savedPipeline = save(pipeline);
        for (Stage stage : pipeline.getStages()) {
            stageDao.saveWithJobs(savedPipeline, stage);
        }
        return savedPipeline;
    }

    private void updateCounter(Pipeline pipeline) {
        Integer lastCount = getCounterForPipeline(pipeline.getName());

        pipeline.updateCounter(lastCount);
        insertOrUpdatePipelineCounter(pipeline, lastCount, pipeline.getCounter());
    }

    public String mostRecentLabel(String pipelineName) {
        return (String) getSqlMapClientTemplate().queryForObject("mostRecentLabel", pipelineName);
    }


    public Pipeline fullPipelineByBuildId(long buildId) {
        Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("getPipelineByBuildId", buildId);
        if (pipeline == null) {
            /* We throw this exception any time you issue a query for a specific id and it's not found */
            throw new DataRetrievalFailureException("Could not load pipeline from build with id " + buildId);
        }
        return loadAssociations(pipeline, pipeline.getName());
    }

    public Pipeline pipelineByBuildIdWithMods(long buildId) {
        Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("getPipelineByBuildId", buildId);
        if (pipeline == null) {
            throw new DataRetrievalFailureException("Could not load pipeline from build with id " + buildId);
        }
        return loadMaterialRevisions(pipeline);
    }

    public Pipeline pipelineByIdWithMods(long pipelineId) {
        Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("pipelineById", pipelineId);
        if (pipeline == null) {
            throw new DataRetrievalFailureException("Could not load pipeline with id " + pipelineId);
        }
        return loadMaterialRevisions(pipeline);
    }

    public Pipeline pipelineWithModsByStageId(String pipelineName, long stageId) {
        String cacheKey = cacheKeyForPipelineWithStageId(pipelineName, stageId);
        synchronized (cacheKey) {
            Pipeline pipeline = (Pipeline) goCache.get(cacheKey);
            if (pipeline == null) {
                pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("getPipelineByStageId", stageId);
                if (pipeline == null) {
                    return new NullPipeline(pipelineName);
                }
                goCache.put(cacheKey, loadMaterialRevisions(pipeline));
            }
            return pipeline;
        }
    }

    private String cacheKeyForPipelineWithStageId(String pipelineName, long stageId) {
        return (PipelineSqlMapDao.class + "_pipelineWithStageId_" + pipelineName.toLowerCase() + "_" + stageId).intern();
    }

    public Pipeline loadAssociations(Pipeline pipeline, String pipelineName) {
        pipeline = loadStages(pipeline);
        pipeline = loadMaterialRevisions(pipeline);
        pipeline = loadEnvironmentVariables(pipeline);
        return pipeline == null ? new NullPipeline(pipelineName) : pipeline;
    }

    private Pipeline loadEnvironmentVariables(Pipeline pipeline) {
        if (pipeline == null) {
            return pipeline;
        }
        pipeline.getBuildCause().setVariables(environmentVariableDao.load(pipeline.getId(), EnvironmentVariableSqlMapDao.EnvironmentVariableType.Trigger));
        return pipeline;
    }

    public PipelineInstanceModels loadHistory(String pipelineName) {
        return PipelineInstanceModels.createPipelineInstanceModels(
                (List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("getAllPipelineHistoryByName", arguments("name", pipelineName).asMap()));
    }

    public PipelineInstanceModel findPipelineHistoryByNameAndCounter(String pipelineName, int pipelineCounter) {
        PipelineInstanceModel instanceModel = loadPipelineInstanceModelByNameAndCounter(pipelineName, pipelineCounter);
        loadPipelineHistoryBuildCause(instanceModel);
        return instanceModel;
    }

    private PipelineInstanceModel loadPipelineInstanceModelByNameAndCounter(String pipelineName, int pipelineCounter) {
        String cacheKey = cacheKeyForPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter);
        PipelineInstanceModel instanceModel = (PipelineInstanceModel) goCache.get(cacheKey);;
        if (instanceModel == null) {
            synchronized (cacheKey) {
                instanceModel = (PipelineInstanceModel) goCache.get(cacheKey);
                if (instanceModel == null) {
                    instanceModel = (PipelineInstanceModel) getSqlMapClientTemplate().queryForObject("getPipelineHistoryByNameAndCounter",
                            arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).asMap());
                    goCache.put(cacheKey, instanceModel);
                }
            }
        }
        return instanceModel;
    }

    private String cacheKeyForPipelineHistoryByNameAndCounter(String pipelineName, int pipelineCounter) {
        return (PipelineSqlMapDao.class + "_cacheKeyForPipelineHistoryByName_" + pipelineName.toLowerCase() + "_AndCounter_" + pipelineCounter).intern();
    }

    public PipelineDependencyGraphOld pipelineGraphByNameAndCounter(String pipelineName, int pipelineCounter) {
        PipelineInstanceModels instanceModels = null;
        try {
            instanceModels = PipelineInstanceModels.createPipelineInstanceModels((List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("pipelineAndItsDepedenciesByNameAndCounter",
                    arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter)
                            .and("stageLocator", pipelineName + "/" + pipelineCounter + "/%/%")
                            .asMap()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (instanceModels.isEmpty()) {
            return null;
        }
        PipelineInstanceModel upstreamPipeline = instanceModels.find(pipelineName);
        loadPipelineHistoryBuildCause(upstreamPipeline);
        return new PipelineDependencyGraphOld(upstreamPipeline, dependentPipelines(upstreamPipeline, instanceModels));
    }

    public Pipeline findEarlierPipelineThatPassedForStage(String pipelineName, String stageName, double naturalOrder) {
        return (Pipeline) getSqlMapClientTemplate().queryForObject("findEarlierPipelineThatPassedForStage",
                arguments("pipelineName", pipelineName).and("stageName", stageName).and("naturalOrder", naturalOrder).asMap());
    }

    private PipelineInstanceModels dependentPipelines(PipelineInstanceModel upstreamPipeline, List<PipelineInstanceModel> instanceModels) {
        instanceModels.remove(upstreamPipeline);
        return PipelineInstanceModels.createPipelineInstanceModels(instanceModels);
    }


    public void cacheActivePipelines() {
        LOGGER.info("Retriving Active Pipelines from Database...");
        final List<PipelineInstanceModel> pipelines = getAllPIMs();
        if (pipelines.isEmpty()) {
            return;
        }
        Thread[] loaderThreads = loadActivePipelineAndHistoryToCache(pipelines);
        cacheMaterialRevisions(pipelines);
        waitForLoaderThreadsToJoin(loaderThreads);
    }

    private List<PipelineInstanceModel> getAllPIMs() {
        return getSqlMapClientTemplate().queryForList("allActivePipelines");
    }

    private List<CaseInsensitiveString> getPipelineNamesInConfig() {
        return configFileDao.load().getAllPipelineNames();
    }

    private void waitForLoaderThreadsToJoin(Thread[] loaderThreads) {
        for (Thread loaderThread : loaderThreads) {
            try {
                loaderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Thread[] loadActivePipelineAndHistoryToCache(final List<PipelineInstanceModel> pipelines) {
        final Thread activePipelinesCacheLoader = new Thread() {
            @Override
            public void run() {
                LOGGER.info("Loading Active Pipelines to cache...Started");
                Map<String, TreeSet<Long>> result = groupPipelineInstanceIdsByPipelineName(pipelines);
                goCache.put(activePipelinesCacheKey(), result);
                LOGGER.info("Loading Active Pipelines to cache...Done");
            }
        };
        final Thread historyCacheLoader = new Thread() {
            @Override
            public void run() {
                LOGGER.info("Loading pipeline history to cache...Started");
                for (PipelineInstanceModel pipeline : pipelines) {
                    goCache.put(pipelineHistoryCacheKey(pipeline.getId()), pipeline);
                }
                LOGGER.info("Loading pipeline history to cache...Done");
            }
        };
        historyCacheLoader.start();
        activePipelinesCacheLoader.start();
        return new Thread[]{activePipelinesCacheLoader, historyCacheLoader};
    }

    public PipelineInstanceModels loadActivePipelines() {
        String cacheKey = activePipelinesCacheKey();
        Map<String, TreeSet<Long>> result = (Map<String, TreeSet<Long>>) goCache.get(cacheKey);
        if (result == null) {
            synchronized (cacheKey) {
                result = (Map<String, TreeSet<Long>>) goCache.get(cacheKey);
                if (result == null) {
                    List<PipelineInstanceModel> pipelines = getAllPIMs();
                    result = groupPipelineInstanceIdsByPipelineName(pipelines);
                    goCache.put(cacheKey, result);
                }
            }
        }
        return convertToPipelineInstanceModels(result);
    }

    private void cacheMaterialRevisions(List<PipelineInstanceModel> models) {
        List<CaseInsensitiveString> pipelinesInConfig = getPipelineNamesInConfig();
        if (pipelinesInConfig.isEmpty()) {
            LOGGER.warn("No pipelines found in Config, Skipping material revision caching.");
            return;
        }
        Set<Long> ids = new HashSet<Long>();
        for (PipelineInstanceModel model : models) {
            if (pipelinesInConfig.contains(new CaseInsensitiveString(model.getName()))) {
                ids.add(model.getId());
            }

        }
        if (ids.isEmpty()) {
            LOGGER.warn("No PIMs found in Config, Skipping material revision caching.");
            return;
        }
        materialRepository.cacheMaterialRevisionsForPipelines(ids);
    }

    private PipelineInstanceModels convertToPipelineInstanceModels(Map<String, TreeSet<Long>> result) {
        List<PipelineInstanceModel> models = new ArrayList<PipelineInstanceModel>();

        List<CaseInsensitiveString> pipelinesInConfig= getPipelineNamesInConfig();
        if (pipelinesInConfig.isEmpty()) {
            LOGGER.warn("No pipelines found in Config, Skipping PIM loading.");
            return PipelineInstanceModels.createPipelineInstanceModels(models);
        }

        List<Long> pipelineIds = loadIdsFromHistory(result);
        int collectionCount = pipelineIds.size();
        for (int i = 0; i < collectionCount; i++) {
            Long id = pipelineIds.get(i);
            PipelineInstanceModel model = loadHistory(id);
            if(model == null){
                continue;
            }
            if (!pipelinesInConfig.contains(new CaseInsensitiveString(model.getName()))) {
                LOGGER.debug("Skipping PIM for pipeline " + model.getName() + " ,since its not found in current config");
                continue;
            }
            models.add(model);
            loadPipelineHistoryBuildCause(model);

        }
        return PipelineInstanceModels.createPipelineInstanceModels(models);
    }

    private List<Long> loadIdsFromHistory(Map<String, TreeSet<Long>> result) {
        List<Long> idsForHistory = new ArrayList<Long>();
        try {
            activePipelineReadLock.lock();
            for (Map.Entry<String, TreeSet<Long>> pipelineToIds : result.entrySet()) {
                idsForHistory.addAll(pipelineToIds.getValue().descendingSet());
            }
        } finally {
            activePipelineReadLock.unlock();
        }
        return idsForHistory;
    }

    public PipelineInstanceModel loadHistoryByIdWithBuildCause(Long id) {
        PipelineInstanceModel model = loadHistory(id);
        loadPipelineHistoryBuildCause(model);
        return model;
    }

    private List<Long> loadPipelineIdsFor(String pipelineName, List<Integer> counters) {
        return getSqlMapClientTemplate().queryForList("pipelineIdsForCounters", arguments("counters", SqlUtil.joinWithQuotesForSql(counters.toArray())).and("name", pipelineName).asMap());
    }

    private Map<String, TreeSet<Long>> groupPipelineInstanceIdsByPipelineName(List<PipelineInstanceModel> pipelines) {
        Map<String, TreeSet<Long>> result;
        result = new HashMap<String, TreeSet<Long>>();
        for (PipelineInstanceModel pipeline : pipelines) {
            TreeSet<Long> ids = initializePipelineInstances(result, pipeline.getName());
            ids.add(pipeline.getId());
        }
        return result;
    }

    String activePipelinesCacheKey() {
        return (PipelineSqlMapDao.class.getName() + "_activePipelines").intern();
    }

    public PipelineInstanceModel loadHistory(long id) {
        String cacheKey = pipelineHistoryCacheKey(id);
        PipelineInstanceModel result = (PipelineInstanceModel) goCache.get(cacheKey);
        if (result == null) {
            synchronized (cacheKey) {
                result = (PipelineInstanceModel) goCache.get(cacheKey);
                if (result == null) {
                    result = (PipelineInstanceModel) getSqlMapClientTemplate().queryForObject("getPipelineHistoryById", arguments("id", id).asMap());
                    if (result == null) {
                        return null;
                    }
                    goCache.put(cacheKey, result);
                }
            }
        }
        return cloner.deepClone(result);
    }

    public void stageStatusChanged(Stage stage) {
        removeStageSpecificCache(stage);
        syncCachedActivePipelines(stage);
        updateCachedLatestSuccessfulStage(stage);
        String pipelineName = stage.getIdentifier().getPipelineName();
        Integer pipelineCounter = stage.getIdentifier().getPipelineCounter();
        clearLockedPipelineCache(pipelineName);
        clearPipelineHistoryCacheViaNameAndCounter(pipelineName, pipelineCounter);
    }

    private void clearPipelineHistoryCacheViaNameAndCounter(String pipelineName, Integer pipelineCounter) {
        goCache.remove(cacheKeyForPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter));
    }

    private void clearLockedPipelineCache(String pipelineName) {
        goCache.remove(lockedPipelineCacheKey(pipelineName));
    }

    private void syncCachedActivePipelines(Stage stage) {
        Map<String, TreeSet<Long>> activePipelinesToIds = (Map<String, TreeSet<Long>>) goCache.get(activePipelinesCacheKey());
        if (activePipelinesToIds == null) {
            return;
        }
        String pipelineName = loadHistory(stage.getPipelineId()).getName();
        try {
            activePipelineWriteLock.lock();
            addActiveAsLatest(stage, activePipelinesToIds, pipelineName);
            removeCompletedIfNotLatest(stage, activePipelinesToIds, pipelineName);
        } finally {
            activePipelineWriteLock.unlock();
        }
    }

    private void addActiveAsLatest(Stage stage, Map<String, TreeSet<Long>> activePipelinesToIds, String pipelineName) {
        if (stage.getState().isActive()) {
            TreeSet<Long> ids = initializePipelineInstances(activePipelinesToIds, pipelineName);
            removeCurrentLatestIfNoLongerActive(stage, ids);
            ids.add(stage.getPipelineId());
        }
    }

    private void removeCompletedIfNotLatest(Stage stage, Map<String, TreeSet<Long>> activePipelinesToIds, String pipelineName) {
        if (stage.getState().completed()) {
            if (activePipelinesToIds.containsKey(pipelineName)) {
                TreeSet<Long> ids = activePipelinesToIds.get(pipelineName);
                if (!ids.last().equals(stage.getPipelineId())) {
                    ids.remove(stage.getPipelineId());
                }
            }
        }
    }

    private void removeCurrentLatestIfNoLongerActive(Stage stage, TreeSet<Long> ids) {
        if (!ids.isEmpty()) {
            if (isNewerThanCurrentLatest(stage, ids) && isCurrentLatestInactive(ids)) {
                ids.remove(ids.last());
            }
        }
    }

    private boolean isNewerThanCurrentLatest(Stage stage, TreeSet<Long> ids) {
        return stage.getPipelineId() > ids.last();
    }

    private boolean isCurrentLatestInactive(TreeSet<Long> ids) {
        return !loadHistory(ids.last()).isAnyStageActive();
    }

    private TreeSet<Long> initializePipelineInstances(Map<String, TreeSet<Long>> pipelineToIds, String pipelineName) {
        if (!pipelineToIds.containsKey(pipelineName)) {
            pipelineToIds.put(pipelineName, new TreeSet<Long>());
        }
        return pipelineToIds.get(pipelineName);
    }

    private void removeStageSpecificCache(Stage stage) {
        goCache.remove(pipelineHistoryCacheKey(stage.getPipelineId()));
        goCache.remove(cacheKeyForlatestPassedStage(stage.getPipelineId(), stage.getName()));
    }

    String pipelineHistoryCacheKey(Long id) {
        return (PipelineSqlMapDao.class.getName() + "_pipelineHistory_" + id).intern();
    }

    public PipelineInstanceModels loadHistory(String pipelineName, int limit, int offset) {
        List<Long> ids = findPipelineIds(pipelineName, limit, offset);
        if (ids.size() == 1) {
            return PipelineInstanceModels.createPipelineInstanceModels(loadHistoryByIdWithBuildCause(ids.get(0)));
        }
        return loadHistory(pipelineName, ids);
    }

    public PipelineInstanceModels loadHistory(String pipelineName, int count, String startingLabel) {
        if (startingLabel.equals("latest")) {
            List<Long> ids = findPipelineIds(pipelineName, count, 0);
            return loadHistory(pipelineName, ids);
        } else {
            Map<String, Object> toGet =
                    arguments("pipelineName", pipelineName)
                            .and("pipelineLabel", startingLabel)
                            .and("limit", count).asMap();
            List<Long> ids = getSqlMapClientTemplate().queryForList("getPipelineRangeForLabel", toGet);

            return loadHistory(pipelineName, ids);
        }
    }

    public int getPageNumberForCounter(String pipelineName, int pipelineCounter, int limit) {
        Integer maxCounter = getCounterForPipeline(pipelineName);
        Pagination pagination = Pagination.pageStartingAt((maxCounter - pipelineCounter), maxCounter, limit);
        return pagination.getCurrentPage();
    }

    public PipelineInstanceModels findMatchingPipelineInstances(String pipelineName, String pattern, int limit) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).
                and("pattern", "%" + pattern.toLowerCase() + "%").
                and("rawPattern", pattern.toLowerCase()).
                and("limit", limit).asMap();
        long begin = System.currentTimeMillis();
        List<PipelineInstanceModel> matchingPIMs = (List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("findMatchingPipelineInstances", args);
        List<PipelineInstanceModel> exactMatchingPims = (List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("findExactMatchingPipelineInstances", args);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[Compare Pipelines] Query initiated for pipeline %s with pattern %s. Query execution took %s milliseconds", pipelineName, pattern,
                    System.currentTimeMillis() - begin));
        }
        exactMatchingPims.addAll(matchingPIMs);
        return PipelineInstanceModels.createPipelineInstanceModels(exactMatchingPims);
    }


    List<Long> findPipelineIds(String pipelineName, int limit, int offset) {
        if (wantLatestIdOnly(limit, offset)) {
            String cacheKey = cacheKeyForLatestPipelineIdByPipelineName(pipelineName);
            List<Long> ids = (List<Long>) goCache.get(cacheKey);
            if (ids == null) {
                synchronized (cacheKey) {
                    ids = (List<Long>) goCache.get(cacheKey);
                    if (ids == null) {
                        ids = fetchPipelineIds(pipelineName, limit, offset);
                        goCache.put(cacheKey, ids);
                    }
                }
            }
            return ids;
        } //dont bother caching if looking for more than the latest, because limit and offset may changed
        return fetchPipelineIds(pipelineName, limit, offset);
    }

    private boolean wantLatestIdOnly(int limit, int offset) {
        return limit == 1 && offset == 0;
    }

    private List<Long> fetchPipelineIds(String pipelineName, int limit, int offset) {
        List<Long> ids;
        Map<String, Object> toGet =
                arguments("pipelineName", pipelineName)
                        .and("limit", limit)
                        .and("offset", offset).asMap();
        ids = getSqlMapClientTemplate().queryForList("getPipelineRange", toGet);
        return ids;
    }

    private String cacheKeyForLatestPipelineIdByPipelineName(String pipelineName) {
        return (PipelineSqlMapDao.class + "_latestPipelineIdByPipelineName_" + pipelineName.toLowerCase()).intern();
    }

    private PipelineInstanceModels loadHistory(String pipelineName, List<Long> ids) {
        if (ids.isEmpty()) {
            return PipelineInstanceModels.createPipelineInstanceModels();
        }

        Map<String, Object> args = arguments("pipelineName", pipelineName)
                .and("from", Collections.min(ids))
                .and("to", Collections.max(ids)).asMap();
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels(
                (List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("getPipelineHistoryByName", args));
        for (PipelineInstanceModel pipelineInstanceModel : history) {
            loadPipelineHistoryBuildCause(pipelineInstanceModel);
        }
        return history;
    }

    public int count(String pipelineName) {
        return (Integer) getSqlMapClientTemplate().queryForObject("getPipelineHistoryCount", pipelineName);
    }

    private Pipeline loadStages(Pipeline pipeline) {
        if (pipeline == null) {
            return pipeline;
        }
        Stages stages = stageDao.getStagesByPipelineId(pipeline.getId());
        pipeline.setStages(stages);
        return pipeline;
    }

    public void setStageDao(StageSqlMapDao stageDao) {
        this.stageDao = stageDao;
    }

    private Pipeline loadMaterialRevisions(Pipeline pipeline) {
        if (pipeline == null) {
            return pipeline;
        }
        long pipelineId = pipeline.getId();

        MaterialRevisions revisions = materialRepository.findMaterialRevisionsForPipeline(pipelineId);
        pipeline.setModificationsOnBuildCause(revisions);

        return pipeline;
    }

    private PipelineInstanceModel loadPipelineHistoryBuildCause(PipelineInstanceModel pipeline) {
        if (pipeline != null) {
            MaterialRevisions materialRevisions = materialRepository.findMaterialRevisionsForPipeline(pipeline.getId());
            pipeline.setMaterialRevisionsOnBuildCause(materialRevisions);
        }
        return pipeline;
    }

    static String getLatestRevisionFromOrderedLists(List<Modification> orderedList1, List<Modification> orderedList2) {
        Modification latestModification = null;

        if (!orderedList1.isEmpty()) {
            latestModification = orderedList1.get(0);
        }
        if (!orderedList2.isEmpty()) {
            Modification modification = orderedList2.get(0);
            if (latestModification == null) {
                latestModification = modification;
            } else if (modification.getModifiedTime().compareTo(latestModification.getModifiedTime()) > 0) {
                latestModification = modification;
            }
        }
        return latestModification != null ? latestModification.getRevision() : null;
    }

    /**
     * ReadWriteLock is used because the profiler output shows that the synchronized block in lockedPipeline is
     * highly contended, and reads (lockedPipeline) far outnumber the writes (lockPipeline/unlockPipeline)
     * <p/>
     * Lock protects two pipeline instances of same pipeline from getting locked(pipeline lock) at the same time.
     */
    private static final ReadWriteLock lockPipelineMutex = new ReentrantReadWriteLock();

    public void lockPipeline(final Pipeline pipeline) {
        lockPipelineMutex.writeLock().lock();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                final String pipelineName = pipeline.getName();
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        clearLockedPipelineCache(pipelineName);
                        lockPipelineMutex.writeLock().unlock();
                    }
                });
                StageIdentifier identifier = lockedPipeline(pipelineName);
                if (identifier != null && !identifier.pipelineIdentifier().equals(pipeline.getIdentifier())) {
                    throw new RuntimeException(String.format("Pipeline '%s' is already locked (counter = %s)", pipelineName, identifier.getPipelineCounter()));
                }
                getSqlMapClientTemplate().update("lockPipeline", pipeline.getId());
            }
        });
    }

    /**
     * Used in the cache to indicate that a pipeline is not locked
     */

    private static final StageIdentifier NOT_LOCKED = new StageIdentifier("NOT_LOCKED", 0, "NOT_LOCKED", null);

    public void unlockPipeline(final String pipelineName) {
        lockPipelineMutex.writeLock().lock();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                final String cacheKey = lockedPipelineCacheKey(pipelineName);
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        goCache.remove(cacheKey);
                        lockPipelineMutex.writeLock().unlock();
                    }
                });
                getSqlMapClientTemplate().update("unlockLockedPipeline", pipelineName);
            }
        });
    }


    public StageIdentifier lockedPipeline(String pipelineName) {
        String cacheKey = lockedPipelineCacheKey(pipelineName);
        lockPipelineMutex.readLock().lock();
        try {
            StageIdentifier lockedBy = (StageIdentifier) goCache.get(cacheKey);
            if (lockedBy != null) {
                // We should not do a reference equals here because the cached object may get serialized and deserialized, in which case == will not work
                return lockedBy.equals(NOT_LOCKED) ? null : lockedBy;
            }
            lockedBy = (StageIdentifier) getSqlMapClientTemplate().queryForObject("lockedPipeline", pipelineName);
            goCache.put(cacheKey, lockedBy == null ? NOT_LOCKED : lockedBy);
            return lockedBy;
        } finally {
            lockPipelineMutex.readLock().unlock();
        }
    }

    String lockedPipelineCacheKey(String pipelineName) {
        // we intern() it because we synchronize on the returned String
        return (PipelineSqlMapDao.class.getName() + "_lockedPipeline_" + pipelineName).intern();
    }

    public List<String> lockedPipelines() {
        return getSqlMapClientTemplate().queryForList("allLockedPipelines");
    }

    public Pipeline findPipelineByCounterOrLabel(String pipelineName, String counterOrLabel) {
        Pipeline pipeline = null;
        try {
            int pipelineCounter = Integer.parseInt(counterOrLabel);
            pipeline = findPipelineByNameAndCounter(pipelineName, pipelineCounter);
        } catch (NumberFormatException e) {
            //it maybe a label
        }

        if (pipeline == null) {
            String pipelineLabel = translatePipelineLabel(pipelineName, counterOrLabel);
            pipeline = findPipelineByNameAndLabel(pipelineName, pipelineLabel);
        }
        return pipeline;
    }

    private String translatePipelineLabel(String pipelineName, String pipelineLabel) {
        String label = pipelineLabel;
        if (pipelineLabel.equalsIgnoreCase(JobIdentifier.LATEST)) {
            label = mostRecentLabel(pipelineName);
        }
        return label;
    }

    public void pause(String pipelineName, String pauseCause, String pauseBy) {
        String cacheKey = cacheKeyForPauseState(pipelineName);
        synchronized (cacheKey) {
            Map<String, Object> args = arguments("pipelineName", pipelineName).and("pauseCause", pauseCause).and("pauseBy", pauseBy).and("paused", true).asMap();
            PipelinePauseInfo pipelinePauseInfo = (PipelinePauseInfo) getSqlMapClientTemplate().queryForObject("getPipelinePauseState", pipelineName);
            if (pipelinePauseInfo == null) {
                getSqlMapClientTemplate().insert("insertPipelinePauseState", args);
            } else {
                getSqlMapClientTemplate().update("updatePipelinePauseState", args);
            }
            goCache.remove(cacheKey);
        }
    }

    public void unpause(String pipelineName) {
        String cacheKey = cacheKeyForPauseState(pipelineName);
        synchronized (cacheKey) {
            Map<String, Object> args = arguments("pipelineName", pipelineName).and("pauseCause", null).and("pauseBy", null).and("paused", false).asMap();
            getSqlMapClientTemplate().update("updatePipelinePauseState", args);
            goCache.remove(cacheKey);
        }

    }

    public PipelinePauseInfo pauseState(String pipelineName) {
        String cacheKey = cacheKeyForPauseState(pipelineName);
        PipelinePauseInfo result = (PipelinePauseInfo) goCache.get(cacheKey);
        if (result == null) {
            synchronized (cacheKey) {
                result = (PipelinePauseInfo) goCache.get(cacheKey);
                if (result == null) {
                    result = (PipelinePauseInfo) getSqlMapClientTemplate().queryForObject("getPipelinePauseState", pipelineName);
                    result = (result == null) ? PipelinePauseInfo.NULL : result;
                    goCache.put(cacheKey, result);
                }
            }
        }
        return result;
    }

    private String cacheKeyForPauseState(String pipelineName) {
        return (PipelineSqlMapDao.class + "_cacheKeyForPauseState_" + pipelineName.toLowerCase()).intern();
    }

    String cacheKeyForlatestPassedStage(long pipelineId, String stage) {
        return (PipelineSqlMapDao.class + "_cacheKeyForlatestPassedStage_" + pipelineId + "_and_" + stage.toLowerCase()).intern();
    }

    public StageIdentifier latestPassedStageIdentifier(long pipelineId, String stage) {
        String cacheKey = cacheKeyForlatestPassedStage(pipelineId, stage);
        StageIdentifier result = (StageIdentifier) goCache.get(cacheKey);
        if (result == null) {
            synchronized (cacheKey) {
                result = (StageIdentifier) goCache.get(cacheKey);
                if (result == null) {
                    result = (StageIdentifier) getSqlMapClientTemplate().queryForObject("latestPassedStageForPipelineId", arguments("id", pipelineId).and("stage", stage).asMap());
                    result = (result == null) ? StageIdentifier.NULL : result;
                    goCache.put(cacheKey, result);
                }
            }
        }
        return result;
    }

    @Override
    public List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, PipelineIdentifier dependencyPipelineIdentifier) {
        String cacheKey = cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(pipelineName, dependencyPipelineIdentifier.getName(), dependencyPipelineIdentifier.getCounter());
        List<PipelineIdentifier> pipelineIdentifiers = (List<PipelineIdentifier>) goCache.get(cacheKey);
        if (pipelineIdentifiers == null) {
            synchronized (cacheKey) {
                pipelineIdentifiers = (List<PipelineIdentifier>) goCache.get(cacheKey);
                if (pipelineIdentifiers == null) {
                    pipelineIdentifiers = (List<PipelineIdentifier>) getSqlMapClientTemplate().queryForList("pipelineInstancesTriggeredOutOfDependencyMaterial",
                            arguments("pipelineName", pipelineName).and("dependencyPipelineName", dependencyPipelineIdentifier.getName())
                                    .and("stageLocator", dependencyPipelineIdentifier.getName() + "/" + dependencyPipelineIdentifier.getCounter() + "/%/%")
                                    .asMap());
                    goCache.put(cacheKey, pipelineIdentifiers);
                }
            }
        }
        return pipelineIdentifiers;
    }

	@Override
	public List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, MaterialInstance materialInstance, String revision) {
		String cacheKey = cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(pipelineName, materialInstance.getFingerprint(), revision);
		List<PipelineIdentifier> pipelineIdentifiers = (List<PipelineIdentifier>) goCache.get(cacheKey);
		if (pipelineIdentifiers == null) {
			synchronized (cacheKey) {
				pipelineIdentifiers = (List<PipelineIdentifier>) goCache.get(cacheKey);
				if (pipelineIdentifiers == null) {
					pipelineIdentifiers = (List<PipelineIdentifier>) getSqlMapClientTemplate().queryForList("pipelineInstancesTriggeredOffOfMaterialRevision",
							arguments("pipelineName", pipelineName).and("materialId", materialInstance.getId()).and("materialRevision", revision).asMap());
					goCache.put(cacheKey, pipelineIdentifiers);
				}
			}
		}
		return pipelineIdentifiers;
	}

    private String cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, String dependencyPipelineName, Integer dependencyPipelineCounter) {
        return (PipelineSqlMapDao.class + "_cacheKeyForPipelineInstancesWithDependencyMaterial_" + pipelineName.toLowerCase() + "_" + dependencyPipelineName.toLowerCase() + "_" + dependencyPipelineCounter).intern();
    }

	String cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, String fingerPrint, String revision) {
        return (PipelineSqlMapDao.class + "_cacheKeyForPipelineInstancesWithDependencyMaterial_" + pipelineName.toLowerCase() + "_" + fingerPrint + "_" + revision).intern();
	}

    private void invalidateCacheConditionallyForPipelineInstancesTriggeredWithDependencyMaterial(Pipeline pipeline) {
        BuildCause buildCause = pipeline.getBuildCause();
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            if (DependencyMaterial.TYPE.equals(materialRevision.getMaterial().getType())) {
                DependencyMaterialRevision dependencyMaterialRevision = (DependencyMaterialRevision) materialRevision.getRevision();
                goCache.remove(cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(pipeline.getName(),
                        dependencyMaterialRevision.getPipelineName(), dependencyMaterialRevision.getPipelineCounter()));
            } else {
				goCache.remove(cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(pipeline.getName(),
						materialRevision.getMaterial().getFingerprint(), materialRevision.getRevision().getRevision()));
			}
        }
    }

    @Override
    public void updateComment(String pipelineName, int pipelineCounter, String comment) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).and("pipelineCounter", pipelineCounter).and("comment", comment).asMap();
        getSqlMapClientTemplate().update("updatePipelineComment", args);

        Pipeline pipeline = findPipelineByNameAndCounter(pipelineName, pipelineCounter);
        goCache.remove(pipelineHistoryCacheKey(pipeline.getId()));
    }
}
