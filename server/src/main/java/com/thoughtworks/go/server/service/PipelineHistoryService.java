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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.PipelineStatusModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.thoughtworks.go.server.service.HistoryUtil.validateCursor;
import static java.lang.String.format;

@Service
public class PipelineHistoryService {
    private static final String NOT_AUTHORIZED_TO_VIEW_PIPELINE = "Not authorized to view pipeline";

    private final PipelineDao pipelineDao;
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private final MaterialRepository materialRepository;
    private final ScheduleService scheduleService;
    private final TriggerMonitor triggerMonitor;
    private final PipelineTimeline pipelineTimeline;
    private final PipelineUnlockApiService pipelineUnlockService;
    private final SchedulingCheckerService schedulingCheckerService;
    private final PipelineLockService pipelineLockService;
    private final PipelinePauseService pipelinePauseService;

    @Autowired
    public PipelineHistoryService(PipelineDao pipelineDao,
                                  GoConfigService goConfigService,
                                  SecurityService securityService,
                                  ScheduleService scheduleService,
                                  MaterialRepository materialRepository,
                                  TriggerMonitor triggerMonitor,
                                  PipelineTimeline pipelineTimeline,
                                  PipelineUnlockApiService pipelineUnlockService,
                                  SchedulingCheckerService schedulingCheckerService, PipelineLockService pipelineLockService,
                                  PipelinePauseService pipelinePauseService) {
        this.pipelineDao = pipelineDao;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.scheduleService = scheduleService;
        this.materialRepository = materialRepository;
        this.triggerMonitor = triggerMonitor;
        this.pipelineTimeline = pipelineTimeline;
        this.pipelineUnlockService = pipelineUnlockService;
        this.schedulingCheckerService = schedulingCheckerService;
        this.pipelineLockService = pipelineLockService;
        this.pipelinePauseService = pipelinePauseService;
    }

    public int totalCount(String pipelineName) {
        return pipelineDao.count(pipelineName);
    }

