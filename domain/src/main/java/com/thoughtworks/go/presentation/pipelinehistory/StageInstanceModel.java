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

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.server.presentation.models.StageConfigurationModel;

import java.util.Date;

import static com.thoughtworks.go.util.GoConstants.APPROVAL_SUCCESS;

public class StageInstanceModel implements StageConfigurationModel {
    private String name;
    private long id;
    private JobHistory jobHistory;
    private boolean canRun;
    private boolean scheduled = true; // true if this stage history really happened
    private String approvalType;
    private String approvedBy;
    private String cancelledBy;
    private String counter;
    private boolean operatePermission;
    private StageInstanceModel previousStage;
    private StageResult result;
    private StageIdentifier identifier;
    private Integer rerunOfCounter;
    private String errorMessage;

    public boolean hasOperatePermission() {
        return operatePermission;
    }

    public void setOperatePermission(boolean operatePermission) {
        this.operatePermission = operatePermission;
    }

    public boolean isSelected() {
        return selected;
    }

    private boolean selected;

    // for test
    public StageInstanceModel(String name, String counter, JobHistory jobHistory) {
        this.name = name;
        this.jobHistory = jobHistory;
        this.counter = counter;
    }

    // for test
    public StageInstanceModel(String name, String counter, JobHistory jobHistory, StageIdentifier identifier) {
        this(name, counter, jobHistory);
        this.identifier = identifier;
    }


    public StageInstanceModel(String name, String counter, StageResult result, StageIdentifier identifier) {
        this(name, counter, new JobHistory(), identifier);
        this.result = result;
    }

    // for ibatis
    public StageInstanceModel() {
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public JobHistory getBuildHistory() {
        return jobHistory;
    }

    public void setBuildHistory(JobHistory jobHistory) {
        this.jobHistory = jobHistory;
    }

    public StageState getState() {
        return StageState.findByBuilds(jobHistory);
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getApprovalDescription() {
        if (approvedBy == null) {
            return "Awaiting Approval";
        }
        return "Approved by " + approvedBy;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public String getApprovalTypeDescription() {
        if (isAutoApproved()) {
            return "auto";
        }
        else {
            return "manual";
        }
    }

    public boolean needsApproval() {
        return approvedBy == null && getState().completed();
    }

    @Override
    public boolean isAutoApproved() {
        return APPROVAL_SUCCESS.equals(approvalType);
    }

    public Date getScheduledDate() {
        if(jobHistory.isEmpty()) {
            return null;
        }
        return jobHistory.getScheduledDate();
    }

    public boolean getCanRun() {
        return this.canRun;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean getCanReRun() {
        return canRun;
    }

    public boolean getCanCancel() {
        return operatePermission && getState().isActive();
    }

    public void setCanRun(boolean canRun) {
        this.canRun = canRun;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean value) {
        this.scheduled = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageInstanceModel that = (StageInstanceModel) o;

        if (!name.equals(that.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }



    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public String getCounter() {
        return counter;
    }

    public void setCounter(String counter) {
        this.counter = counter;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getCanApprove() {
        return (getCanRun() && !isScheduled());
    }

    public boolean hasUnsuccessfullyCompleted() {
        for (JobHistoryItem jobHistoryItem : jobHistory) {
            if(jobHistoryItem.hasUnsuccessfullyCompleted()) return true;
        }
        return false;
    }

    public boolean hasPassed() {
        for (JobHistoryItem jobHistoryItem : jobHistory) {
            if(!jobHistoryItem.hasPassed()) return false;
        }
        return true;
    }

    public boolean hasFailed() {
        for (JobHistoryItem jobHistoryItem : jobHistory) {
            if(jobHistoryItem.hasFailed()) return true;
        }
        return false;
    }

    public boolean isRunning() {
        for (JobHistoryItem jobHistoryItem : jobHistory) {
            if(jobHistoryItem.isRunning()) return true;
        }
        return false;
    }

    public boolean hasPreviousStage() {
        return this.previousStage != null;
    }

    public void setPreviousStage(StageInstanceModel previousStage) {
        this.previousStage = previousStage;
    }

    public StageInstanceModel getPreviousStage() {
        return previousStage;
    }

    public StageResult getResult() {
        return result;
    }

    public StageIdentifier getIdentifier() {
        return identifier;
    }

	public String getPipelineName() {
		return identifier.getPipelineName();
	}

	public Integer getPipelineCounter() {
		return identifier.getPipelineCounter();
	}

    public String locator() {
        return identifier.getStageLocator();
    }

    public boolean isRerunJobs() {
        return rerunOfCounter != null;
    }

    public Integer getRerunOfCounter() {
        return rerunOfCounter;
    }

    public void setRerunOfCounter(Integer rerunOfCounter) {
        this.rerunOfCounter = rerunOfCounter;
    }
}
