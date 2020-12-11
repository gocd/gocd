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
package com.thoughtworks.go.server.dao;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.cache.LazyCache;
import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.ClonerFactory;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class PipelineSqlMapDao extends SqlMapClientDaoSupport implements Initializer, PipelineDao, StageStatusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineSqlMapDao.class);
    private static final Marker FATAL = MarkerFactory.getMarker("FATAL");
    private final LazyCache pipelineByBuildIdCache;
    private final CacheKeyGenerator cacheKeyGenerator;
    private StageDao stageDao;
    private MaterialRepository materialRepository;
    private EnvironmentVariableDao environmentVariableDao;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private final GoConfigDao configFileDao;
    private final Cloner cloner = ClonerFactory.instance();
    private Clock timeProvider;
    private final ReadWriteLock activePipelineRWLock = new ReentrantReadWriteLock();
    private final Lock activePipelineReadLock = activePipelineRWLock.readLock();
    private final Lock activePipelineWriteLock = activePipelineRWLock.writeLock();

    @Autowired
    public PipelineSqlMapDao(StageDao stageDao,
                             MaterialRepository materialRepository,
                             GoCache goCache,
                             EnvironmentVariableDao environmentVariableDao,
                             TransactionTemplate transactionTemplate,
                             SqlSessionFactory sqlSessionFactory,
                             TransactionSynchronizationManager transactionSynchronizationManager,
                             SystemEnvironment systemEnvironment,
                             GoConfigDao configFileDao,
                             Database database,
                             TimeProvider timeProvider) {
        super(goCache, sqlSessionFactory, systemEnvironment, database);
        this.stageDao = stageDao;
        this.materialRepository = materialRepository;
        this.environmentVariableDao = environmentVariableDao;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.configFileDao = configFileDao;
        this.timeProvider = timeProvider;
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
        this.pipelineByBuildIdCache = new LazyCache(createCacheIfRequired(PipelineSqlMapDao.class.getName()), transactionSynchronizationManager);
    }

    private static Ehcache createCacheIfRequired(String cacheName) {
        final CacheManager instance = CacheManager.newInstance(new Configuration().name(cacheName));
        synchronized (instance) {
            if (!instance.cacheExists(cacheName)) {
                instance.addCache(new Cache(cacheConfiguration(cacheName)));
            }
            return instance.getCache(cacheName);
        }
    }

    private static CacheConfiguration cacheConfiguration(String cacheName) {
        return new CacheConfiguration(cacheName, 1024)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
                .overflowToDisk(false)
                .diskPersistent(false);
    }


    @Override
    public void initialize() {
        try {
            LOGGER.info("Loading active pipelines into memory.");
            cacheActivePipelines();
            LOGGER.info("Done loading active pipelines into memory.");
        } catch (Exception e) {
            LOGGER.error(FATAL, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startDaemon() {

    }

    @Override
    public Pipeline save(final Pipeline pipeline) {
        return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        goCache.remove(cacheKeyForLatestPipelineIdByPipelineName(pipeline.getName()));
                        invalidateCacheConditionallyForPipelineInstancesTriggeredWithDependencyMaterial(pipeline);
                    }
                });

                pipelineByBuildIdCache.flushOnCommit();
                getSqlMapClientTemplate().insert("insertPipeline", pipeline);
                savePipelineMaterialRevisions(pipeline, pipeline.getId());
                environmentVariableDao.save(pipeline.getId(), EnvironmentVariableType.Trigger, pipeline.scheduleTimeVariables());
                return pipeline;
            }
        });
    }


    @Override
    public Integer getCounterForPipeline(String name) {
        Integer counter = (Integer) getSqlMapClientTemplate().queryForObject("getCounterForPipeline", name);
        return counter == null ? 0 : counter;
    }

    public List<String> getPipelineNamesWithMultipleEntriesForLabelCount() {
        List<String> pipelinenames = getSqlMapClientTemplate().queryForList("getPipelineNamesWithMultipleEntriesForLabelCount");
        if (pipelinenames.size() > 0 && StringUtils.isBlank(pipelinenames.get(0)))
            return new ArrayList<>();
        return pipelinenames;
    }

    public void deleteOldPipelineLabelCountForPipelineInConfig(String pipelineName) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).asMap();
        getSqlMapClientTemplate().delete("deleteOldPipelineLabelCountForPipelineInConfig", args);
    }

    public void deleteOldPipelineLabelCountForPipelineCurrentlyNotInConfig(String pipelineName) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).asMap();
        getSqlMapClientTemplate().delete("deleteOldPipelineLabelCountForPipelineCurrentlyNotInConfig", args);
    }

    @Override
    public void insertOrUpdatePipelineCounter(Pipeline pipeline, Integer lastCount, Integer newCount) {
        Map<String, Object> args = arguments("pipelineName", pipeline.getName()).and("count", newCount).asMap();
        Integer hasPipelineRow = (Integer) getSqlMapClientTemplate().queryForObject("hasPipelineInfoRow", pipeline.getName());
        transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                pipelineByBuildIdCache.flushOnCommit();
                if (hasPipelineRow == 0) {
                    getSqlMapClientTemplate().insert("insertPipelineLabelCounter", args);
                } else if (newCount > lastCount) {
                    // Counter will not be updated when using other types of labels such as Date or Revision.
                    getSqlMapClientTemplate().update("updatePipelineLabelCounter", args, 1);
                }

                return null;
            }
        });
    }

    @Override
    public Pipeline findPipelineByNameAndCounter(String name, int counter) {
        Map<String, Object> map = arguments("name", name).and("counter", counter).asMap();
        return (Pipeline) getSqlMapClientTemplate().queryForObject("findPipelineByNameAndCounter", map);
    }

    @Override
    public BuildCause findBuildCauseOfPipelineByNameAndCounter(String name, int counter) {
        String cacheKey = cacheKeyForBuildCauseByNameAndCounter(name, counter);
        BuildCause buildCause = (BuildCause) goCache.get(cacheKey);
        if (buildCause == null) {
            synchronized (cacheKey) {
                buildCause = (BuildCause) goCache.get(cacheKey);
                if (buildCause == null) {
                    Pipeline pipeline = findPipelineByNameAndCounter(name, counter);
                    if (pipeline == null) {
                        throw new RecordNotFoundException(String.format("Pipeline %s with counter %d was not found", name, counter));
                    }
                    loadMaterialRevisions(pipeline);
                    buildCause = pipeline.getBuildCause();
                    goCache.put(cacheKey, buildCause);
                }
            }
        }
        return buildCause;
    }

    String cacheKeyForBuildCauseByNameAndCounter(String name, int counter) {
        return cacheKeyGenerator.generate("buildCauseByNameAndCounter", name.toLowerCase(), counter);
    }

    @Override
    public Pipeline findPipelineByNameAndLabel(String name, String label) {
        Map<String, Object> map = arguments("name", name).and("label", label).asMap();
        return (Pipeline) getSqlMapClientTemplate().queryForObject("findPipelineByNameAndLabel", map);
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

    String latestSuccessfulStageCacheKey(String pipelineName, String stageName) {
        return cacheKeyGenerator.generate("latestSuccessfulStage", pipelineName, stageName);
    }

    private void savePipelineMaterialRevisions(Pipeline pipeline, final Long pipelineId) {
        MaterialRevisions materialRevisions = pipeline.getBuildCause().getMaterialRevisions();
        materialRepository.createPipelineMaterialRevisions(pipeline, pipelineId, materialRevisions);
    }

    @Override
    public Pipeline loadPipeline(long pipelineId) {
        Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("pipelineById", pipelineId);
        return loadAssociations(pipeline, pipeline == null ? "" : pipeline.getName());
    }

    @Override
    public Pipeline mostRecentPipeline(String pipelineName) {
        Pipeline mostRecent = (Pipeline) getSqlMapClientTemplate().queryForObject("mostRecentPipeline", pipelineName);
        return loadAssociations(mostRecent, pipelineName);
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
    @Override
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

    @Override
    public Pipeline pipelineWithMaterialsAndModsByBuildId(long buildId) {
        String cacheKey = this.cacheKeyGenerator.generate("getPipelineByBuildId", buildId);
        return pipelineByBuildIdCache.get(cacheKey, new Supplier<Pipeline>() {
            @Override
            public Pipeline get() {
                Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("getPipelineByBuildId", buildId);
                if (pipeline == null) {
                    throw new DataRetrievalFailureException("Could not load pipeline from build with id " + buildId);
                }
                return loadMaterialRevisions(pipeline);
            }
        });
    }

    @Override
    public PipelineIdentifier mostRecentPipelineIdentifier(String pipelineName) {
        return (PipelineIdentifier) getSqlMapClientTemplate().queryForObject("mostRecentPipelineIdentifier", pipelineName);
    }

    @Override
    public Pipeline pipelineByIdWithMods(long pipelineId) {
        Pipeline pipeline = (Pipeline) getSqlMapClientTemplate().queryForObject("pipelineById", pipelineId);
        if (pipeline == null) {
            throw new DataRetrievalFailureException("Could not load pipeline with id " + pipelineId);
        }
        return loadMaterialRevisions(pipeline);
    }

    @Override
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
        pipeline.getBuildCause().setVariables(environmentVariableDao.load(pipeline.getId(), EnvironmentVariableType.Trigger));
        return pipeline;
    }

    @Override
    public PipelineInstanceModels loadHistory(String pipelineName) {
        return PipelineInstanceModels.createPipelineInstanceModels(
                (List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("getAllPipelineHistoryByName", arguments("name", pipelineName).asMap()));
    }

    @Override
    public PipelineInstanceModel findPipelineHistoryByNameAndCounter(String pipelineName, int pipelineCounter) {
        PipelineInstanceModel instanceModel = loadPipelineInstanceModelByNameAndCounter(pipelineName, pipelineCounter);
        loadPipelineHistoryBuildCause(instanceModel);
        return instanceModel;
    }

    private PipelineInstanceModel loadPipelineInstanceModelByNameAndCounter(String pipelineName, int pipelineCounter) {
        String cacheKey = cacheKeyForPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter);
        PipelineInstanceModel instanceModel = (PipelineInstanceModel) goCache.get(cacheKey);
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

    String cacheKeyForPipelineHistoryByNameAndCounter(String pipelineName, int pipelineCounter) {
        return cacheKeyGenerator.generate("cacheKeyForPipelineHistoryByName", pipelineName.toLowerCase(), "AndCounter", pipelineCounter);
    }

    @Override
    public Pipeline findEarlierPipelineThatPassedForStage(String pipelineName, String stageName, double naturalOrder) {
        return (Pipeline) getSqlMapClientTemplate().queryForObject("findEarlierPipelineThatPassedForStage",
                arguments("pipelineName", pipelineName).and("stageName", stageName).and("naturalOrder", naturalOrder).asMap());
    }

    private PipelineInstanceModels dependentPipelines(PipelineInstanceModel upstreamPipeline,
                                                      List<PipelineInstanceModel> instanceModels) {
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
                Map<CaseInsensitiveString, TreeSet<Long>> result = groupPipelineInstanceIdsByPipelineName(pipelines);
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

    @Override
    public PipelineInstanceModels loadActivePipelines() {
        return convertToPipelineInstanceModels(getAllActivePipelineNamesVsTheirInstanceIDs());
    }

    @Override
    public PipelineInstanceModels loadActivePipelineInstancesFor(CaseInsensitiveString pipelineName) {
        Map<CaseInsensitiveString, TreeSet<Long>> allActivePipelineNamesVsTheirInstanceIDs = getAllActivePipelineNamesVsTheirInstanceIDs();
        Map<CaseInsensitiveString, TreeSet<Long>> similarMapForSinglePipeline = new HashMap<>();

        if (allActivePipelineNamesVsTheirInstanceIDs.containsKey(pipelineName)) {
            similarMapForSinglePipeline.put(pipelineName, allActivePipelineNamesVsTheirInstanceIDs.get(pipelineName));
        }

        return convertToPipelineInstanceModels(similarMapForSinglePipeline);
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

    private PipelineInstanceModels convertToPipelineInstanceModels(Map<CaseInsensitiveString, TreeSet<Long>> result) {
        List<PipelineInstanceModel> models = new ArrayList<PipelineInstanceModel>();

        List<CaseInsensitiveString> pipelinesInConfig = getPipelineNamesInConfig();
        if (pipelinesInConfig.isEmpty()) {
            LOGGER.warn("No pipelines found in Config, Skipping PIM loading.");
            return PipelineInstanceModels.createPipelineInstanceModels(models);
        }

        List<Long> pipelineIds = loadIdsFromHistory(result);
        int collectionCount = pipelineIds.size();
        for (int i = 0; i < collectionCount; i++) {
            Long id = pipelineIds.get(i);
            PipelineInstanceModel model = loadHistory(id);
            if (model == null) {
                continue;
            }
            if (!pipelinesInConfig.contains(new CaseInsensitiveString(model.getName()))) {
                LOGGER.debug("Skipping PIM for pipeline {} ,since its not found in current config", model.getName());
                continue;
            }
            models.add(model);
            loadPipelineHistoryBuildCause(model);

        }
        return PipelineInstanceModels.createPipelineInstanceModels(models);
    }

    private List<Long> loadIdsFromHistory(Map<CaseInsensitiveString, TreeSet<Long>> result) {
        List<Long> idsForHistory = new ArrayList<Long>();
        try {
            activePipelineReadLock.lock();
            for (Map.Entry<CaseInsensitiveString, TreeSet<Long>> pipelineToIds : result.entrySet()) {
                idsForHistory.addAll(pipelineToIds.getValue().descendingSet());
            }
        } finally {
            activePipelineReadLock.unlock();
        }
        return idsForHistory;
    }

    @Override
    public PipelineInstanceModel loadHistoryByIdWithBuildCause(Long id) {
        PipelineInstanceModel model = loadHistory(id);
        loadPipelineHistoryBuildCause(model);
        return model;
    }

    private Map<CaseInsensitiveString, TreeSet<Long>> groupPipelineInstanceIdsByPipelineName(List<PipelineInstanceModel> pipelines) {
        Map<CaseInsensitiveString, TreeSet<Long>> result = new HashMap<CaseInsensitiveString, TreeSet<Long>>();
        for (PipelineInstanceModel pipeline : pipelines) {
            TreeSet<Long> ids = initializePipelineInstances(result, new CaseInsensitiveString(pipeline.getName()));
            ids.add(pipeline.getId());
        }
        return result;
    }

    String activePipelinesCacheKey() {
        return cacheKeyGenerator.generate("activePipelines");
    }

    @Override
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

    @Override
    public void stageStatusChanged(Stage stage) {
        removeStageSpecificCache(stage);
        syncCachedActivePipelines(stage);
        updateCachedLatestSuccessfulStage(stage);
        String pipelineName = stage.getIdentifier().getPipelineName();
        Integer pipelineCounter = stage.getIdentifier().getPipelineCounter();
        clearPipelineHistoryCacheViaNameAndCounter(pipelineName, pipelineCounter);
    }

    private void clearPipelineHistoryCacheViaNameAndCounter(String pipelineName, Integer pipelineCounter) {
        goCache.remove(cacheKeyForPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter));
    }


    private void syncCachedActivePipelines(Stage stage) {
        Map<CaseInsensitiveString, TreeSet<Long>> activePipelinesToIds = (Map<CaseInsensitiveString, TreeSet<Long>>) goCache.get(activePipelinesCacheKey());
        if (activePipelinesToIds == null) {
            return;
        }
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(loadHistory(stage.getPipelineId()).getName());
        try {
            activePipelineWriteLock.lock();
            addActiveAsLatest(stage, activePipelinesToIds, pipelineName);
            removeCompletedIfNotLatest(stage, activePipelinesToIds, pipelineName);
        } finally {
            activePipelineWriteLock.unlock();
        }
    }

    private void addActiveAsLatest(Stage stage,
                                   Map<CaseInsensitiveString, TreeSet<Long>> activePipelinesToIds,
                                   CaseInsensitiveString pipelineName) {
        if (stage.getState().isActive()) {
            TreeSet<Long> ids = initializePipelineInstances(activePipelinesToIds, pipelineName);
            removeCurrentLatestIfNoLongerActive(stage, ids);
            ids.add(stage.getPipelineId());
        }
    }

    private void removeCompletedIfNotLatest(Stage stage,
                                            Map<CaseInsensitiveString, TreeSet<Long>> activePipelinesToIds,
                                            CaseInsensitiveString pipelineName) {
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

    private TreeSet<Long> initializePipelineInstances(Map<CaseInsensitiveString, TreeSet<Long>> pipelineToIds,
                                                      CaseInsensitiveString pipelineName) {
        if (!pipelineToIds.containsKey(pipelineName)) {
            pipelineToIds.put(pipelineName, new TreeSet<Long>());
        }
        return pipelineToIds.get(pipelineName);
    }

    private void removeStageSpecificCache(Stage stage) {
        goCache.remove(pipelineHistoryCacheKey(stage.getPipelineId()));
        goCache.remove(cacheKeyForLatestPassedStage(stage.getPipelineId(), stage.getName()));
    }

    String pipelineHistoryCacheKey(Long id) {
        return cacheKeyGenerator.generate("pipelineHistory", id);
    }

    @Override
    public PipelineInstanceModels loadHistory(String pipelineName, int limit, int offset) {
        List<Long> ids = findPipelineIds(pipelineName, limit, offset);
        if (ids.size() == 1) {
            return PipelineInstanceModels.createPipelineInstanceModels(loadHistoryByIdWithBuildCause(ids.get(0)));
        }
        return loadHistory(pipelineName, ids);
    }

    @Override
    public PipelineInstanceModels loadHistory(String pipelineName, FeedModifier modifier, long cursor, Integer pageSize) {
        List<Long> ids = findPipelineIds(pipelineName, modifier, cursor, pageSize);
        if (ids.size() == 1) {
            return PipelineInstanceModels.createPipelineInstanceModels(loadHistoryByIdWithBuildCause(ids.get(0)));
        }
        return loadHistory(pipelineName, ids);
    }

    private List<Long> findPipelineIds(String pipelineName, FeedModifier modifier, long cursor, int pageSize) {
        Map<String, Object> params =
                arguments("pipelineName", pipelineName)
                        .and("cursor", cursor)
                        .and("limit", pageSize).asMap();
        return (List<Long>) getSqlMapClientTemplate().queryForList("getPipelineIds" + modifier.suffix(), params);
    }

    @Override
    public PipelineRunIdInfo getOldestAndLatestPipelineId(String pipelineName) {
        Map<String, Object> params = arguments("pipelineName", pipelineName).asMap();
        return (PipelineRunIdInfo) getSqlMapClientTemplate().queryForObject("getOldestAndLatestPipelineRun", params);
    }

    @Override
    public int getPageNumberForCounter(String pipelineName, int pipelineCounter, int limit) {
        Integer maxCounter = getCounterForPipeline(pipelineName);
        Pagination pagination = Pagination.pageStartingAt((maxCounter - pipelineCounter), maxCounter, limit);
        return pagination.getCurrentPage();
    }

    @Override
    public PipelineInstanceModels findMatchingPipelineInstances(String pipelineName, String pattern, int limit) {
        Map<String, Object> args = arguments("pipelineName", pipelineName).
                and("pattern", "%" + pattern.toLowerCase() + "%").
                and("rawPattern", pattern.toLowerCase()).
                and("limit", limit).asMap();
        long begin = System.currentTimeMillis();
        List<PipelineInstanceModel> matchingPIMs = (List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("findMatchingPipelineInstances", args);
        List<PipelineInstanceModel> exactMatchingPims = (List<PipelineInstanceModel>) getSqlMapClientTemplate().queryForList("findExactMatchingPipelineInstances", args);
        LOGGER.debug("[Compare Pipelines] Query initiated for pipeline {} with pattern {}. Query execution took {} milliseconds", pipelineName, pattern, System.currentTimeMillis() - begin);
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

    String cacheKeyForLatestPipelineIdByPipelineName(String pipelineName) {
        return cacheKeyGenerator.generate("latestPipelineIdByPipelineName", pipelineName.toLowerCase());
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

    @Override
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
            pipeline.getBuildCause().setApprover(pipeline.getApprovedBy());
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

    public void pause(String pipelineName, String pauseCause, String pauseBy) {
        String cacheKey = cacheKeyForPauseState(pipelineName);
        synchronized (cacheKey) {
            Map<String, Object> args = arguments("pipelineName", pipelineName).and("pauseCause", pauseCause).and("pauseBy", pauseBy).and("paused", true).and("pausedAt", timeProvider.currentTime()).asMap();
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
            Map<String, Object> args = arguments("pipelineName", pipelineName).and("pauseCause", null).and("pauseBy", null).and("paused", false).and("pausedAt", null).asMap();
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

    String cacheKeyForPauseState(String pipelineName) {
        return cacheKeyGenerator.generate("cacheKeyForPauseState", pipelineName.toLowerCase());
    }

    String cacheKeyForLatestPassedStage(long pipelineId, String stage) {
        return cacheKeyGenerator.generate("cacheKeyForlatestPassedStage", pipelineId, stage.toLowerCase());
    }

    @Override
    public StageIdentifier latestPassedStageIdentifier(long pipelineId, String stage) {
        String cacheKey = cacheKeyForLatestPassedStage(pipelineId, stage);
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
    public List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName,
                                                                                        PipelineIdentifier dependencyPipelineIdentifier) {
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
    public List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName,
                                                                                        MaterialInstance materialInstance,
                                                                                        String revision) {
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

    @Override
    public PipelineInstanceModels loadHistoryForDashboard(List<String> pipelineNames) {
        if (pipelineNames == null || pipelineNames.isEmpty()) {
            return PipelineInstanceModels.createPipelineInstanceModels();
        }

        // this is done to pick up an SQL query optimized for a single pipeline (no IN clause)
        if (pipelineNames.size() == 1) {
            return loadHistoryForDashboard(pipelineNames.get(0));
        }

        Map<String, Object> args = arguments("pipelineNames", pipelineNames).asMap();
        List<PipelineInstanceModel> resultSet = getSqlMapClientTemplate().queryForList("getPipelinesForDashboard", args);
        return PipelineInstanceModels.createPipelineInstanceModels(resultSet);
    }


    private PipelineInstanceModels loadHistoryForDashboard(String pipelineName) {
        if (isBlank(pipelineName)) {
            return PipelineInstanceModels.createPipelineInstanceModels();
        }

        Map<String, Object> args = arguments("pipelineName", pipelineName).asMap();
        List<PipelineInstanceModel> resultSet = getSqlMapClientTemplate().queryForList("getPipelineForDashboard", args);
        return PipelineInstanceModels.createPipelineInstanceModels(resultSet);
    }

    String cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName,
                                                                       String dependencyPipelineName,
                                                                       Integer dependencyPipelineCounter) {
        return cacheKeyGenerator.generate("cacheKeyForPipelineInstancesWithDependencyMaterial", pipelineName.toLowerCase(), dependencyPipelineName.toLowerCase(), dependencyPipelineCounter);
    }

    String cacheKeyForPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName,
                                                                       String fingerPrint,
                                                                       String revision) {
        return cacheKeyGenerator.generate("cacheKeyForPipelineInstancesWithDependencyMaterial", pipelineName.toLowerCase(), fingerPrint, revision);
    }

    private void invalidateCacheConditionallyForPipelineInstancesTriggeredWithDependencyMaterial(Pipeline pipeline) {
        BuildCause buildCause = pipeline.getBuildCause();
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            if (DependencyMaterial.TYPE.equals(materialRevision.getMaterial().getTypeName())) {
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

    private Map<CaseInsensitiveString, TreeSet<Long>> getAllActivePipelineNamesVsTheirInstanceIDs() {
        String cacheKey = activePipelinesCacheKey();
        Map<CaseInsensitiveString, TreeSet<Long>> result = (Map<CaseInsensitiveString, TreeSet<Long>>) goCache.get(cacheKey);
        if (result == null) {
            synchronized (cacheKey) {
                result = (Map<CaseInsensitiveString, TreeSet<Long>>) goCache.get(cacheKey);
                if (result == null) {
                    List<PipelineInstanceModel> pipelines = getAllPIMs();
                    result = groupPipelineInstanceIdsByPipelineName(pipelines);
                    goCache.put(cacheKey, result);
                }
            }
        }
        return result;
    }
}