    public PipelineInstanceModel load(String pipelineName, int pipelineCounter, Username username) {
        PipelineInstanceModel pipeline = pipelineDao.findPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter);
        if (pipeline == null) {
            throw new RecordNotFoundException(EntityType.PipelineInstance, pipelineCounter);
        }

        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipeline.getName()));
        if (!securityService.hasViewPermissionForPipeline(username, pipeline.getName())) {
            throw new NotAuthorizedException(NOT_AUTHORIZED_TO_VIEW_PIPELINE);
        }
        populatePipelineInstanceModel(username, false, pipelineConfig, pipeline);
        return pipeline;
    }

    public PipelineInstanceModels load(String pipelineName, Pagination pagination, String username, boolean populateCanRun) {
        PipelineInstanceModels history = pipelineDao.loadHistory(pipelineName, pagination.getPageSize(), pagination.getOffset());

        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));

        for (PipelineInstanceModel pipelineInstanceModel : history) {
            populatePipelineInstanceModel(new Username(new CaseInsensitiveString(username)), populateCanRun, pipelineConfig, pipelineInstanceModel);
        }
        addEmptyPipelineInstanceIfNeeded(pipelineName, history, new Username(new CaseInsensitiveString(username)), pipelineConfig, populateCanRun);
        return history;
    }

    public PipelineInstanceModels loadPipelineHistoryData(Username username, String pipelineName, long afterCursor, long beforeCursor, int pageSize) {
        checkForExistenceAndAccess(username, pipelineName);
        PipelineInstanceModels history;
        if (validateCursor(afterCursor, "after")) {
            history = pipelineDao.loadHistory(pipelineName, FeedModifier.After, afterCursor, pageSize);
        } else if (validateCursor(beforeCursor, "before")) {
            history = pipelineDao.loadHistory(pipelineName, FeedModifier.Before, beforeCursor, pageSize);
        } else {
            history = pipelineDao.loadHistory(pipelineName, FeedModifier.Latest, 0, pageSize);
        }
        return populatePipelineInstanceModels(username, history);
    }

    public PipelineRunIdInfo getOldestAndLatestPipelineId(String pipelineName, Username username) {
        checkForExistenceAndAccess(username, pipelineName);
        return pipelineDao.getOldestAndLatestPipelineId(pipelineName);
    }

    private PipelineInstanceModels populatePipelineInstanceModels(Username username, PipelineInstanceModels history) {
        for (PipelineInstanceModel pipelineInstanceModel : history) {
            populatePlaceHolderStages(pipelineInstanceModel);
            populateCanRunStatus(username, pipelineInstanceModel);
            populateStageOperatePermission(pipelineInstanceModel, username);
        }

        return history;
    }

    private void checkForExistenceAndAccess(Username username, String pipelineName) {
        if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            throw new RecordNotFoundException(EntityType.Pipeline, pipelineName);
        }
        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            throw new NotAuthorizedException(NOT_AUTHORIZED_TO_VIEW_PIPELINE);
        }
    }

    public PipelineStatusModel getPipelineStatus(String pipelineName, String username, OperationResult result) {
        PipelineConfig pipelineConfig = goConfigService.currentCruiseConfig().getPipelineConfigByName(new CaseInsensitiveString(pipelineName));
        if (pipelineConfig == null) {
            result.notFound("Not Found", "Pipeline not found", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }
        if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
            result.forbidden("Forbidden", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return null;
        }

        PipelinePauseInfo pipelinePauseInfo = pipelinePauseService.pipelinePauseInfo(pipelineName);
        boolean isCurrentlyLocked = pipelineLockService.isLocked(pipelineName);
        boolean isSchedulable = schedulingCheckerService.canManuallyTrigger(pipelineConfig, username, new ServerHealthStateOperationResult());

        return new PipelineStatusModel(isCurrentlyLocked, isSchedulable, pipelinePauseInfo);
    }

    private void populatePipelineInstanceModel(Username username, boolean populateCanRun, PipelineConfig pipelineConfig, PipelineInstanceModel pipelineInstanceModel) {
        populatePipelineInstanceModel(pipelineConfig, pipelineInstanceModel);

        if (populateCanRun) {
            //make sure this happens after the placeholder stage is populated
            populateCanRunStatus(username, pipelineInstanceModel);
        }
        populateStageOperatePermission(pipelineInstanceModel, username);
    }

    private void populatePipelineInstanceModel(PipelineConfig pipelineConfig, PipelineInstanceModel pipelineInstanceModel) {
        if (pipelineInstanceModel.getId() > 0) {
            CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineInstanceModel.getName());
            pipelineInstanceModel.setPipelineAfter(pipelineTimeline.runAfter(pipelineInstanceModel.getId(), pipelineName));
            pipelineInstanceModel.setPipelineBefore(pipelineTimeline.runBefore(pipelineInstanceModel.getId(), pipelineName));
        }
        populatePlaceHolderStages(pipelineInstanceModel);
        populateMaterialRevisionsOnBuildCause(pipelineInstanceModel);
        pipelineInstanceModel.setMaterialConfigs(pipelineConfig.materialConfigs());
        pipelineInstanceModel.setLatestRevisions(materialRepository.findLatestRevisions(pipelineConfig.materialConfigs()));
    }

    //we need placeholder stage for unscheduled stages in pipeline, so we can trigger it
    private void populatePlaceHolderStages(PipelineInstanceModel pipeline) {
        StageInstanceModels stageHistory = pipeline.getStageHistory();
        String pipelineName = pipeline.getName();
        appendFollowingStagesFromConfig(pipelineName, stageHistory);
    }

    private void appendFollowingStagesFromConfig(String pipelineName, StageInstanceModels stageHistory) {
        stageHistory.updateFutureStagesFrom(goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName)));
    }

    private void populateCanRunStatus(Username username, PipelineInstanceModel pipelineInstanceModel) {
        for (StageInstanceModel stageHistoryItem : pipelineInstanceModel.getStageHistory()) {
            ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
            boolean canRun = scheduleService.canRun(
                    pipelineInstanceModel.getPipelineIdentifier(), stageHistoryItem.getName(),
                    CaseInsensitiveString.str(username.getUsername()), pipelineInstanceModel.hasPreviousStageBeenScheduled(
                            stageHistoryItem.getName()), result);
            stageHistoryItem.setCanRun(canRun);
            if (!canRun && result.getServerHealthState() != null && result.getServerHealthState().getType().equals(HealthStateType.forbidden())) {
                stageHistoryItem.setErrorMessage(result.getServerHealthState().getMessage());
            }
        }
        populatePipelineCanRunStatus(username, pipelineInstanceModel);
    }

    private void populatePipelineCanRunStatus(Username username, PipelineInstanceModel pipelineInstanceModel) {
        boolean canPipelineRun = schedulingCheckerService.canManuallyTrigger(pipelineInstanceModel.getName(), username);
        pipelineInstanceModel.setCanRun(canPipelineRun);
    }

    private void addEmptyPipelineInstanceIfNeeded(String pipelineName, PipelineInstanceModels history, Username username, PipelineConfig pipelineConfig, boolean populateCanRun) {
        if (history.isEmpty()) {
            PipelineInstanceModel model = addEmptyPipelineInstance(pipelineName, username, pipelineConfig, populateCanRun);
            history.add(model);
        }
    }

    private PipelineInstanceModel addEmptyPipelineInstance(String pipelineName, Username username, PipelineConfig pipelineConfig, boolean populateCanRun) {
        StageInstanceModels stageHistory = new StageInstanceModels();
        appendFollowingStagesFromConfig(pipelineName, stageHistory);
        PipelineInstanceModel model = PipelineInstanceModel.createEmptyPipelineInstanceModel(pipelineName, BuildCause.createWithEmptyModifications(), stageHistory);
        populatePipelineInstanceModel(username, populateCanRun, pipelineConfig, model);
        model.setCanRun(true);
        return model;
    }

    @VisibleForTesting
    PipelineInstanceModels loadWithEmptyAsDefault(String pipelineName, Pagination pagination, String userName) {
        if (!securityService.hasViewPermissionForPipeline(new Username(new CaseInsensitiveString(userName)), pipelineName)) {
            return PipelineInstanceModels.createPipelineInstanceModels();
        }
        PipelineInstanceModels pipelineInstanceModels;
        if (triggerMonitor.isAlreadyTriggered(new CaseInsensitiveString(pipelineName))) {

            StageInstanceModels stageHistory = new StageInstanceModels();
            appendFollowingStagesFromConfig(pipelineName, stageHistory);
            PipelineInstanceModel model = PipelineInstanceModel.createPreparingToSchedule(pipelineName, stageHistory);
            model.setCanRun(false);

            pipelineInstanceModels = PipelineInstanceModels.createPipelineInstanceModels(model);
        } else {
            pipelineInstanceModels = load(pipelineName, pagination, userName, true);
        }
        return pipelineInstanceModels;
    }

    public @Nullable PipelineInstanceModel latest(String pipelineName, Username username) {
        PipelineInstanceModels models = loadWithEmptyAsDefault(pipelineName, Pagination.ONE_ITEM, CaseInsensitiveString.str(username.getUsername()));
        return models.isEmpty() ? null : models.getFirst();
    }

    public PipelineInstanceModels latestInstancesForConfiguredPipelines(Username username) {
        PipelineInstanceModels pipelineInstances = PipelineInstanceModels.createPipelineInstanceModels();
        for (PipelineConfigs group : goConfigService.currentCruiseConfig().getGroups()) {
            for (PipelineConfig pipelineConfig : group) {
                PipelineInstanceModel pipelineInstanceModel = latest(CaseInsensitiveString.str(pipelineConfig.name()), username);
                if (pipelineInstanceModel != null) {
                    pipelineInstances.add(pipelineInstanceModel);
                }
            }
        }
        return pipelineInstances;
    }

    @SuppressWarnings("unused") // May be used in Rails code
    public boolean validate(String pipelineName, Username username, OperationResult result) {
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            String pipelineNotKnown = format("Pipeline named [%s] is not known.", pipelineName);
            result.notFound(pipelineNotKnown, pipelineNotKnown, HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            result.forbidden(NOT_AUTHORIZED_TO_VIEW_PIPELINE, NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return false;
        }
        return true;
    }

    public PipelineInstanceModel findPipelineInstance(String pipelineName, int pipelineCounter, Username username, OperationResult result) {
        return decoratePIM(pipelineName, pipelineCounter, username, result, pipelineDao.findPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter));
    }

    @SuppressWarnings("unused") // May be used in Rails code
    public PipelineInstanceModel findPipelineInstance(String pipelineName, int pipelineCounter, long id, Username username, OperationResult result) {
        return decoratePIM(pipelineName, pipelineCounter, username, result, pipelineDao.loadHistoryByIdWithBuildCause(id));
    }

    private PipelineInstanceModel decoratePIM(String pipelineName, int pipelineCounter, Username username, OperationResult result, PipelineInstanceModel pipelineInstanceModel) {
        if (!validate(pipelineName, username, result)) {
            return null;
        }
        if (pipelineInstanceModel == null && pipelineCounter == 0) {
            pipelineInstanceModel = PipelineInstanceModel.createEmptyPipelineInstanceModel(pipelineName, BuildCause.createWithEmptyModifications(), new StageInstanceModels());
        }
        if (pipelineInstanceModel == null) {
            String pipelineInstanceNotFound = format("Pipeline [%s/%s] not found.", pipelineName, pipelineCounter);
            result.notFound(pipelineInstanceNotFound, pipelineInstanceNotFound, HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }
        populatePlaceHolderStages(pipelineInstanceModel);
        populateStageOperatePermission(pipelineInstanceModel, username);
        populateCanRunStatus(username, pipelineInstanceModel);
        populateLockStatus(pipelineName, username, pipelineInstanceModel);
        return pipelineInstanceModel;
    }

    private void populateLockStatus(String pipelineName, Username username, PipelineInstanceModel pipelineInstanceModel) {
        pipelineInstanceModel.setCanUnlock(pipelineUnlockService.canUnlock(pipelineName, username, new HttpOperationResult()));
        pipelineInstanceModel.setIsLockable(goConfigService.isLockable(pipelineName));
        pipelineInstanceModel.setCurrentlyLocked(pipelineLockService.isLocked(pipelineName));
    }

    private void populateStageOperatePermission(PipelineInstanceModel pipelineInstanceModel, Username username) {
        for (StageInstanceModel stage : pipelineInstanceModel.getStageHistory()) {
            stage.setOperatePermission(securityService.hasOperatePermissionForStage(pipelineInstanceModel.getName(), stage.getName(), CaseInsensitiveString.str(username.getUsername())));
        }
    }

    public PipelineInstanceModels findMatchingPipelineInstances(String pipelineName, String pattern, int limit, Username userName, HttpLocalizedOperationResult result) {
        pattern = escapeWildCardsAndTrim(pattern.trim());
        if (!securityService.hasViewPermissionForPipeline(userName, pipelineName)) {
            result.forbidden(LocalizedMessage.forbiddenToViewPipeline(pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return PipelineInstanceModels.createPipelineInstanceModels();
        }
        PipelineInstanceModels models = pipelineDao.findMatchingPipelineInstances(pipelineName, pattern, limitForPipeline(pipelineName, limit));
        for (PipelineInstanceModel model : models) {
            populatePlaceHolderStages(model);
            populateMaterialRevisionsOnBuildCause(model);
        }
        return models;
    }

    private String escapeWildCardsAndTrim(String pattern) {
        pattern = pattern.replace("%", "\\%").replace("_", "\\_");
        return pattern;
    }

    private int limitForPipeline(String pipelineName, int limit) {
        return goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName)).size() * limit;
    }

    private void populateMaterialRevisionsOnBuildCause(PipelineInstanceModel model) {
        model.setMaterialRevisionsOnBuildCause(materialRepository.findMaterialRevisionsForPipeline(model.getId()));
    }

    public void updateComment(String pipelineName, int pipelineCounter, String comment, Username username) {
        if (!securityService.hasOperatePermissionForPipeline(username.getUsername(), pipelineName)) {
            throw new NotAuthorizedException(format("You do not have operate permissions for pipeline '%s'.", pipelineName));
        }

        Pipeline pipeline = pipelineDao.findPipelineByNameAndCounter(pipelineName, pipelineCounter);
        if (pipeline == null) {
            throw new RecordNotFoundException(format("Pipeline instance for '%s' with counter '%d' were not found!", pipelineName, pipelineCounter));
        }
        pipelineDao.updateComment(pipelineName, pipelineCounter, comment);
    }
}
