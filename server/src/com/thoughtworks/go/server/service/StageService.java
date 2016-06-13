/*
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
 */

package com.thoughtworks.go.server.service;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.StageStatusCache;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.domain.feed.FeedEntry;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.dto.DurationBeans;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.dao.sparql.StageRunFinder;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.StageStatusMessage;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.MingleCard;
import com.thoughtworks.go.server.ui.ModificationForPipeline;
import com.thoughtworks.go.server.ui.StageSummaryModel;
import com.thoughtworks.go.server.ui.StageSummaryModels;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.*;

@Service
public class StageService implements StageRunFinder, StageFinder {
    private static final Logger LOGGER = Logger.getLogger(StageService.class);
    private static final int FEED_PAGE_SIZE = 25;

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
    public StageService(StageDao stageDao, JobInstanceService jobInstanceService, StageStatusTopic stageStatusTopic, StageStatusCache stageStatusCache,
                        SecurityService securityService, PipelineDao pipelineDao, ChangesetService changesetService, GoConfigService goConfigService,
                        TransactionTemplate transactionTemplate, TransactionSynchronizationManager transactionSynchronizationManager, GoCache goCache,
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

    public String stageNameByStageId(long stageId) {
        return stageDao.stageNameByStageId(stageId);
    }

    public Stage stageById(long id) {
        return stageDao.stageById(id);
    }

    public Stage getStageByIdWithBuilds(long id) {
        return stageDao.stageByIdWithBuilds(id);
    }

    public Stage findStageWithIdentifier(String pipelineName, int pipelineCounter, String stageName, String stageCounter, String username, OperationResult result) {
        if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound("Not Found", "Pipeline not found", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }
        if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
            result.unauthorized("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return null;
        }

        return findStageWithIdentifier(new StageIdentifier(pipelineName, pipelineCounter, stageName, stageCounter));
    }

    public Stage findStageWithIdentifier(StageIdentifier identifier) {
        return stageDao.findStageWithIdentifier(identifier);
    }

    public StageSummaryModel findStageSummaryByIdentifier(StageIdentifier stageId, Username username, LocalizedOperationResult result) {
        if (!securityService.hasViewPermissionForPipeline(username, stageId.getPipelineName())) {
            result.unauthorized(LocalizedMessage.cannotViewPipeline(stageId.getPipelineName()), HealthStateType.general(HealthStateScope.forPipeline(stageId.getPipelineName())));
            return null;
        }
        Stages stages = stageDao.getAllRunsOfStageForPipelineInstance(stageId.getPipelineName(), stageId.getPipelineCounter(), stageId.getStageName());
        for (Stage stage : stages) {
            if (stage.getIdentifier().getStageCounter().equals(stageId.getStageCounter())) {
                StageSummaryModel summaryModel = new StageSummaryModel(stage, stages, stageDao, null);
                return summaryModel;
            }
        }
        result.notFound(LocalizedMessage.stageNotFound(stageId), HealthStateType.general(HealthStateScope.GLOBAL));
        return null;
    }

    public synchronized void cancelStage(final Stage stage) {
        cancel(stage);
        notifyStageStatusChangeListeners(stage);
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override public void afterCommit() {
                stageStatusTopic.post(new StageStatusMessage(stage.getIdentifier(), stage.stageState(), stage.getResult(), UserHelper.getUserName()));
            }
        });
    }

    private void cancel(final Stage stage) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (JobInstance job : stage.getJobInstances()) {
                    jobInstanceService.cancelJob(job);
                }
                updateStageWithoutNotifications(stage);
            }
        });
    }

    public DurationBeans getBuildDurations(String pipelineName, Stage stage) {
        DurationBeans durationBeans = new DurationBeans();
        for (JobInstance job : stage.getJobInstances()) {
            durationBeans.add(getDuration(pipelineName, stage.getName(), job));
        }
        return durationBeans;
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

    public Stage stageByIdWithModifications(long stageId) {
        return stageDao.stageById(stageId);
    }

    public Stage mostRecentPassed(String pipelineName, String stageName) {
        return stageDao.mostRecentPassed(pipelineName, stageName);
    }

    public int getCount(String pipelineName, String stageName) {
        return stageDao.getCount(pipelineName, stageName);
    }

    public Stage save(final Pipeline pipeline, final Stage stage) {
        return (Stage) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                stage.building();
                final Stage savedStage = persistStage(pipeline, stage);
                persistJobs(savedStage);
                notifyStageStatusChangeListeners(savedStage);
                return savedStage;
            }
        });
    }

    private void notifyStageStatusChangeListeners(final Stage savedStage) {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override public void afterCommit() {
                StageStatusListener[] prototype = new StageStatusListener[0];
                for (StageStatusListener stageStatusListener : stageStatusListeners.toArray(prototype)) {
                    try {
                        stageStatusListener.stageStatusChanged(savedStage);
                    } catch (Throwable e) {
                        LOGGER.error("error notifying listener for stage " + savedStage, e);
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
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                updateStageWithoutNotifications(stage);
                notifyStageStatusChangeListeners(stage);
            }
        });
    }

    private void updateStageWithoutNotifications(final Stage stage) {
        stage.calculateResult();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        clearCachedCompletedStageFeeds(stage.getIdentifier().getPipelineName());
                    }
                });
                stageDao.updateResult(stage, stage.getResult());
            }
        });
    }

    public Stage mostRecentStageWithBuilds(String pipelineName, StageConfig stageConfig) {
        Stage stage = findLatestStage(pipelineName, CaseInsensitiveString.str(stageConfig.name()));
        if (stage == null) {
            return NullStage.createNullStage(stageConfig);
        }
        stage.setJobInstances(jobInstanceService.currentJobsOfStage(pipelineName, stageConfig));
        return stage;
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
                    populateAuthorsAndMingleCards(feedEntries, pipelineName, username);
                    goCache.put(key, feedEntries);
                }
            }
        }
        return cloner.deepClone(new FeedEntries(new ArrayList<FeedEntry>(feedEntries)));
    }

    private String cacheKeyForLatestStageFeedForPipeline(String pipelineName) {
        return String.format("%s_latestStageFeedForPipeline_%s", getClass().getName(), pipelineName).intern();
    }

    private void clearCachedCompletedStageFeeds(String pipelineName) {
        String key = cacheKeyForLatestStageFeedForPipeline(pipelineName);
        synchronized (key) {
            goCache.remove(key);
        }
    }

    public FeedEntries feedBefore(long entryId, String pipelineName, Username username) {
        List<StageFeedEntry> stageEntries = stageDao.findCompletedStagesFor(pipelineName, FeedModifier.Before, entryId, FEED_PAGE_SIZE);
        populateAuthorsAndMingleCards(stageEntries, pipelineName, username);
        return new FeedEntries(new ArrayList<FeedEntry>(stageEntries));
    }

    private void populateAuthorsAndMingleCards(List<StageFeedEntry> stageEntries, String pipelineName, Username username) {
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
                if (config.hasPipelineNamed(new CaseInsensitiveString(pipelineForRev))) {
                    PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineForRev));
                    MingleConfig mingleConfig = pipelineConfig.getMingleConfig();
                    Set<String> cardNos = rev.getCardNumbersFromComments();
                    if (mingleConfig.isDefined()) {
                        for (String cardNo : cardNos) {
                            stageEntry.addCard(new MingleCard(mingleConfig, cardNo));
                        }
                    }
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("pipeline not found: " + pipelineForRev);
                    }
                }
            }
        }
    }

    public StageSummaryModels findStageHistoryForChart(String pipelineName, String stageName, int pageNumber, int pageSize, Username username) {
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

    public StageHistoryPage findStageHistoryPageByNumber(String pipelineName, String stageName, int pageNumber, int pageSize) {
        return stageDao.findStageHistoryPageByNumber(pipelineName, stageName, pageNumber, pageSize);
    }

	public StageInstanceModels findDetailedStageHistoryByOffset(String pipelineName, String stageName, Pagination pagination, String username, OperationResult result) {
		if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
			result.notFound("Not Found", "Pipeline not found", HealthStateType.general(HealthStateScope.GLOBAL));
			return null;
		}
		if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
			result.unauthorized("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
			return null;
		}

		return stageDao.findDetailedStageHistoryByOffset(pipelineName, stageName, pagination);
	}

    public Long findStageIdByLocator(String locator) {
        String[] parts = locator.split("/");
        String pipelineName = parts[0];
        String counterOrLabel = parts[1];
        if (counterOrLabel.matches(".+\\[.+\\]")) {
            counterOrLabel = counterOrLabel.substring(0, counterOrLabel.indexOf("["));
        }
        String stageName = parts[2];
        String stageCounter = parts[3];

        Pipeline pipeline = pipelineDao.findPipelineByCounterOrLabel(pipelineName, counterOrLabel);
        return stageDao.findStageIdByPipelineAndStageNameAndCounter(pipeline.getId(), stageName, stageCounter);
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
        changeJob(new JobOperation() {
            public void invoke() {
                jobInstanceService.cancelJob(jobInstance);
            }
        }, jobInstance.getIdentifier());
    }

    /**
     * @deprecated don't call this directly, go through ScheduleService.failJob so that stageLevel synchronization is done
     */
    public void failJob(final JobInstance jobInstance) {
        changeJob(new JobOperation() {
            public void invoke() {
                jobInstanceService.failJob(jobInstance);
            }
        }, jobInstance.getIdentifier());
    }

    private void changeJob(final JobOperation jobOperation, final JobIdentifier identifier) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobOperation.invoke();
                stageDao.clearCachedStage(identifier.getStageIdentifier());
                Stage stage = stageDao.findStageWithIdentifier(identifier.getStageIdentifier());
                updateStageWithoutNotifications(stage);
                notifyStageStatusChangeListeners(stage);
            }
        });
    }

    public List<StageIdentifier> findRunForStage(StageIdentifier stageIdentifier) {
        String pipelineName = stageIdentifier.getPipelineName();
        String stageName = stageIdentifier.getStageName();
        double toNaturalOrder = pipelineDao.findPipelineByNameAndCounter(pipelineName, stageIdentifier.getPipelineCounter()).getNaturalOrder();
        Pipeline pipelineThatLastPassed = pipelineDao.findEarlierPipelineThatPassedForStage(pipelineName, stageName, toNaturalOrder);
        double fromNaturalOrder = pipelineThatLastPassed != null ? pipelineThatLastPassed.getNaturalOrder() : 0.0;

        List<StageIdentifier> finalIds = new ArrayList<>();
        List<StageIdentifier> failedStages = stageDao.findFailedStagesBetween(pipelineName, stageName, fromNaturalOrder, toNaturalOrder);
        if (failedStages.isEmpty() || !failedStages.get(0).equals(stageIdentifier)) {
            return finalIds;
        }
        for (StageIdentifier identifier : failedStages) {
            finalIds.add(identifier);
        }
        return finalIds;
    }

    public boolean isStageActive(String pipelineName, String stageName) {
        return stageDao.isStageActive(pipelineName, stageName);
    }

    public boolean isAnyStageActiveForPipeline(String pipelineName, int counter) {
        return stageDao.findAllStagesFor(pipelineName, counter).isAnyStageActive();
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
