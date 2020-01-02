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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.util.TimeConverter;

import java.util.Date;

public class PipelineInstanceModel implements PipelineInfo {
    private long id;
    private String name;
    private String label;
    private BuildCause buildCause;
    private StageInstanceModels stageHistory;
    protected boolean canRun;
    private Integer counter;
    private MaterialRevisions latest = new MaterialRevisions();
    private MaterialConfigs materialConfigs = new MaterialConfigs();
    protected boolean isPreparingToSchedule;
    private PipelineTimelineEntry pipelineAfter;
    private PipelineTimelineEntry pipelineBefore;
    private boolean canUnlock;
    private boolean lockable;
    private double naturalOrder;
    private int previousPipelineCounter;
    private String previousPipelineLabel;
    private boolean isCurrentlyLocked;
    private TrackingTool trackingTool;
    private String comment;

    private PipelineInstanceModel() {
        stageHistory = new StageInstanceModels();
    }

    public PipelineInstanceModel(String name, Integer counter, String label, BuildCause buildCause, StageInstanceModels stageHistory) {
        this.name = name;
        this.counter = counter;
        this.label = label;
        this.buildCause = buildCause;
        this.stageHistory = stageHistory;
    }

    public boolean hasHistoricalData() {
        return true;
    }

    public BuildCause getBuildCause() {
        return buildCause;
    }

