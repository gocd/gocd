/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.StageNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.InvalidAgentException;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.service.result.*;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;

@Service
public class ScheduleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleService.class);

    private GoConfigService goConfigService;
    private PipelineService pipelineService;
    private StageService stageService;
    private SchedulingCheckerService schedulingChecker;
    private PipelineDao pipelineDao;
    private StageDao stageDao;
    private JobInstanceDao jobInstanceDao;
    private AgentAssignment agentAssignment;
    private StageOrderService stageOrderService;
    private SecurityService securityService;
    private PipelineScheduleQueue pipelineScheduleQueue;
    private JobInstanceService jobInstanceService;
    private EnvironmentConfigService environmentConfigService;
    private PipelineLockService pipelineLockService;
    private ServerHealthService serverHealthService;
    private AgentService agentService;
    private TransactionSynchronizationManager synchronizationManager;
    private TimeProvider timeProvider;
    private TransactionTemplate transactionTemplate;

    private final Object autoScheduleMutex = new Object();
    private ConsoleActivityMonitor consoleActivityMonitor;
    private PipelinePauseService pipelinePauseService;
    private InstanceFactory instanceFactory;
    private SchedulingPerformanceLogger schedulingPerformanceLogger;
    private ElasticProfileService elasticProfileService;
    private ClusterProfilesService clusterProfilesService;

    protected ScheduleService() {
    }

    @Autowired
    public ScheduleService(GoConfigService goConfigService,
                           PipelineService pipelineService,
                           StageService stageService,
                           SchedulingCheckerService schedulingChecker,
                           PipelineDao pipelineDao,
                           StageDao stageDao,
                           StageOrderService stageOrderService,
                           SecurityService securityService,
                           PipelineScheduleQueue pipelineScheduleQueue,
                           JobInstanceService jobInstanceService,
                           JobInstanceDao jobInstanceDao,
                           AgentAssignment agentAssignment,
                           EnvironmentConfigService environmentConfigService,
                           PipelineLockService pipelineLockService,
                           ServerHealthService serverHealthService,
                           TransactionTemplate transactionTemplate,
                           AgentService agentService,
                           TransactionSynchronizationManager synchronizationManager,
                           TimeProvider timeProvider,
                           ConsoleActivityMonitor consoleActivityMonitor,
                           PipelinePauseService pipelinePauseService,
                           InstanceFactory instanceFactory,
                           SchedulingPerformanceLogger schedulingPerformanceLogger,
                           ElasticProfileService elasticProfileService,
                           ClusterProfilesService clusterProfilesService
    ) {
        this.goConfigService = goConfigService;
        this.pipelineService = pipelineService;
        this.stageService = stageService;
        this.schedulingChecker = schedulingChecker;
        this.pipelineDao = pipelineDao;
        this.stageDao = stageDao;
        this.stageOrderService = stageOrderService;
        this.securityService = securityService;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
        this.jobInstanceService = jobInstanceService;
        this.jobInstanceDao = jobInstanceDao;
        this.agentAssignment = agentAssignment;
        this.environmentConfigService = environmentConfigService;
        this.pipelineLockService = pipelineLockService;
        this.serverHealthService = serverHealthService;
        this.transactionTemplate = transactionTemplate;
        this.agentService = agentService;
        this.synchronizationManager = synchronizationManager;
        this.timeProvider = timeProvider;
        this.consoleActivityMonitor = consoleActivityMonitor;
        this.pipelinePauseService = pipelinePauseService;
        this.instanceFactory = instanceFactory;
        this.schedulingPerformanceLogger = schedulingPerformanceLogger;
        this.elasticProfileService = elasticProfileService;
        this.clusterProfilesService = clusterProfilesService;
    }

    //Note: This is called from a Spring timer
    public void autoSchedulePipelinesFromRequestBuffer() {
        synchronized (autoScheduleMutex) {
            try {
                for (Entry<CaseInsensitiveString, BuildCause> entry : pipelineScheduleQueue.toBeScheduled().entrySet()) {
                    CaseInsensitiveString pipelineName = entry.getKey();
                    BuildCause buildCause = entry.getValue();

                    LOGGER.info("[Pipeline Schedule] Scheduling pipeline {} with build cause {}", pipelineName, buildCause);

                    long schedulingStartTime = System.currentTimeMillis();
                    Pipeline pipeline = schedulePipeline(pipelineName, buildCause);
                    long schedulingEndTime = System.currentTimeMillis();

                    if (pipeline != null) {
                        schedulingPerformanceLogger.scheduledPipeline(pipelineName, pipelineScheduleQueue.toBeScheduled().size(), schedulingStartTime, schedulingEndTime);
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("[Pipeline Schedule] An exception occurred while scheduling the pipeline. {}", e);
            }
        }
    }

    Pipeline schedulePipeline(final CaseInsensitiveString pipelineName, final BuildCause buildCause) {
        try {
            PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(pipelineName);

            if (canSchedule(pipelineConfig)) {
                final Pipeline pipelineInstance = pipelineScheduleQueue.createPipeline(buildCause, pipelineConfig, schedulingContext(buildCause.getApprover(), pipelineConfig, pipelineConfig.first()),
                        goConfigService.getCurrentConfig().getMd5(), timeProvider);
                serverHealthService.update(stageSchedulingSuccessfulState(pipelineName.toString(), CaseInsensitiveString.str(pipelineConfig.get(0).name())));
                return pipelineInstance;
            }
        } catch (RecordNotFoundException e) {
            LOGGER.error("Could not find pipeline {}", pipelineName, e);
            pipelineScheduleQueue.clearPipeline(pipelineName);
        } catch (CannotScheduleException e) {
            pipelineScheduleQueue.clearPipeline(pipelineName);
            serverHealthService.update(stageSchedulingFailedState(pipelineName.toString(), e));
        } catch (Exception e) {
            LOGGER.error("Error while scheduling pipeline {}", pipelineName, e);
            pipelineScheduleQueue.clearPipeline(pipelineName);
        }
        return null;
    }

    private ServerHealthState stageSchedulingFailedState(String pipelineName, CannotScheduleException e) {
        return ServerHealthState.failedToScheduleStage(HealthStateType.general(HealthStateScope.forStage(pipelineName, e.getStageName())), pipelineName, e.getStageName(), e.getMessage());
    }

    private ServerHealthState stageSchedulingSuccessfulState(String pipelineName, String stageName) {
        return ServerHealthState.success(HealthStateType.general(HealthStateScope.forStage(pipelineName, stageName)));
    }

    private Stage internalRerun(Pipeline pipeline, String stageName, String approvedBy, final StageInstanceCreator creator, ErrorConditionHandler errorHandler) {
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        if (!schedulingChecker.canRerunStage(pipeline.getIdentifier(), stageName, approvedBy, result)) {
            errorHandler.cantSchedule("Cannot schedule: " + result.getServerHealthState().getDescription(), pipeline.getName(), stageName);
        }
        return scheduleStage(pipeline, stageName, approvedBy, creator, errorHandler);
    }

    public Stage scheduleStage(final Pipeline pipeline, final String stageName, final String username, final StageInstanceCreator creator, final ErrorConditionHandler errorHandler) {
        return (Stage) transactionTemplate.execute((TransactionCallback) status -> {
            String pipelineName = pipeline.getName();
            PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
            StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(stageName));
            if (stageConfig == null) {
                throw new StageNotFoundException(pipelineName, stageName);
            }
            SchedulingContext context = schedulingContext(username, pipelineConfig, stageConfig);
            pipelineLockService.lockIfNeeded(pipeline);
            Stage instance = null;
            try {
                instance = creator.create(pipelineName, stageName, context);
                LOGGER.info("[Stage Schedule] Scheduling stage {} for pipeline {}", stageName, pipeline.getName());
            } catch (CannotScheduleException e) {
                serverHealthService.update(stageSchedulingFailedState(pipelineName, e));
                errorHandler.cantSchedule(e, pipelineName);
            }
            serverHealthService.update(stageSchedulingSuccessfulState(pipelineName, stageName));
            stageService.save(pipeline, instance);
            return instance;
        });
    }

    private SchedulingContext schedulingContext(String username, PipelineConfig pipelineConfig, StageConfig stageConfig) {
        Agents availableAgents = environmentConfigService.agentsForPipeline(pipelineConfig.name());
        SchedulingContext context = new DefaultSchedulingContext(username, availableAgents, elasticProfileService.listAll(), clusterProfilesService.listAll());
        context = context.overrideEnvironmentVariables(pipelineConfig.getVariables());
        context = context.overrideEnvironmentVariables(stageConfig.getVariables());
        return context;
    }

    private boolean canSchedule(PipelineConfig pipelineConfig) {
        return schedulingChecker.canAutoTriggerConsumer(pipelineConfig);
    }

    public boolean rerunStage(Pipeline pipeline, StageConfig stageConfig, String approvedBy) {
        internalRerun(pipeline, CaseInsensitiveString.str(stageConfig.name()), approvedBy, new NewStageInstanceCreator(goConfigService), new ExceptioningErrorHandler());
        return true;
    }

    public Stage rerunStage(String pipelineName, Integer pipelineCounter, String stageName) {
        return rerunStage(pipelineName, pipelineCounter, stageName, new ExceptioningErrorHandler());
    }

    public Stage rerunStage(String pipelineName, Integer pipelineCounter, String stageName, ErrorConditionHandler errorHandler) {
        return lockAndRerunStage(pipelineName, pipelineCounter, stageName, new NewStageInstanceCreator(goConfigService), errorHandler);
    }

    /**
     * Top-level operation only; consumes exceptions
     */
    public Stage rerunStage(String pipelineName, Integer pipelineCounter, String stageName, HttpOperationResult result) {
        String identifier = StringUtils.join(Arrays.asList(pipelineName, pipelineCounter, stageName), "/");
        HealthStateType healthStateType = HealthStateType.general(HealthStateScope.forStage(pipelineName, stageName));
        Stage stage = null;

        try {
            stage = rerunStage(pipelineName, pipelineCounter, stageName, new ResultUpdatingErrorHandler(result));

            if (result.isSuccess()) { // this check is for clarity, but is not needed as failures result in RunTimeExceptions
                result.accepted(String.format("Request to schedule stage %s accepted", identifier), "", healthStateType);
            }
        } catch (RuntimeException e) {
            // if the result is successful, but there's still an exception, treat it as a 500
            // else the result is assumed to contain the right status code and error message
            if (result.isSuccess()) {
                String message = String.format(
                        "Stage rerun request for stage [%s] could not be completed " +
                                "because of an unexpected failure. Cause: %s", identifier,
                        e.getMessage()
                );
                LOGGER.error(message, e);
                result.internalServerError(message, healthStateType);
            }
        }
        return stage;
    }

    private Stage lockAndRerunStage(String pipelineName, Integer counter, String stageName, StageInstanceCreator creator, final ErrorConditionHandler errorHandler) {
        synchronized (mutexForPipeline(pipelineName)) {
            OperationResult result = new ServerHealthStateOperationResult();
            if (!schedulingChecker.canSchedule(result)) {
                errorHandler.cantSchedule(result.getServerHealthState().getDescription(), pipelineName, stageName);
            }

            String username = CaseInsensitiveString.str(SessionUtils.currentUsername().getUsername());
            if (!securityService.hasOperatePermissionForStage(pipelineName, stageName, username)) {
                errorHandler.noOperatePermission(pipelineName, stageName);
            }

            Pipeline pipeline = pipelineService.fullPipelineByCounter(pipelineName, counter);
            ScheduleStageResult canScheduleStage = schedulingChecker.shouldAllowSchedulingStage(pipeline, stageName);
            switch (canScheduleStage) {
                case PipelineNotFound:
                    errorHandler.nullPipeline(pipelineName, counter, stageName);
                    break;
                case PreviousStageNotRan:
                    errorHandler.previousStageNotRun(pipelineName, stageName);
                    break;
                case PreviousStageNotPassed:
                    String errorMessage = String.format("Cannot schedule %s as the previous stage %s has %s!", stageName, canScheduleStage.getPreviousStageName(), canScheduleStage.getPreviousStageResult());

                    errorHandler.cantSchedule(errorMessage, pipelineName, stageName);
                    break;
            }

            Stage stage = internalRerun(pipeline, stageName, username, creator, errorHandler);
            if (stage == null) {
                errorHandler.nullStage(stageName);
            }

            return stage;
        }
    }

    /**
     * IMPORTANT: this method is only meant for TOP level usage(never use this within a transaction). It gobbles exception.
     */
    public Stage rerunJobs(final Stage stage, final List<String> jobNames, final HttpOperationResult result) {
        final StageIdentifier identifier = stage.getIdentifier();
        HealthStateType healthStateForStage = HealthStateType.general(HealthStateScope.forStage(identifier.getPipelineName(), identifier.getStageName()));

        if (jobNames == null || jobNames.isEmpty()) {
            String message = "No job was selected to re-run.";
            result.badRequest(message, message, healthStateForStage);
            return null;
        }
        try {
            Stage resultStage = lockAndRerunStage(identifier.getPipelineName(), identifier.getPipelineCounter(), identifier.getStageName(), (pipelineName, stageName, context) -> {
                StageConfig stageConfig = goConfigService.stageConfigNamed(identifier.getPipelineName(), identifier.getStageName());
                String latestMd5 = goConfigService.getCurrentConfig().getMd5();
                try {
                    return instanceFactory.createStageForRerunOfJobs(stage, jobNames, context, stageConfig, timeProvider, latestMd5);
                } catch (CannotRerunJobException e) {
                    result.notFound(e.getMessage(), e.getMessage(), healthStateForStage);
                    throw e;
                }
            }, new ResultUpdatingErrorHandler(result));

            result.accepted(String.format("Request to rerun jobs accepted", identifier), "", healthStateForStage);
            return resultStage;
        } catch (RuntimeException e) {
            if (result.canContinue()) {
                String message = String.format("Job rerun request for job(s) [%s] could not be completed because of unexpected failure. Cause: %s", StringUtils.join(jobNames.toArray(), ", "),
                        e.getMessage());
                result.internalServerError(message, healthStateForStage);
                LOGGER.error(message, e);
            }
            return null;
        }
    }

    /**
     * IMPORTANT: this method is only meant for TOP level usage(never use this within a transaction). It gobbles exception.
     */
    public Stage rerunFailedJobs(final Stage stage, final HttpOperationResult result) {
        final StageIdentifier identifier = stage.getIdentifier();
        JobInstances jobInstances = stage.jobsWithResult(JobResult.Cancelled, JobResult.Failed);
        List<String> jobNames = jobInstances.stream().map(JobInstance::getName).collect(Collectors.toList());

        if (jobNames.isEmpty()) {
            String message = "There are no failed jobs in the stage that could be re-run";
            result.badRequest(message, message, HealthStateType.general(HealthStateScope.forStage(identifier.getPipelineName(), identifier.getStageName())));
            return null;
        }
        return rerunJobs(stage, jobNames, result);
    }

    private String mutexForPipeline(String pipelineName) {
        String s = String.format("%s_forPipeline_%s", getClass().getName(), pipelineName);
        return s.intern(); // interned because we synchronize on it
    }

    private void triggerNextStageInPipeline(Pipeline pipeline, String stageName, String approvedBy) {
        StageConfig nextStage = stageOrderService.getNextStage(pipeline, stageName);
        if (nextStage == null) {
            return;
        }
        if (!nextStage.supportAutoApproval()) {
            return;
        }
        if (isStageActive(pipeline, nextStage)) {
            return;
        }
        scheduleStage(pipeline, CaseInsensitiveString.str(nextStage.name()), approvedBy, new NewStageInstanceCreator(goConfigService), new ExceptioningErrorHandler());
    }

    //this method checks if specified stage is active in all pipelines

    private boolean isStageActive(Pipeline pipeline, StageConfig nextStage) {
        return stageDao.isStageActive(pipeline.getName(), CaseInsensitiveString.str(nextStage.name()));
    }

    public void automaticallyTriggerRelevantStagesFollowingCompletionOf(Stage stage) {
        if (!stage.isCompleted()) {
            return;
        }
        try {
            Pipeline pipeline = pipelineDao.loadPipeline(stage.getPipelineId());

            unlockIfNecessary(pipeline, stage);

            if (pipelinePauseService.isPaused(pipeline.getName())) {
                return;
            }
            // if there has been a newer successful run of the previous stage, we should trigger off this stage again
            // in the same pipeline instance that the newer successful run happened in.
            //TODO: ChrisS & LYH : This is only accidentally working
            //
            if (shouldTriggerThisStageInNewerPipeline(pipeline, stage)) {
                triggerCurrentStageInNewerPipeline(pipeline.getName(), stage);
            }
            // if this stage completed successfully, we should try to trigger the next stage in this pipeline
            if (stage.isCompletedAndPassed()) {
                triggerNextStageInPipeline(pipeline, stage.getName(), DEFAULT_APPROVED_BY);
            }
        } catch (Exception ex) {
            String message = String.format("Failed to trigger next stage for %s.", stage.getName());
            LOGGER.error(message, ex);
            throw ex;
        }
    }

    public void unlockIfNecessary(Pipeline pipeline, Stage stage) {
        StageConfig nextStage = stageOrderService.getNextStage(pipeline, stage.getName());

        boolean isLastStage = nextStage == null;

        boolean isUnlockable = goConfigService.isUnlockableWhenFinished(pipeline.getName());
        boolean nextStageIsManual = nextStage != null && nextStage.requiresApproval();
        boolean pipelineIsConsideredToBeComplete = stage.isCompleted() && (!stage.passed() || nextStageIsManual);

        if (isLastStage || (isUnlockable && pipelineIsConsideredToBeComplete)) {
            pipelineLockService.unlock(pipeline.getName());
        }
    }

    private boolean shouldTriggerThisStageInNewerPipeline(Pipeline pipeline, Stage stage) {
        return !goConfigService.isFirstStage(pipeline.getName(), stage.getName())
                && !goConfigService.requiresApproval(new CaseInsensitiveString(pipeline.getName()), new CaseInsensitiveString(stage.getName()));
    }

    private void triggerCurrentStageInNewerPipeline(String pipelineName, Stage currentStage) {
        // get the most recent passed stage in this collection of pipeline id's
        StageConfig previousStage = goConfigService.previousStage(pipelineName, currentStage.getName());
        Stage mostRecentPassed = stageService.mostRecentPassed(pipelineName, CaseInsensitiveString.str(previousStage.name()));

        if (mostRecentPassed != null && mostRecentPassed.getPipelineId() > currentStage.getPipelineId()) {
            Pipeline mostRecentEligiblePipeline = pipelineDao.loadPipeline(mostRecentPassed.getPipelineId());
            if (!mostRecentEligiblePipeline.hasStageBeenRun(currentStage.getName())) {
                triggerNextStageInPipeline(mostRecentEligiblePipeline, mostRecentPassed.getName(), DEFAULT_APPROVED_BY);
            }
        }
    }

    // synchronized for updating job

    public Stage cancelAndTriggerRelevantStages(Long stageId, Username username, LocalizedOperationResult result) throws Exception {
        Stage stageForId;
        LocalizedOperationResult opResult = result == null ? new DefaultLocalizedOperationResult() : result;
        try {
            stageForId = stageService.stageById(stageId);
        } catch (Exception e) {
            LOGGER.error("[Stage Cancellation] Failed to retrieve stage identifier", e);
            opResult.notFound("Stage '" + stageId + "' not found.", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }

        if (!stageForId.isActive()) {
            opResult.setMessage("Stage is not active. Cancellation Ignored.");
            return stageForId;
        }

        String stageMutex = mutexForStageInstance(stageForId.getIdentifier());
        synchronized (stageMutex) {
            // reload stage so we see committed state after acquiring mutex
            final Stage stage = stageService.stageById(stageId);

            String pipelineName = stage.getIdentifier().getPipelineName();
            String stageName = stage.getIdentifier().getStageName();
            String user = username == null ? null : username.getUsername().toString();

            if (!securityService.hasOperatePermissionForStage(pipelineName, stageName, user)) {
                opResult.forbidden("Unauthorized to operate stage named " + stageName, HealthStateType.forbidden());
                return null;
            }

            LOGGER.info("[Stage Cancellation] Cancelling stage {}", stage.getIdentifier());
            transactionTemplate.executeWithExceptionHandling(new com.thoughtworks.go.server.transaction.TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    stageService.cancelStage(stage, user);
                }
            });

            transactionTemplate.executeWithExceptionHandling(new com.thoughtworks.go.server.transaction.TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws Exception {
                    automaticallyTriggerRelevantStagesFollowingCompletionOf(stage);
                }
            });

            opResult.setMessage("Stage cancelled successfully.");

            return stage;
        }
    }

    public boolean canRun(PipelineIdentifier pipelineIdentifier, String stageName, String username, boolean hasPreviousStageBeenScheduled) {
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        return canRun(pipelineIdentifier, stageName, username, hasPreviousStageBeenScheduled, result);
    }

    public boolean canRun(PipelineIdentifier pipelineIdentifier, String stageName, String username, boolean hasPreviousStageBeenScheduled, ServerHealthStateOperationResult result) {
        if (!goConfigService.hasStageConfigNamed(pipelineIdentifier.getName(), stageName)) {
            return false;
        }

        schedulingChecker.canScheduleStage(pipelineIdentifier, stageName, username, result);
        return result.getServerHealthState().isSuccess() && hasPreviousStageBeenScheduled;
    }

    // synchronized for updating job

    public void updateJobStatus(final JobIdentifier jobIdentifier, final JobState jobState) throws Exception {
        // have to synchronize at stage-level because cancellation happens at stage-level
        final String stageMutex = mutexForStageInstance(jobIdentifier);
        synchronized (stageMutex) {
            synchronized (mutexForJob(jobIdentifier)) {
                final JobInstance job = jobInstanceService.buildByIdWithTransitions(jobIdentifier.getBuildId());

                transactionTemplate.executeWithExceptionHandling(new com.thoughtworks.go.server.transaction.TransactionCallbackWithoutResult() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        if (job.isNull() || job.getState() == JobState.Rescheduled || job.getResult() == JobResult.Cancelled) {
                            return;
                        }

                        job.changeState(jobState);
                        //TODO: #2318 JobInstance should contain identifier after it's loaded from database
                        job.setIdentifier(jobIdentifier);
                        jobInstanceService.updateStateAndResult(job);

                        synchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                            @Override
                            public void afterCommit() {
                                stageDao.clearCachedAllStages(jobIdentifier.getPipelineName(), jobIdentifier.getPipelineCounter(), jobIdentifier.getStageName());
                            }
                        });

                        if (job.isCompleted()) {
                            Stage stage = stageService.stageById(job.getStageId());
                            stageService.updateResult(stage);
                        }
                    }
                });

                // this has to be in a separate transaction because the above should not fail due to errors when scheduling a the next stage
                // (e.g. CannotScheduleException thrown when there are no agents for run-on-all-agent jobs)
                transactionTemplate.executeWithExceptionHandling(new com.thoughtworks.go.server.transaction.TransactionCallbackWithoutResult() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) throws Exception {
                        if (job.isCompleted()) {
                            Stage stage = stageService.stageById(job.getStageId());
                            automaticallyTriggerRelevantStagesFollowingCompletionOf(stage);
                        }
                    }
                });
            }
        }
    }

    private String mutexForStageInstance(StageIdentifier id) {
        return mutexForStageInstance(id.getPipelineName(), id.getPipelineCounter(), id.getStageName(), id.getStageCounter());
    }

    private String mutexForStageInstance(JobIdentifier id) {
        return mutexForStageInstance(id.getPipelineName(), id.getPipelineCounter(), id.getStageName(), id.getStageCounter());
    }

    private String mutexForStageInstance(String pipelineName, Integer pipelineCounter, String stageName, String stageCounter) {
        String s = String.format("%s_forStageInstance_%s_%s_%s_%s", getClass().getName(), pipelineName, pipelineCounter, stageName, stageCounter);
        return s.intern(); // interned because we synchronize on it
    }

    //Note: This is called from a Spring timer

    public void rescheduleHungJobs() {
        try {
            //TODO 2779
            AgentInstances knownAgents = agentService.findRegisteredAgents();
            List<String> liveAgentIdList = getLiveAgentUuids(knownAgents);
            if (!liveAgentIdList.isEmpty()) {
                JobInstances jobs = jobInstanceService.findHungJobs(liveAgentIdList);
                for (JobInstance buildId : jobs) {
                    LOGGER.warn("Found hung job[id={}], rescheduling it", buildId);
                    rescheduleJob(buildId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error occured during reschedule hung builds: ", e);
        }
    }

    // Note: This is also called from a spring timer (cancelHungJobs)
    public void cancelHungJobs() {
        try {
            consoleActivityMonitor.cancelUnresponsiveJobs(this);
        } catch (Exception e) {
            LOGGER.error("Error occurred during cancelling unresponsive job: ", e);
        }
    }

    private List<String> getLiveAgentUuids(AgentInstances knownAgents) {
        List<String> agents = new ArrayList<>();
        for (AgentInstance agent : knownAgents) {
            if (agent.getStatus() != AgentStatus.LostContact) {
                agents.add(agent.getAgent().getUuid());
            }
        }
        return agents;
    }

    public void rescheduleAbandonedBuildIfNecessary(final AgentIdentifier identifier) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                final JobInstance jobInstance = agentAssignment.latestActiveJobOnAgent(identifier.getUuid());
                if (jobInstance != null) {
                    LOGGER.warn("[Job Reschedule] Found latest incomplete job for agent {} [Job Instance: {}]", identifier, jobInstance);
                    rescheduleJob(jobInstance);
                }
            }
        });
    }

    //synchronized for updating job
    public void rescheduleJob(final JobInstance toBeRescheduled) {
        final JobIdentifier jobIdentifier = toBeRescheduled.getIdentifier();
        synchronized (mutexForStageInstance(jobIdentifier)) {
            synchronized (mutexForJob(jobIdentifier)) {
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        LOGGER.warn("[Job Reschedule] Rescheduling and marking old job as ignored: {}", toBeRescheduled);
                        //Reloading it because we want to see the latest committed state after acquiring the mutex.
                        JobInstance oldJob = jobInstanceService.buildById(toBeRescheduled.getId());
                        if (oldJob.isCompleted() || oldJob.isRescheduled()) {
                            return;
                        }
                        JobInstance newJob = oldJob.clone();
                        oldJob.changeState(JobState.Rescheduled);
                        jobInstanceService.updateStateAndResult(oldJob);

                        //Make a new Job
                        newJob.reschedule();
                        jobInstanceService.save(oldJob.getIdentifier().getStageIdentifier(), oldJob.getStageId(), newJob);

                        //Copy the plan for the old job since we don't load job plan with jobInstance by default
                        JobPlan plan = jobInstanceDao.loadPlan(oldJob.getId());
                        jobInstanceDao.ignore(oldJob);
                        jobInstanceDao.save(newJob.getId(), plan);
                        LOGGER.info("[Job Reschedule] Scheduled new job: {}. Replacing old job: {}", newJob.getIdentifier(), oldJob.getIdentifier());
                    }
                });
            }
        }
    }

    public void cancelJob(final JobInstance instance) {
        synchronized (mutexForStageInstance(instance.getIdentifier())) {
            stageService.cancelJob(instance);
        }
    }

    public void jobCompleting(JobIdentifier jobIdentifier, JobResult result, String agentUuid) {
        // have to synchronize at stage-level because cancellation happens at stage-level
        synchronized (mutexForStageInstance(jobIdentifier)) {
            synchronized (mutexForJob(jobIdentifier)) {
                JobInstance jobInstance = jobInstanceService.buildByIdWithTransitions(jobIdentifier.getBuildId());
                if (jobInstance.isNull() || jobInstance.getResult() == JobResult.Cancelled || jobInstance.getState() == JobState.Rescheduled) {
                    return;
                }
                //TODO: #2318 JobInstance should contain identifier after it's loaded from database
                jobInstance.setIdentifier(jobIdentifier);
                if (!StringUtils.equals(jobInstance.getAgentUuid(), agentUuid)) {
                    LOGGER.error("Build Instance is using agent [{}] but status updating from agent [{}]", jobInstance.getAgentUuid(), agentUuid);
                    throw new InvalidAgentException("AgentUUID has changed in the middle of a job. AgentUUID:"
                            + agentUuid + ", Build: " + jobInstance.toString());
                }
                jobInstance.completing(result);
                jobInstanceService.updateStateAndResult(jobInstance);
            }
        }
    }

    public boolean updateAssignedInfo(String agentUuid, JobPlan job) {
        // have to synchronize at stage-level because cancellation happens at stage-level
        JobIdentifier jobIdentifier = job.getIdentifier();
        synchronized (mutexForStageInstance(jobIdentifier)) {
            JobInstance instance = jobInstanceService.buildByIdWithTransitions(job.getJobId());
            if (instance.getState() == JobState.Completed) {
                LOGGER.info("[Agent Assignment] Not assigning a completed job [{}] to agent {}", instance.getIdentifier(), agentUuid);
                return true;
            }
            instance.assign(agentUuid, timeProvider.currentTime());
            jobInstanceService.updateAssignedInfo(instance);
            return false;
        }
    }

    public String mutexForJob(JobIdentifier jobIdentifier) {
        return String.format("%s_forJobInstance_%s", getClass().getName(), jobIdentifier.buildLocator()).intern();
    }

    public void cancelJob(JobIdentifier jobIdentifier) {
        cancelJob(jobInstanceService.buildById(jobIdentifier.getBuildId()));
    }

    public void failJob(JobInstance instance) {
        synchronized (mutexForStageInstance(instance.getIdentifier())) {
            stageService.failJob(instance);
        }
    }

    public interface StageInstanceCreator {
        Stage create(final String pipelineName, final String stageName, final SchedulingContext context);
    }

    public static class NewStageInstanceCreator implements StageInstanceCreator {

        private final GoConfigService goConfigService;

        public NewStageInstanceCreator(GoConfigService goConfigService) {
            this.goConfigService = goConfigService;
        }

        @Override
        public Stage create(final String pipelineName, final String stageName, final SchedulingContext context) {
            return goConfigService.scheduleStage(pipelineName, stageName, context);
        }
    }

    public interface ErrorConditionHandler {
        void nullStage(String stageName);

        void cantSchedule(String description, String pipelineName, String stageName);

        void noOperatePermission(String pipelineName, String stageName);

        void nullPipeline(String pipelineName, Integer pipelineCounter, String stageName);

        void previousStageNotRun(String pipelineName, String stageName);

        void cantSchedule(CannotScheduleException e, String pipelineName);
    }

    public static class ExceptioningErrorHandler implements ErrorConditionHandler {
        @Override
        public void nullStage(String stageName) {
            throw new RuntimeException(String.format("Stage [%s] not found", stageName));
        }

        @Override
        public void cantSchedule(String description, String pipelineName, String stageName) {
            throw new RuntimeException(description);
        }

        @Override
        public void noOperatePermission(String pipelineName, String stageName) {
            throw new NotAuthorizedException(noOperatePermissionMessage(pipelineName, stageName));
        }

        @Override
        public void nullPipeline(String pipelineName, Integer pipelineCounter, String stageName) {
            throw new RecordNotFoundException(String.format("Pipeline instance [%s/%s] not found", pipelineName, pipelineCounter));
        }

        @Override
        public void previousStageNotRun(String pipelineName, String stageName) {
            throw new RuntimeException(previousStageNotRunMessage(pipelineName, stageName));
        }

        protected String previousStageNotRunMessage(String pipelineName, String stageName) {
            return String.format("Can not run stage [%s] in pipeline [%s] because its previous stage has not been run.", stageName, pipelineName);
        }

        @Override
        public void cantSchedule(CannotScheduleException e, String pipelineName) {
            throw e;
        }

        protected String noOperatePermissionMessage(String pipelineName, String stageName) {
            return String.format("User does not have operate permissions for stage [%s] of pipeline [%s]", stageName, pipelineName);
        }

    }

    private static class ResultUpdatingErrorHandler extends ExceptioningErrorHandler {
        private final OperationResult result;

        public ResultUpdatingErrorHandler(OperationResult result) {
            this.result = result;
        }

        @Override
        public void cantSchedule(String description, String pipelineName, String stageName) {
            result.conflict(description, description, stageScopedHealthState(pipelineName, stageName));
            super.cantSchedule(description, pipelineName, stageName);
        }

        private HealthStateType stageScopedHealthState(String pipelineName, String stageName) {
            return HealthStateType.general(HealthStateScope.forStage(pipelineName, stageName));
        }

        @Override
        public void previousStageNotRun(String pipelineName, String stageName) {
            String message = previousStageNotRunMessage(pipelineName, stageName);
            result.badRequest(message, message, stageScopedHealthState(pipelineName, stageName));
            super.previousStageNotRun(pipelineName, stageName);
        }

        @Override
        public void noOperatePermission(String pipelineName, String stageName) {
            String message = noOperatePermissionMessage(pipelineName, stageName);
            result.forbidden(message, message, stageScopedHealthState(pipelineName, stageName));
            super.noOperatePermission(pipelineName, stageName);
        }

        @Override
        public void cantSchedule(CannotScheduleException e, String pipelineName) {
            result.conflict(e.getMessage(), e.getMessage(), stageScopedHealthState(pipelineName, e.getStageName()));
            super.cantSchedule(e, pipelineName);
        }
    }

}
