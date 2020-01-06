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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.StageStatusMessage;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.ModificationForPipeline;
import com.thoughtworks.go.server.ui.StageSummaryModel;
import com.thoughtworks.go.server.ui.StageSummaryModels;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.server.service.ServiceConstants.History.BAD_CURSOR_MSG;
import static java.lang.String.format;

@Service
public class StageService implements StageFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(StageService.class);
    private static final int FEED_PAGE_SIZE = 25;
    private final CacheKeyGenerator cacheKeyGenerator;

    private StageDao stageDao;
    private JobInstanceService jobInstanceService;
    private SecurityService securityService;
    private PipelineDao pipelineDao;
    private final ChangesetService changesetService;
    private final GoConfigService goConfigService;
    private TransactionTemplate transactionTemplate;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private List<StageStatusListener> stageStatusListeners;
    private StageStatusTopic stageStatusTopic;
    private StageStatusCache stageStatusCache;
    private Cloner cloner = new Cloner();
    private GoCache goCache;
    private static final String NOT_AUTHORIZED_TO_VIEW_PIPELINE = "Not authorized to view pipeline";

    @Autowired
    public StageService(StageDao stageDao,
                        JobInstanceService jobInstanceService,
                        StageStatusTopic stageStatusTopic,
                        StageStatusCache stageStatusCache,
                        SecurityService securityService,
                        PipelineDao pipelineDao,
                        ChangesetService changesetService,
                        GoConfigService goConfigService,
                        TransactionTemplate transactionTemplate,
                        TransactionSynchronizationManager transactionSynchronizationManager,
                        GoCache goCache,
                        StageStatusListener... stageStatusListeners) {
        this.stageDao = stageDao;
        this.jobInstanceService = jobInstanceService;
        this.stageStatusTopic = stageStatusTopic;
        this.stageStatusCache = stageStatusCache;
        this.securityService = securityService;
        this.pipelineDao = pipelineDao;
        this.changesetService = changesetService;
        this.goConfigService = goConfigService;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.goCache = goCache;
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
        this.stageStatusListeners = new ArrayList<>(Arrays.asList(stageStatusListeners));
    }

    public void addStageStatusListener(StageStatusListener listener) {
        stageStatusListeners.add(listener);
    }

    public Stage getStageByBuild(JobInstance jobInstance) {
        return getStageByBuild(jobInstance.getId());
    }

    public Stage getStageByBuild(long buildId) {
        return stageDao.getStageByBuild(buildId);
    }

    public Stage stageById(long id) {
        return stageDao.stageById(id);
    }

    public Stage findStageWithIdentifier(String pipelineName,
                                         int pipelineCounter,
                                         String stageName,
                                         String stageCounter,
                                         String username,
                                         OperationResult result) {
        if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            String message = String.format("Pipeline '%s' not found", pipelineName);
            result.notFound("Not Found", message, HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }

        if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
            result.forbidden("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return null;
        }

        Pipeline pipeline = pipelineDao.findPipelineByNameAndCounter(pipelineName, pipelineCounter);
        if (pipeline == null) {
            String message = String.format("Pipeline '%s' with counter '%s' not found", pipelineName, pipelineCounter);
            result.notFound("Not Found", message, HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }

        return findStageWithIdentifier(new StageIdentifier(pipelineName, pipelineCounter, stageName, stageCounter));
    }

    public Stage findStageWithIdentifier(String pipelineName,
                                         Integer pipelineCounter,
                                         String stageName,
                                         String stageCounter,
                                         Username username) {
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            throw new RecordNotFoundException(EntityType.Pipeline, pipelineName);
        }

        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            throw new NotAuthorizedException(NOT_AUTHORIZED_TO_VIEW_PIPELINE);
        }

        Pipeline pipeline = pipelineDao.findPipelineByNameAndCounter(pipelineName, pipelineCounter);
        if (pipeline == null) {
            String message = String.format("Pipeline '%s' with counter '%s' not found!", pipelineName, pipelineCounter);
            throw new RecordNotFoundException(message);
        }

        return findStageWithIdentifier(new StageIdentifier(pipelineName, pipelineCounter, stageName, stageCounter));
    }

    @Override
    public Stage findStageWithIdentifier(StageIdentifier identifier) {
        return stageDao.findStageWithIdentifier(identifier);
    }

    public StageSummaryModel findStageSummaryByIdentifier(StageIdentifier stageId,
                                                          Username username,
                                                          LocalizedOperationResult result) {
        if (!securityService.hasViewPermissionForPipeline(username, stageId.getPipelineName())) {
            result.forbidden(LocalizedMessage.forbiddenToViewPipeline(stageId.getPipelineName()), HealthStateType.general(HealthStateScope.forPipeline(stageId.getPipelineName())));
            return null;
        }
        Stages stages = stageDao.getAllRunsOfStageForPipelineInstance(stageId.getPipelineName(), stageId.getPipelineCounter(), stageId.getStageName());
        for (Stage stage : stages) {
            if (stage.getIdentifier().getStageCounter().equals(stageId.getStageCounter())) {
                StageSummaryModel summaryModel = new StageSummaryModel(stage, stages, stageDao, null);
                return summaryModel;
            }
        }
        result.notFound("Stage '" + stageId + "' not found.", HealthStateType.general(HealthStateScope.GLOBAL));
        return null;
    }

    public synchronized void cancelStage(final Stage stage, String username) {
        cancel(stage, username);
        notifyStageStatusChangeListeners(stage);
        //Send a notification only if none of the jobs are assigned.
        // If any of the jobs are assigned. JobStatusListener.onMessage will send the stage cancel notification.
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                JobInstances jobInstances = stage.getJobInstances();
                boolean noJobIsAssigned = jobInstances.stream().noneMatch(JobInstance::isAssignedToAgent);
                if (noJobIsAssigned) {
                    stageStatusTopic.post(new StageStatusMessage(stage.getIdentifier(), stage.stageState(), stage.getResult(), SessionUtils.currentUsername()));
                }
            }
        });
    }

    private void cancel(final Stage stage, String username) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (JobInstance job : stage.getJobInstances()) {
                    jobInstanceService.cancelJob(job);
                }
                updateStageWithoutNotifications(stage, username);
            }
        });
    }

    public DurationBean getBuildDuration(String pipelineName, String stageName, JobInstance job) {
        return getDuration(pipelineName, stageName, job);
    }

    private DurationBean getDuration(String pipelineName, String stageName, JobInstance job) {
        if (job.isCompleted()) {
            // Calculating duration is an expensive query; only do so when the stage is building.
            return new DurationBean(job.getId(), 0L);
        }

        Long duration = stageDao.getDurationOfLastSuccessfulOnAgent(pipelineName, stageName, job);
        return new DurationBean(job.getId(), duration == null ? 0L : duration);
    }

    public Stage mostRecentPassed(String pipelineName, String stageName) {
        return stageDao.mostRecentPassed(pipelineName, stageName);
    }

    public int getCount(String pipelineName, String stageName) {
        return stageDao.getCount(pipelineName, stageName);
    }

    public Stage save(final Pipeline pipeline, final Stage stage) {
        return (Stage) transactionTemplate.execute((TransactionCallback) status -> {
            stage.building();
            final Stage savedStage = persistStage(pipeline, stage);
            persistJobs(savedStage);
            notifyStageStatusChangeListeners(savedStage);
            return savedStage;
        });
    }

    private void notifyStageStatusChangeListeners(final Stage savedStage) {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                StageStatusListener[] prototype = new StageStatusListener[0];
                for (StageStatusListener stageStatusListener : stageStatusListeners.toArray(prototype)) {
                    try {
                        stageStatusListener.stageStatusChanged(savedStage);
                    } catch (Throwable e) {
                        LOGGER.error("error notifying listener for stage {}", savedStage, e);
                    }
                }
            }
        });
    }

    // Because current stage order is defined by existing order(for rerun) or Max(order)+1 (for the first run), so we
    // need to synchronize this method call to make sure no concurrent issues.
    private synchronized Stage persistStage(Pipeline pipeline, Stage stage) {
        long pipelineId = pipeline.getId();
        stage.setOrderId(resolveStageOrder(pipelineId, stage.getName()));
        Stage savedStage = stageDao.save(pipeline, stage);

        savedStage.setIdentifier(new StageIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter())));
        for (JobInstance jobInstance : savedStage.getJobInstances()) {
            jobInstance.setIdentifier(new JobIdentifier(pipeline, savedStage, jobInstance));
        }
        return savedStage;
    }

    private void persistJobs(Stage stage) {
        for (JobInstance job : stage.getJobInstances()) {
            jobInstanceService.save(stage.getIdentifier(), stage.getId(), job);
        }
    }

    //stage order definition: 1) if stage has been scheduled, copy existing order 2) if not, increase the max existing
    // stage order in current pipeline by 1, as current stage's order
    private Integer resolveStageOrder(long pipelineId, String stageName) {
        Integer order = getStageOrderInPipeline(pipelineId, stageName);
        if (order == null) {
            order = getMaxStageOrderInPipeline(pipelineId) + 1;
        }
        return order;
    }

    private Integer getStageOrderInPipeline(long pipelineId, String stageName) {
        return stageDao.getStageOrderInPipeline(pipelineId, stageName);
    }

    private int getMaxStageOrderInPipeline(long pipelineId) {
        return stageDao.getMaxStageOrder(pipelineId);
    }

    public void updateResult(final Stage stage) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                updateStageWithoutNotifications(stage, null);
                notifyStageStatusChangeListeners(stage);
            }
        });
    }

    private void updateStageWithoutNotifications(final Stage stage, String username) {
        stage.calculateResult();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        clearCachedCompletedStageFeeds(stage.getIdentifier().getPipelineName());
                    }
                });
                stageDao.updateResult(stage, stage.getResult(), username);
            }
        });
    }

    public Stage findLatestStage(String pipelineName, String stageName) {
        return stageStatusCache.currentStage(new StageConfigIdentifier(pipelineName, stageName));
    }

    public FeedEntries feed(String pipelineName, Username username) {
        String key = cacheKeyForLatestStageFeedForPipeline(pipelineName);
        List<StageFeedEntry> feedEntries = (List<StageFeedEntry>) goCache.get(key);
        if (feedEntries == null) {
            synchronized (key) {
                feedEntries = (List<StageFeedEntry>) goCache.get(key);//Double check locking is done because the query is expensive (takes about 2 seconds)
                if (feedEntries == null) {
                    feedEntries = stageDao.findCompletedStagesFor(pipelineName, FeedModifier.Latest, -1, FEED_PAGE_SIZE);
                    populateAuthors(feedEntries, pipelineName, username);
                    goCache.put(key, feedEntries);
                }
            }
        }
        return cloner.deepClone(new FeedEntries(new ArrayList<>(feedEntries)));
    }

    private String cacheKeyForLatestStageFeedForPipeline(String pipelineName) {
        return cacheKeyGenerator.generate("latestStageFeedForPipeline", pipelineName);
    }

    private void clearCachedCompletedStageFeeds(String pipelineName) {
        String key = cacheKeyForLatestStageFeedForPipeline(pipelineName);
        synchronized (key) {
            goCache.remove(key);
        }
    }

    public FeedEntries feedBefore(long entryId, String pipelineName, Username username) {
        List<StageFeedEntry> stageEntries = stageDao.findCompletedStagesFor(pipelineName, FeedModifier.Before, entryId, FEED_PAGE_SIZE);
        populateAuthors(stageEntries, pipelineName, username);
        return new FeedEntries(new ArrayList<>(stageEntries));
    }

    private void populateAuthors(List<StageFeedEntry> stageEntries,
                                 String pipelineName,
                                 Username username) {
        List<Long> pipelineIds = new ArrayList<>();
        for (StageFeedEntry stageEntry : stageEntries) {
            pipelineIds.add(stageEntry.getPipelineId());
        }
        CruiseConfig config = goConfigService.currentCruiseConfig();
        Map<Long, List<ModificationForPipeline>> revisionsPerPipeline = changesetService.modificationsOfPipelines(pipelineIds, pipelineName, username);
        for (StageFeedEntry stageEntry : stageEntries) {
            List<ModificationForPipeline> revs = revisionsPerPipeline.get(stageEntry.getPipelineId());
            for (ModificationForPipeline rev : revs) {
                Author author = rev.getAuthor();
                if (author != null) {
                    stageEntry.addAuthor(author);
                }

                String pipelineForRev = rev.getPipelineId().getPipelineName();
                if (!config.hasPipelineNamed(new CaseInsensitiveString(pipelineForRev))) {
                    LOGGER.debug("pipeline not found: {}", pipelineForRev);
                }
            }
        }
    }

    public StageSummaryModels findStageHistoryForChart(String pipelineName,
                                                       String stageName,
                                                       int pageNumber,
                                                       int pageSize,
                                                       Username username) {
        int total = stageDao.getTotalStageCountForChart(pipelineName, stageName);

        Pagination pagination = Pagination.pageByNumber(pageNumber, total, pageSize);

        List<Stage> stages = stageDao.findStageHistoryForChart(pipelineName, stageName, pageSize, pagination.getOffset());

        StageSummaryModels stageSummaryModels = new StageSummaryModels();
        for (Stage forStage : stages) {
            StageSummaryModel stageSummaryByIdentifier = new StageSummaryModel(forStage, null, stageDao, forStage.getIdentifier());
            if (!stageSummaryByIdentifier.getStage().getState().completed()) {
                continue;
            }
            stageSummaryModels.add(stageSummaryByIdentifier);
        }
        stageSummaryModels.setPagination(pagination);
        return stageSummaryModels;
    }

    public StageHistoryPage findStageHistoryPage(Stage stage, int pageSize) {
        return stageDao.findStageHistoryPage(stage, pageSize);
    }

    public StageHistoryPage findStageHistoryPageByNumber(String pipelineName,
                                                         String stageName,
                                                         int pageNumber,
                                                         int pageSize) {
        return stageDao.findStageHistoryPageByNumber(pipelineName, stageName, pageNumber, pageSize);
    }

    public StageInstanceModels findDetailedStageHistoryByOffset(String pipelineName,
                                                                String stageName,
                                                                Pagination pagination,
                                                                String username,
                                                                OperationResult result) {
        if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound("Not Found", "Pipeline not found", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }
        if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
            result.forbidden("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return null;
        }

        return stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
    }

    public StageInstanceModels findStageHistoryViaCursor(Username username, String pipelineName, String stageName, long afterCursor, long beforeCursor, Integer pageSize) {
        checkForExistenceAndAccess(username, pipelineName);
        StageInstanceModels stageInstanceModels;
        if (validateCursor(afterCursor, "after")) {
            stageInstanceModels = stageDao.findDetailedStageHistoryViaCursor(pipelineName, stageName, FeedModifier.After, afterCursor, pageSize);
        } else if (validateCursor(beforeCursor, "before")) {
            stageInstanceModels = stageDao.findDetailedStageHistoryViaCursor(pipelineName, stageName, FeedModifier.Before, beforeCursor, pageSize);
        } else {
            stageInstanceModels = stageDao.findDetailedStageHistoryViaCursor(pipelineName, stageName, FeedModifier.Latest, 0, pageSize);
        }
        return stageInstanceModels;
    }

    public PipelineRunIdInfo getOldestAndLatestStageInstanceId(Username username, String pipelineName, String stageName) {
        checkForExistenceAndAccess(username, pipelineName);
        return stageDao.getOldestAndLatestStageInstanceId(pipelineName, stageName);
    }

    private void checkForExistenceAndAccess(Username username, String pipelineName) {
        if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            throw new RecordNotFoundException(EntityType.Pipeline, pipelineName);
        }
        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            throw new NotAuthorizedException(EntityType.Pipeline.forbiddenToView(pipelineName, username.getUsername()));
        }
    }

    private boolean validateCursor(Long cursor, String key) {
        if (cursor == 0) return false;
        if (cursor < 0) {
            throw new BadRequestException(format(BAD_CURSOR_MSG, key));
        }
        return true;
    }

    /**
     * @return Listeners
     * @deprecated Used only in tests
     */
    List<StageStatusListener> getStageStatusListeners() {
        return stageStatusListeners;
    }

    /**
     * @deprecated don't call this directly, go through ScheduleService.cancelJob so that stageLevel synchronization is done
     */
    public void cancelJob(final JobInstance jobInstance) {
        changeJob(() -> jobInstanceService.cancelJob(jobInstance), jobInstance.getIdentifier());
    }

    /**
     * @deprecated don't call this directly, go through ScheduleService.failJob so that stageLevel synchronization is done
     */
    public void failJob(final JobInstance jobInstance) {
        changeJob(() -> jobInstanceService.failJob(jobInstance), jobInstance.getIdentifier());
    }

    private void changeJob(final JobOperation jobOperation, final JobIdentifier identifier) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobOperation.invoke();
                stageDao.clearCachedStage(identifier.getStageIdentifier());
                Stage stage = stageDao.findStageWithIdentifier(identifier.getStageIdentifier());
                updateStageWithoutNotifications(stage, null);
                notifyStageStatusChangeListeners(stage);
            }
        });
    }

    public boolean isStageActive(String pipelineName, String stageName) {
        return stageDao.isStageActive(pipelineName, stageName);
    }

    public boolean isAnyStageActiveForPipeline(PipelineIdentifier pipelineIdentifier) {
        return stageDao.findAllStagesFor(pipelineIdentifier.getName(), pipelineIdentifier.getCounter()).isAnyStageActive();
    }

    public List<Stage> oldestStagesWithDeletableArtifacts() {
        return stageDao.oldestStagesHavingArtifacts();
    }

    public void markArtifactsDeletedFor(Stage stage) {
        stageDao.markArtifactsDeletedFor(stage);
    }

    public List<StageIdentity> findLatestStageInstances() {
        return stageDao.findLatestStageInstances();
    }

    public interface JobOperation {
        void invoke();
    }
}