    public void setBuildCause(BuildCause buildCause) {
        this.buildCause = buildCause;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getUri() {
        return "/" + name + "/" + label;
    }

    public String getBuildCauseMessage() {
        return buildCause == null ? "Unknown" : buildCause.getBuildCauseMessage();
    }

    @Override
    public boolean hasStageBeenRun(String stageName) {
        return stageHistory.hasStage(stageName);
    }

    @Override
    public String nextStageName(String stageName) {
        return stageHistory.nextStageName(stageName);
    }

    public void setName(String name) {
        this.name = name;
    }

    public StageInstanceModels getStageHistory() {
        return stageHistory;
    }

    public String getBuildCauseBy() {
        return buildCause.getBuildCauseMessage();
    }

    public void setStageHistory(StageInstanceModels stageHistory) {
        this.stageHistory = stageHistory;
    }

    public Date getScheduledDate() {
        return stageHistory == null ? null : stageHistory.getScheduledDate();
    }

    public TimeConverter.ConvertedTime getCreatedTimeForDisplay() {
        return TimeConverter.convertHandleNull(getScheduledDate());
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean isAnyStageActive() {
        for (StageInstanceModel stage : stageHistory) {
            if (stage.getState().isActive()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPreviousStageBeenScheduled(String stageName) {
        StageInstanceModel stage = stageHistory.byName(stageName);
        int index = stageHistory.indexOf(stage);
        if (index == 0) {
            return true;
        }
        if (index > 0) {
            return stageHistory.get(index - 1).isScheduled();
        }
        return false;
    }

    public void selectStage(String selectedStageName) {
        StageInstanceModel stage = stageHistory.byName(selectedStageName);
        if (stage != null && stage.isScheduled()) {
            stage.setSelected(true);
        }
    }

    public boolean isScheduled() {
        for (StageInstanceModel stageInstanceModel : stageHistory) {
            if (stageInstanceModel.isScheduled()) { return true; }
        }
        return false;
    }

    public void setCanRun(boolean canRun) {
        this.canRun = canRun;
    }

    public boolean getCanRun() {
        return canRun;
    }

    public String getRevisionOfLatestModification() {
        return buildCause.getMaterialRevisions().latestRevision();
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public PipelineIdentifier getPipelineIdentifier() {
        return new PipelineIdentifier(name, counter, label);
    }

    public void setMaterialRevisionsOnBuildCause(MaterialRevisions materialRevisions) {
        buildCause.setMaterialRevisions(materialRevisions);
    }

    public MaterialRevisions getCurrentRevisions() {
        return buildCause.getMaterialRevisions();
    }

    public MaterialRevisions getLatestRevisions() {
        return latest;
    }

    public MaterialConfigs getMaterials() {
        return materialConfigs;
    }

    public Revision getLatestRevision(MaterialConfig materialConfig) {
        return revisionFor(getLatestMaterialRevision(materialConfig));
    }

    private Revision revisionFor(MaterialRevision materialRevision) {
        return materialRevision.hasModifications() ? materialRevision.getRevision() : UNKNOWN_REVISION;
    }

    public MaterialRevision getLatestMaterialRevision(MaterialConfig materialConfig) {
       return findMaterialRevisionOf(materialConfig, latest);
    }

    public Revision getCurrentRevision(MaterialConfig materialConfig) {
        return revisionFor(findMaterialRevisionOf(materialConfig, buildCause.getMaterialRevisions()));
    }

    private MaterialRevision findMaterialRevisionOf(MaterialConfig materialConfig, MaterialRevisions materialRevisions) {
        for (MaterialRevision materialRevision : materialRevisions) {
            if (materialRevision.getMaterial().hasSameFingerprint(materialConfig)) {
                return materialRevision;
            }
        }
        return new NullMaterialRevision();
    }

    public MaterialRevision findCurrentMaterialRevisionForUI(MaterialConfig materialConfig) {
        MaterialRevision materialRevision = findCurrentMaterialRevisionUsingPipelineUniqueFingerprint(materialConfig);
        if (materialRevision == null) {
            materialRevision = findCurrentMaterialRevisionUsingFingerprint(materialConfig);
        }
        return materialRevision;
    }

    private MaterialRevision findCurrentMaterialRevisionUsingFingerprint(MaterialConfig materialConfig) {
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            if (materialRevision.getMaterial().hasSameFingerprint(materialConfig)) {
                return materialRevision;
            }
        }
        return null;
    }

    private MaterialRevision findCurrentMaterialRevisionUsingPipelineUniqueFingerprint(MaterialConfig materialConfig) {
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            if (materialRevision.getMaterial().getPipelineUniqueFingerprint().equals(materialConfig.getPipelineUniqueFingerprint())) {
                return materialRevision;
            }
        }
        return null;
    }

    public void setLatestRevisions(MaterialRevisions latest) {
        this.latest = latest;
    }

    public void setMaterialConfigs(MaterialConfigs materialConfigs) {
        this.materialConfigs = materialConfigs;
    }

    public boolean hasModificationsFor(MaterialConfig materialConfig) {
        return getLatestRevision(materialConfig).isRealRevision();
    }

    public Revision getCurrentRevision(String requestedMaterialName) {
        for (MaterialRevision materialRevision : getCurrentRevisions()) {
            String materialName = CaseInsensitiveString.str(materialRevision.getMaterial().getName());
            if(materialName != null && materialName.equals(requestedMaterialName)) {
                return materialRevision.getRevision();
            }
        }
        throw new RuntimeException("material not known for pipeline " + getName());
    }

    public String getApprovedBy() {
        return getStageHistory().first().getApprovedBy();
    }

    public Boolean isLatestStageUnsuccessful() {
        return stageHistory.isLatestStageUnsuccessful();
    }

    public Boolean isLatestStageSuccessful() {
        return stageHistory.isLatestStageSuccessful();
    }

    public StageInstanceModel latestStage() {
        return stageHistory.latestStage();
    }

    public int indexOf(StageInstanceModel stageInstanceModel) {
        return stageHistory.indexOf(stageInstanceModel);
    }

    public int numberOfStages() {
        return stageHistory.size();
    }

    public Boolean isRunning() {
        for (StageInstanceModel model : stageHistory) {
            if (model instanceof NullStageHistoryItem || model.isRunning()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNeverCheckedForRevisions() {
        return latest.isEmpty();
    }

    public boolean hasNewRevisions() {
        for (MaterialConfig materialConfig : materialConfigs) {
            if (hasNewRevisions(materialConfig)) { return true; }
        }
        return false;
    }

    public boolean hasNewRevisions(MaterialConfig materialConfig) {
        Revision currentRevision = getCurrentRevision(materialConfig);
        Revision revision = getLatestRevision(materialConfig);
        return !currentRevision.equals(revision);
    }

    public boolean isPreparingToSchedule() {
        return isPreparingToSchedule;
    }

    public String getPipelineStatusMessage() {
        StageInstanceModel latestStage = stageHistory.latestStage();
        return String.format("%s: %s", latestStage.getState(), latestStage.getName());
    }

    /**
     * @depracated use the other construction methods
     */
    public static PipelineInstanceModel createEmptyModel() {
        return new PipelineInstanceModel();
    }

    public static PipelineInstanceModel createPreparingToSchedule(String name, StageInstanceModels stages) {
        return new PreparingToScheduleInstance(name, stages);
    }

    public static PipelineInstanceModel createPipeline(String name, Integer counter, String label, BuildCause buildCause, StageInstanceModels stageHistory) {
        return new PipelineInstanceModel(name, counter, label, buildCause, stageHistory);
    }

    public static EmptyPipelineInstanceModel createEmptyPipelineInstanceModel(String pipelineName, BuildCause withEmptyModifications, StageInstanceModels stageHistory) {
        return new EmptyPipelineInstanceModel(pipelineName, withEmptyModifications, stageHistory);
    }

    public boolean isLatestStage(StageInstanceModel stage) {
        return stageHistory.isLatestStage(stage);
    }

    public PipelineTimelineEntry getPipelineBefore() {
        return pipelineBefore;
    }

    public PipelineTimelineEntry getPipelineAfter() {
        return pipelineAfter;
    }

    public void setPipelineAfter(PipelineTimelineEntry pipelineAfter) {
        this.pipelineAfter = pipelineAfter;
    }

    public void setPipelineBefore(PipelineTimelineEntry pipelineBefore) {
        this.pipelineBefore = pipelineBefore;
    }

    public boolean canUnlock() {
        return canUnlock;
    }

    public void setCanUnlock(boolean canUnlock) {
        this.canUnlock = canUnlock;
    }

    public boolean isLockable() {
        return lockable;
    }

    public void setIsLockable(boolean isLockable) {
        this.lockable = isLockable;
    }

    public StageInstanceModel activeStage() {
        for (StageInstanceModel stageInstanceModel : stageHistory) {
            if (stageInstanceModel.getState().isActive()) return stageInstanceModel;
        }
        return null;
    }

    public void setNaturalOrder(double naturalOrder) {
        this.naturalOrder = naturalOrder;
    }

    public double getNaturalOrder() {
        return naturalOrder;
    }

    public static final Revision UNKNOWN_REVISION = new Revision() {
        public static final String NO_HISTORICAL_DATA = "No historical data";

        @Override
        public String getRevision() {
            return NO_HISTORICAL_DATA;
        }

        @Override
        public String getRevisionUrl() {
            return NO_HISTORICAL_DATA;
        }

        @Override
        public boolean isRealRevision() {
            return false;
        }
    };

    public StageInstanceModel stage(String stageName) {
        return getStageHistory().byName(stageName);
    }

    public String getPreviousLabel() {
        return previousPipelineLabel;
    }

    public int getPreviousCounter() {
        return previousPipelineCounter;
    }

    public void setPreviousPipelineCounter(int counter) {
        this.previousPipelineCounter = counter;
    }

    public void setPreviousPipelineLabel(String label) {
        this.previousPipelineLabel = label;
    }

    public boolean hasStage(StageIdentifier identifier) {
        for (StageInstanceModel instanceModel : stageHistory) {
            if(identifier.equals(instanceModel.getIdentifier())) return true;
        }
        return false;
    }

    public String getApprovedByForDisplay() {
        return "Triggered by " + getApprovedBy();
    }

    public boolean isCurrentlyLocked() {
        return isCurrentlyLocked;
    }

    public void setCurrentlyLocked(boolean isCurrentlyLocked) {
        this.isCurrentlyLocked = isCurrentlyLocked;
    }

    public boolean isBisect() {
        double naturalOrder = this.naturalOrder;
        return isBisect(naturalOrder);
    }

    public static boolean isBisect(double naturalOrder) {
        return naturalOrder - new Double(naturalOrder).intValue() > 0;//TODO: may be we should be using long, as int can lead to truncation
    }

    public TrackingTool getTrackingTool() {
        return trackingTool;
    }

    public void setTrackingTool(TrackingTool trackingTool) {
        this.trackingTool = trackingTool;
    }

    public DependencyMaterialConfig findDependencyMaterial(CaseInsensitiveString pipelineName) {
        return getMaterials().findDependencyMaterial(pipelineName);
    }

    public void setComment(String comment) { this.comment = comment; }

    public String getComment() {
        return comment;
    }
}
