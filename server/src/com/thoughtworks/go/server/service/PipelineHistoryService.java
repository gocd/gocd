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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.PipelineDependencyGraphOld;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.PipelineStatusModel;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PipelineHistoryService implements PipelineInstanceLoader {
    private PipelineDao pipelineDao;
    private final StageDao stageDao;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private MaterialRepository materialRepository;
    private ScheduleService scheduleService;
    private TriggerMonitor triggerMonitor;
    private JobDurationStrategy jobDurationStrategy;
    private final PipelineTimeline pipelineTimeline;
    private PipelineUnlockApiService pipelineUnlockService;
    private SchedulingCheckerService schedulingCheckerService;
    private PipelineLockService pipelineLockService;
    private PipelinePauseService pipelinePauseService;
    private static final String NOT_AUTHORIZED_TO_VIEW_PIPELINE = "Not authorized to view pipeline";

    @Autowired
    public PipelineHistoryService(PipelineDao pipelineDao,
                                  StageDao stageDao,
                                  GoConfigService goConfigService,
                                  SecurityService securityService,
                                  ScheduleService scheduleService,
                                  MaterialRepository materialRepository,
                                  JobDurationStrategy jobDurationStrategy,
                                  TriggerMonitor triggerMonitor,
                                  PipelineTimeline pipelineTimeline,
                                  PipelineUnlockApiService pipelineUnlockService,
                                  SchedulingCheckerService schedulingCheckerService, PipelineLockService pipelineLockService,
                                  PipelinePauseService pipelinePauseService) {
        this.pipelineDao = pipelineDao;
        this.stageDao = stageDao;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.scheduleService = scheduleService;
        this.materialRepository = materialRepository;
        this.triggerMonitor = triggerMonitor;
        this.pipelineTimeline = pipelineTimeline;
        this.jobDurationStrategy = jobDurationStrategy;
        this.pipelineUnlockService = pipelineUnlockService;
        this.schedulingCheckerService = schedulingCheckerService;
        this.pipelineLockService = pipelineLockService;
        this.pipelinePauseService = pipelinePauseService;
    }

    public int totalCount(String pipelineName) {
        return pipelineDao.count(pipelineName);
    }

    public PipelineInstanceModel load(long id, Username username, OperationResult result) {
        PipelineInstanceModel pipeline = pipelineDao.loadHistory(id);
        if (pipeline == null) {
            result.notFound("Not Found", "Pipeline not found", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }
        PipelineConfig pipelineConfig = goConfigService.currentCruiseConfig().pipelineConfigByName(new CaseInsensitiveString(pipeline.getName()));
        if (!securityService.hasViewPermissionForPipeline(username, pipeline.getName())) {
            result.unauthorized("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipeline.getName())));
            return null;
        }
        populatePipelineInstanceModel(username, false, pipelineConfig, pipeline);
        return pipeline;
    }

    public PipelineInstanceModel loadPipelineForShine(long id) {// TODO: Fix method name - Sachin & JJ
        PipelineInstanceModel pipeline = pipelineDao.loadHistory(id);
        PipelineConfig pipelineConfig = goConfigService.currentCruiseConfig().pipelineConfigByName(new CaseInsensitiveString(pipeline.getName()));
        populatePipelineInstanceModel(pipelineConfig, pipeline);
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

	/*
	 * Load just enough data for Pipeline History API. The API is complete enough to build Pipeline History Page. Does following:
	 * Exists check, Authorized check, Loads paginated pipeline data, Populates build-cause,
	 * Populates future stages as empty, Populates can run for pipeline & each stage, Populate stage run permission
	 */
	public PipelineInstanceModels loadMinimalData(String pipelineName, Pagination pagination, Username username, OperationResult result) {
		if (!goConfigService.currentCruiseConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
			result.notFound("Not Found", "Pipeline " + pipelineName + " not found", HealthStateType.general(HealthStateScope.GLOBAL));
			return null;
		}
		if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
			result.unauthorized("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
			return null;
		}

		PipelineInstanceModels history = pipelineDao.loadHistory(pipelineName, pagination.getPageSize(), pagination.getOffset());

		for (PipelineInstanceModel pipelineInstanceModel : history) {
			populateMaterialRevisionsOnBuildCause(pipelineInstanceModel);

			populatePlaceHolderStages(pipelineInstanceModel);
			populateCanRunStatus(username, pipelineInstanceModel);
			populateStageOperatePermission(pipelineInstanceModel, username);
		}

		return history;
	}

	public PipelineStatusModel getPipelineStatus(String pipelineName, String username, OperationResult result) {
		PipelineConfig pipelineConfig = goConfigService.currentCruiseConfig().getPipelineConfigByName(new CaseInsensitiveString(pipelineName));
		if (pipelineConfig == null) {
			result.notFound("Not Found", "Pipeline not found", HealthStateType.general(HealthStateScope.GLOBAL));
			return null;
		}
		if (!securityService.hasViewPermissionForPipeline(Username.valueOf(username), pipelineName)) {
			result.unauthorized("Unauthorized", NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
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
        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        StageInstanceModel lastStage = stageHistory.last();
        StageConfig nextStage = lastStage == null ? pipelineConfig.getFirstStageConfig() : pipelineConfig.nextStage(new CaseInsensitiveString(lastStage.getName()));
        while (nextStage != null && !stageHistory.hasStage(CaseInsensitiveString.str(nextStage.name()))) {
            stageHistory.addFutureStage(CaseInsensitiveString.str(nextStage.name()), !nextStage.requiresApproval());
            nextStage = pipelineConfig.nextStage(nextStage.name());
        }
    }

    private void populateCanRunStatus(Username username, PipelineInstanceModel pipelineInstanceModel) {
        for (StageInstanceModel stageHistoryItem : pipelineInstanceModel.getStageHistory()) {
            boolean canRun = scheduleService.canRun(
                    pipelineInstanceModel.getPipelineIdentifier(), stageHistoryItem.getName(),
                    CaseInsensitiveString.str(username.getUsername()), pipelineInstanceModel.hasPreviousStageBeenScheduled(
                            stageHistoryItem.getName()));
            stageHistoryItem.setCanRun(canRun);
        }
        populatePipelineCanRunStatus(username, pipelineInstanceModel);
    }

    private void populatePipelineCanRunStatus(Username username, PipelineInstanceModel pipelineInstanceModel) {
        boolean canPipelineRun = schedulingCheckerService.canManuallyTrigger(pipelineInstanceModel.getName(), username);
        pipelineInstanceModel.setCanRun(canPipelineRun);
    }

    public PipelineInstanceModels findPipelineInstances(String pipelineName, String pipelineLabel, int count, String username) {
        PipelineInstanceModels history = pipelineDao.loadHistory(pipelineName, count, pipelineLabel);
        addPlaceholderStages(history);
        addEmptyPipelineInstanceIfNeeded(pipelineName, history, new Username(new CaseInsensitiveString(username)), goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName)), false);
        applySecurity(history, pipelineName, username);
        applyCanRun(new Username(new CaseInsensitiveString(username)), history);
        return history;
    }

    public int getPageNumberForCounter(String pipelineName, int pipelineCounter, int limit) {
        return pipelineDao.getPageNumberForCounter(pipelineName, pipelineCounter, limit);
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

    private void applyCanRun(Username username, PipelineInstanceModels history) {
        for (PipelineInstanceModel pipelineInstanceModel : history) {
            populateCanRunStatus(username, pipelineInstanceModel);
        }
    }

    private void addPlaceholderStages(PipelineInstanceModels history) {
        for (PipelineInstanceModel pipelineInstanceModel : history) {
            populatePlaceHolderStages(pipelineInstanceModel);
        }
    }

    private void applySecurity(PipelineInstanceModels history, String pipelineName, String username) {
        if (!securityService.hasViewPermissionForPipeline(new Username(new CaseInsensitiveString(username)), pipelineName)) {
            history.clear();
        }
    }

    public PipelineInstanceModels loadWithEmptyAsDefault(String pipelineName, Pagination pagination, String userName) {
        if (!securityService.hasViewPermissionForPipeline(new Username(new CaseInsensitiveString(userName)), pipelineName)) {
            return PipelineInstanceModels.createPipelineInstanceModels();
        }
        PipelineInstanceModels pipelineInstanceModels = null;
        if (triggerMonitor.isAlreadyTriggered(pipelineName)) {

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

    public PipelineInstanceModel latest(String pipelineName, Username username) {
        PipelineInstanceModels models = loadWithEmptyAsDefault(pipelineName, Pagination.ONE_ITEM, CaseInsensitiveString.str(username.getUsername()));
        return models.isEmpty() ? null : models.get(0);
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

    public PipelineInstanceModels findAllPipelineInstances(String pipelineName, Username username, HttpOperationResult result) {
        if (!validate(pipelineName, username, result)) {
            return null;
        }
        return pipelineDao.loadHistory(pipelineName);
    }

    public boolean validate(String pipelineName, Username username, OperationResult result) {
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            String pipelineNotKnown = String.format("Pipeline named [%s] is not known.", pipelineName);
            result.notFound(pipelineNotKnown, pipelineNotKnown, HealthStateType.general(HealthStateScope.GLOBAL));
            return false;
        }
        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            result.unauthorized(NOT_AUTHORIZED_TO_VIEW_PIPELINE, NOT_AUTHORIZED_TO_VIEW_PIPELINE, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return false;
        }
        return true;
    }

    public PipelineDependencyGraphOld pipelineDependencyGraph(String pipelineName, int pipelineCounter, final Username username, OperationResult result) {
        if (!validate(pipelineName, username, result)) {
            return null;
        }
        PipelineDependencyGraphOld graph = pipelineDao.pipelineGraphByNameAndCounter(pipelineName, pipelineCounter);
        if (graph == null) {
            String message = String.format("Pipeline [%s] with counter [%s] is not found", pipelineName, pipelineCounter);
            result.notFound(message, message, HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return null;
        }
        removePipelinesThatAreNotInConfig(username, graph);
        addConfiguredPipelinesThatAreNotRunYet(username, graph, goConfigService.downstreamPipelinesOf(pipelineName));
        populatePipelineState(graph.pipeline(), username);
        for (PipelineInstanceModel pipelineInstanceModel : graph.dependencies()) {
            populateDownstreamPipelineState(username, pipelineInstanceModel);
        }
        return graph;
    }

    private void populateDownstreamPipelineState(Username username, PipelineInstanceModel pipelineInstanceModel) {
        populatePlaceHolderStages(pipelineInstanceModel);
        populatePipelineCanRunStatus(username,pipelineInstanceModel);
        populateCanRunStatus(username, pipelineInstanceModel);
        populateStageOperatePermission(pipelineInstanceModel, username);
        pipelineInstanceModel.setMaterialConfigs(goConfigService.materialConfigsFor(new CaseInsensitiveString(pipelineInstanceModel.getName())));
    }

    private void removePipelinesThatAreNotInConfig(final Username username, PipelineDependencyGraphOld graph) {
        graph.filterDependencies(new PipelineDependencyGraphOld.Filterer() {
            public boolean filterPipeline(String pipelineName) {
                return goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName)) && securityService.hasViewPermissionForPipeline(username, pipelineName);
            }
        });
    }

    private void addConfiguredPipelinesThatAreNotRunYet(Username username, PipelineDependencyGraphOld graph, List<PipelineConfig> dependents) {
        for (PipelineConfig dependent : dependents) {
            if(!graph.hasDependent(CaseInsensitiveString.str(dependent.name()))){
                graph.addDependent(addEmptyPipelineInstance(CaseInsensitiveString.str(dependent.name()), username, dependent, false));
            }
        }
    }

    private void populatePipelineState(PipelineInstanceModel instance, Username username) {
        populatePlaceHolderStages(instance);
        populateCanRunStatus(username,instance);
        populateStageOperatePermission(instance,username);
        populateLockStatus(instance.getName(), username, instance);

        long id = pipelineTimeline.pipelineBefore(instance.getId());
        if (id != -1) {
            PipelineInstanceModel prevPipeline = pipelineDao.loadHistory(id);
            instance.setPreviousPipelineLabel(prevPipeline.getLabel());
            instance.setPreviousPipelineCounter(prevPipeline.getCounter());
        }
    }

    public PipelineInstanceModel findPipelineInstance(String pipelineName, int pipelineCounter, Username username, OperationResult result) {
        return decoratePIM(pipelineName, pipelineCounter, username, result, pipelineDao.findPipelineHistoryByNameAndCounter(pipelineName, pipelineCounter));
    }

    public PipelineInstanceModel findPipelineInstance(String pipelineName, int pipelineCounter, long id, Username username, OperationResult result) {
        return decoratePIM(pipelineName, pipelineCounter, username, result, pipelineDao.loadHistoryByIdWithBuildCause(id));
    }

    private PipelineInstanceModel decoratePIM(String pipelineName, int pipelineCounter, Username username, OperationResult result, PipelineInstanceModel pipelineInstanceModel) {
        if (!validate(pipelineName, username, result)) {
            return null;
        }
        if (pipelineInstanceModel == null && pipelineCounter == 0){
            pipelineInstanceModel = PipelineInstanceModel.createEmptyPipelineInstanceModel(pipelineName, BuildCause.createWithEmptyModifications(), new StageInstanceModels());
        }
        if (pipelineInstanceModel == null) {
            String pipelineInstanceNotFound = String.format("Pipeline [%s/%s] not found.", pipelineName, pipelineCounter);
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
        pipelineInstanceModel.setCurrentlyLocked(pipelineLockService.lockedPipeline(pipelineName) != null);
    }

    private void populateStageOperatePermission(PipelineInstanceModel pipelineInstanceModel, Username username) {
        for (StageInstanceModel stage : pipelineInstanceModel.getStageHistory()) {
            stage.setOperatePermission(securityService.hasOperatePermissionForStage(pipelineInstanceModel.getName(), stage.getName(), CaseInsensitiveString.str(username.getUsername())));
        }
    }

    public List<PipelineGroupModel> allActivePipelineInstances(Username username, PipelineSelections pipelineSelections) {
        PipelineGroupModels groupModels = allPipelineInstances(username);
        filterSelections(groupModels,pipelineSelections);
        removeEmptyGroups(groupModels);
        updateGroupAdministrability(username, groupModels);
        return groupModels.asList();
    }

    public List<PipelineGroupModel> getActivePipelineInstance(Username username, String pipeline) {
        PipelineGroupModels models = allPipelineInstances(username);
        filterSelections(models, PipelineSelections.singleSelection(pipeline));
        removeEmptyGroups(models);
        return models.asList();
    }

    private PipelineGroupModels allPipelineInstances(Username username) {
        CruiseConfig currentConfig = goConfigService.currentCruiseConfig();
        PipelineGroups groups = currentConfig.getGroups();
        PipelineInstanceModels activePipelines = filterPermissions(pipelineDao.loadActivePipelines(), username);

        PipelineGroupModels groupModels = new PipelineGroupModels();
        for (PipelineConfig pipelineConfig : currentConfig.getAllPipelineConfigs()) {
            CaseInsensitiveString pipelineName = pipelineConfig.name();
            for (PipelineInstanceModel activePipeline : activePipelines.findAll(CaseInsensitiveString.str(pipelineName))) {
                activePipeline.setTrackingTool(pipelineConfig.getTrackingTool());
                activePipeline.setMingleConfig(pipelineConfig.getMingleConfig());
                populatePlaceHolderStages(activePipeline);

                String groupName = groups.findGroupNameByPipeline(pipelineName);
                if (groupName == null) {
                    throw new RuntimeException("Unable to find group find pipeline " + pipelineName);
                }
                populatePreviousStageState(activePipeline);
                populateLockStatus(activePipeline.getName(), username, activePipeline);
                boolean canForce = schedulingCheckerService.canManuallyTrigger(CaseInsensitiveString.str(pipelineName), username);
                PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(CaseInsensitiveString.str(pipelineName));
                groupModels.addPipelineInstance(groupName, activePipeline, canForce, securityService.hasOperatePermissionForPipeline(
                        username.getUsername(), CaseInsensitiveString.str(pipelineName)
                ), pauseInfo);
            }
        }

        for (PipelineConfigs group : groups) {
            populateMissingPipelines(username, groupModels, group);
        }
        return groupModels;
    }

    private void filterSelections(PipelineGroupModels groupModels, PipelineSelections pipelineSelections) {
        for (PipelineGroupModel groupModel : groupModels.asList()) {
            for (PipelineModel pipelineModel : groupModel.getPipelineModels()) {
                if(!pipelineSelections.includesPipeline(pipelineModel.getName())){
                    groupModels.removePipeline(groupModel,pipelineModel);
                }
            }
        }
    }

    private void removeEmptyGroups(PipelineGroupModels groupModels) {
        for (PipelineGroupModel pipelineGroupModel : groupModels.asList()) {
            if(pipelineGroupModel.getPipelineModels().isEmpty()){
                groupModels.remove(pipelineGroupModel);
            }
        }
    }

    private void updateGroupAdministrability(Username username, PipelineGroupModels groupModels) {
        for (PipelineGroupModel groupModel : groupModels.asList()) {
            if (!goConfigService.isUserAdminOfGroup(username.getUsername(), groupModel.getName())) {
                continue;
            }

            for (PipelineModel pipelineModel : groupModel.getPipelineModels()) {
                if(goConfigService.isPipelineEditableViaUI(pipelineModel.getName()))
                    pipelineModel.updateAdministrability(true);
            }
        }
    }

    private void populatePreviousStageState(PipelineInstanceModel activePipeline) {
        if (activePipeline.isAnyStageActive()) {
            StageInstanceModel activeStage = activePipeline.activeStage();
            StageInstanceModel latestActive = null;
            long id = activePipeline.getId();
            do {
                PipelineTimelineEntry timelineEntry = pipelineTimeline.runBefore(id, new CaseInsensitiveString(activePipeline.getName()));
                if (timelineEntry == null) {
                    break;
                }
                id = timelineEntry.getId();
                PipelineInstanceModel instanceModel = pipelineDao.loadHistory(id);
                if (instanceModel != null && instanceModel.hasStageBeenRun(activeStage.getName())) {
                    latestActive = instanceModel.getStageHistory().byName(activeStage.getName());
                }
            } while (latestActive == null);
            activeStage.setPreviousStage(latestActive);
        }
    }

    private void populateMissingPipelines(Username username, PipelineGroupModels groupModels, PipelineConfigs group) {
        String groupName = group.getGroup();
        for (PipelineConfig pipelineConfig : group) {
            if (!groupModels.containsPipeline(groupName, CaseInsensitiveString.str(pipelineConfig.name()))) {
                PipelineModel latestPipeline = latestPipelineModel(username, CaseInsensitiveString.str(pipelineConfig.name()));
                if (latestPipeline != null) {
                    groupModels.addPipelineInstance(groupName, latestPipeline.getLatestPipelineInstance(), latestPipeline.canForce(), latestPipeline.canOperate(), latestPipeline.getPausedInfo());
                }
            }
        }
    }

    private PipelineInstanceModels filterPermissions(PipelineInstanceModels pipelineInstanceModels, Username username) {
        PipelineInstanceModels newModels = PipelineInstanceModels.createPipelineInstanceModels();
        for (PipelineInstanceModel pipelineInstanceModel : pipelineInstanceModels) {
            if (securityService.hasViewPermissionForPipeline(username, pipelineInstanceModel.getName())) {
                newModels.add(pipelineInstanceModel);
            }
        }
        return newModels;
    }

    public PipelineModel latestPipelineModel(Username username, String pipelineName) {
        PipelineInstanceModel instanceModel = latest(pipelineName, username);
        if (instanceModel != null) {
            boolean canForce = schedulingCheckerService.canManuallyTrigger(pipelineName, username);
            PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(pipelineName);
            PipelineModel pipelineModel = new PipelineModel(pipelineName, canForce, securityService.hasOperatePermissionForPipeline(
                    username.getUsername(), pipelineName
            ), pauseInfo);
            populateLockStatus(instanceModel.getName(), username, instanceModel);
            pipelineModel.addPipelineInstance(instanceModel);
            String groupName = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
            if(goConfigService.isPipelineEditableViaUI(pipelineName))
                pipelineModel.updateAdministrability(goConfigService.isUserAdminOfGroup(username.getUsername(), groupName));
            else
                pipelineModel.updateAdministrability(false);
            return pipelineModel;
        }
        return null;
    }

    public PipelineInstanceModels findPipelineInstancesByPageNumber(String pipelineName, int pageNumber, int limit, String userName) {
        Pagination pagination = Pagination.pageByNumber(pageNumber, totalCount(pipelineName), limit);
        PipelineInstanceModels instanceModels = load(pipelineName, pagination, userName, false);
        instanceModels.setPagination(pagination);
        return instanceModels;
    }

    public PipelineInstanceModels findMatchingPipelineInstances(String pipelineName, String pattern, int limit, Username userName, HttpLocalizedOperationResult result) {
        pattern = escapeWildCardsAndTrim(pattern.trim());
        if (!securityService.hasViewPermissionForPipeline(userName, pipelineName)) {
            result.unauthorized(LocalizedMessage.cannotViewPipeline(pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
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
        pattern = pattern.replace("%", "\\%" ).replace("_", "\\_");
        return pattern;
    }

    private int limitForPipeline(String pipelineName, int limit) {
        return goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName)).size() * limit;
    }

    private void populateMaterialRevisionsOnBuildCause(PipelineInstanceModel model) {
        model.setMaterialRevisionsOnBuildCause(materialRepository.findMaterialRevisionsForPipeline(model.getId()));
    }

    public void updateComment(String pipelineName, int pipelineCounter, String comment, Username username, HttpLocalizedOperationResult result) {
        if (!Toggles.isToggleOn(Toggles.PIPELINE_COMMENT_FEATURE_TOGGLE_KEY)) {
            result.notImplemented(LocalizedMessage.string("FEATURE_NOT_AVAILABLE", "Pipeline Comment"));
            return;
        }

        if (securityService.hasOperatePermissionForPipeline(username.getUsername(), pipelineName)) {
            pipelineDao.updateComment(pipelineName, pipelineCounter, comment);
        } else {
            result.unauthorized(LocalizedMessage.cannotOperatePipeline(pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
        }
    }

    private static class PipelineGroupModels {
        List<PipelineGroupModel> groupModels = new ArrayList<>();

        public void addPipelineInstance(String groupName, PipelineInstanceModel pipelineInstanceModel, boolean canForce, boolean canOperate, PipelinePauseInfo pipelinePauseInfo) {
            PipelineModel pipelineModel = pipelineModelForPipelineName(groupName, pipelineInstanceModel.getName(), canForce, canOperate, pipelinePauseInfo);
            pipelineModel.addPipelineInstance(pipelineInstanceModel);
        }

        public boolean containsPipeline(String groupName, String pipelineName) {
            return get(groupName).containsPipeline(pipelineName);
        }

        public List<PipelineGroupModel> asList() {
            return new ArrayList<>(groupModels);
        }

        private PipelineModel pipelineModelForPipelineName(String groupName, String pipelineName, boolean canForce, boolean canOperate, PipelinePauseInfo pipelinePauseInfo){
            PipelineGroupModel group = get(groupName);
            return group.pipelineModelForPipelineName(pipelineName, canForce, canOperate, pipelinePauseInfo);
        }

        private PipelineGroupModel get(String groupName) {
            for (PipelineGroupModel groupModel : groupModels) {
                if (groupModel.getName().equals(groupName)) {
                    return groupModel;
                }
            }
            PipelineGroupModel newGroupModel = new PipelineGroupModel(groupName);
            groupModels.add(newGroupModel);
            return newGroupModel;
        }

        public void removePipeline(PipelineGroupModel groupModel, PipelineModel pipelineModel) {
            groupModel.remove(pipelineModel);
            if(groupModel.getPipelineModels().isEmpty()){
                groupModels.remove(groupModel);
            }
        }

        public void remove(PipelineGroupModel pipelineGroupModel) {
            groupModels.remove(pipelineGroupModel);
        }
    }
}
