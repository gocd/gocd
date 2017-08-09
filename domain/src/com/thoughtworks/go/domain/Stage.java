/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.server.domain.StageStatusHandler;
import com.thoughtworks.go.util.Clock;
import org.slf4j.Logger;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Date;

public class Stage extends PersistentObject {
    private static final Logger LOG = LoggerFactory.getLogger(Stage.class);

    private Long pipelineId;
    private String name;
    private JobInstances jobInstances = new JobInstances();
    private String approvedBy;
    private int orderId;
    private Timestamp createdTime;
    private Timestamp lastTransitionedTime;
    private String approvalType;
    private boolean fetchMaterials = StageConfig.DEFAULT_FETCH_MATERIALS;
    private StageResult result = DEFAULT_RESULT;
    private int counter;
    private StageIdentifier identifier;
    private Long completedByTransitionId;
    private StageState state;
    private boolean latestRun = true;
    private boolean cleanWorkingDir = StageConfig.DEFAULT_CLEAN_WORKING_DIR;
    private Integer rerunOfCounter;
    private boolean artifactsDeleted;

    private static final StageResult DEFAULT_RESULT = StageResult.Unknown;
    private static final Cloner CLONER = new Cloner();
    private String configVersion = null;

    public Stage() {
    }

    public Stage(String name, JobInstances jobInstances, String approvedBy, String approvalType, final Clock clock) {
        this(name, jobInstances, approvedBy, approvalType, StageConfig.DEFAULT_FETCH_MATERIALS, StageConfig.DEFAULT_CLEAN_WORKING_DIR, clock);
    }

    public Stage(String name, JobInstances jobInstances, String approvedBy, String approvalType, boolean fetchMaterials, boolean cleanWorkingDir, final Clock clock) {
        this.name = name;
        this.jobInstances = jobInstances;
        this.approvedBy = approvedBy;
        this.approvalType = approvalType;
        this.fetchMaterials = fetchMaterials;
        this.cleanWorkingDir = cleanWorkingDir;
        this.createdTime = new Timestamp(clock.currentTimeMillis());
    }

    public Stage(String name, JobInstances jobInstances, String approvedBy, String approvalType, boolean fetchMaterials, boolean cleanWorkingDir, String configVersion, final Clock clock) {
        this(name, jobInstances, approvedBy, approvalType, fetchMaterials, cleanWorkingDir, clock);
        this.configVersion = configVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JobInstances getJobInstances() {
        return jobInstances;
    }

    public void setJobInstances(JobInstances jobInstances) {
        this.jobInstances = jobInstances;
    }

    public Long getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(Long pipelineId) {
        this.pipelineId = pipelineId;
    }

    public Stage mostRecent(Stage completed) {
        return this;
    }

    public StageState stageState() {
        return jobInstances.stageState();
    }

    public StageState getState() {
        return state;
    }

    public String status() {
        return stageState().status();
    }

    public Date scheduledDate() {
        return new Date(createdTime.getTime());
    }

    public Date completedDate() {
        if (completedByTransitionId != null && lastTransitionedTime != null) {
            return new Date(lastTransitionedTime.getTime());
        } else if (lastTransitionedTime == null) {//TODO: Should not be null anymore. Keeping this for Legacy code so that we dont end up fixing a lot of tests. We should remove this eventually.
            Date mostRecent = null;
            for (JobStateTransition transition : jobInstances.stateTransitions()) {
                if (transition.getCurrentState() == JobState.Completed && (mostRecent == null || transition.getStateChangeTime().after(mostRecent))) {
                    mostRecent = transition.getStateChangeTime();
                }
            }
            return mostRecent;
        }
        return null;
    }

    public Date latestTransitionDate() {
        Date latest = jobInstances.latestTransitionDate();
        if (latest == null) {
            return createdTime;
        }
        return latest;
    }

    public boolean isCompletedAndPassed() {
        return this.stageState().completed() && passed();
    }

    public boolean passed() {
        return this.stageState().equals(StageState.Passed);
    }

    public boolean failed() {
        return this.stageState().equals(StageState.Failed);
    }

    public boolean isActive() {
        return this.stageState().isActive();
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public boolean isApproved() {
        return approvedBy != null;
    }

    public void fail() {
        for (JobInstance job : getJobInstances()) {
            job.fail();
        }
    }

    public Timestamp getLastTransitionedTime() {
        return lastTransitionedTime;
    }

    public void setLastTransitionedTime(Timestamp lastTransitionedTime) {
        this.lastTransitionedTime = lastTransitionedTime;
    }

    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String toString() {
        return "<Stage"
                + " id='" + id + "'"
                + " name='" + name + "'"
                + " stageCounter='" + counter + "'"
                + " createdTime='" + createdTime + "'"
                + " result='" + result + "'"
                + ">";
    }

    public StageResult getResult() {
        return result;
    }

    public synchronized void calculateResult() {
        this.state = this.stageState();
        this.result = state.stageResult();
        if (state.completed()) {
            long latestTransitionId = jobInstances.latestTransitionId();
            if (latestTransitionId != JobStateTransition.NOT_PERSISTED) {
                LOG.info("Stage is being completed by transition id: {}", latestTransitionId);
                if (completedByTransitionId != null) {
                    LOG.warn("Completing transition id for stage is being changed from {} to {}", completedByTransitionId, latestTransitionId);
                }
                this.completedByTransitionId = latestTransitionId;
            }
        }
    }

    public String getApprovalType() {
        return approvalType;
    }

    public void setCounter(int counter) {
        this.counter = counter;
        if (getIdentifier() != null) {
            getIdentifier().setStageCounter(String.valueOf(counter));
        }
    }

    public int getCounter() {
        return this.counter;
    }

    public void setIdentifier(StageIdentifier identifier) {
        this.identifier = identifier;
    }

    public StageIdentifier getIdentifier() {
        return identifier;
    }

    public JobInstance findJob(String name) {
        return jobInstances.getByName(name);
    }

    // This is to differentiate b/w scheduled & building
    public boolean isScheduled() {
        if (counter == 1) {
            return areAllJobsInScheduledState();
        }
        return false;
    }

    // This is to differentiate b/w re-run & building
    public boolean isReRun() {
        if (counter > 1) {
            // stage re-run || job re-run
            return areAllJobsInScheduledState() || (rerunOfCounter != null && areAllReRunJobsInScheduledState());
        }
        return false;
    }

    private boolean areAllJobsInScheduledState() {
        for (JobInstance instance : jobInstances) {
            if (instance.getState() != JobState.Scheduled) {
                return false;
            }
        }
        return true;
    }

    private boolean areAllReRunJobsInScheduledState() {
        for (JobInstance instance : jobInstances) {
            if (instance.isRerun() && instance.getState() != JobState.Scheduled) {
                return false;
            }
        }
        return true;
    }

    public boolean isCompleted() {
        return stageState().completed();
    }

    public void statusHandling(StageStatusHandler stageStatusHandler) {
        if (this.stageState().completedNormally()) {
            stageStatusHandler.onNormalCompletion(stageState(), result);
        }
    }

    public RunDuration getDuration() {
        if (!state.completed()) {
            return RunDuration.IN_PROGRESS_DURATION;
        }
        Date start = new Date(createdTime.getTime());
        Date end = new Date(lastTransitionedTime.getTime());
        return new RunDuration.ActualDuration(new Duration(end.getTime() - start.getTime()));
    }

    private Date latest(JobState stage) {
        Date date = null;
        for (JobStateTransition transition : jobInstances.stateTransitions()) {
            if (transition.getCurrentState().equals(stage)) {
                Date time = transition.getStateChangeTime();
                if (date == null || time.after(date)) {
                    date = time;
                }
            }
        }
        return date;
    }

    private Date earliest(JobState stage) {
        Date date = null;
        for (JobStateTransition transition : jobInstances.stateTransitions()) {
            if (transition.getCurrentState().equals(stage)) {
                Date time = transition.getStateChangeTime();
                if (date == null || time.before(date)) {
                    date = time;
                }
            }
        }
        return date;
    }

    public String stageLocator() {
        return identifier.stageLocator();
    }

    public String stageLocatorForDisplay() {
        return identifier.stageLocatorForDisplay();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Stage stage = (Stage) o;

        if (counter != stage.counter) {
            return false;
        }
        if (orderId != stage.orderId) {
            return false;
        }
        if (fetchMaterials != stage.fetchMaterials) {
            return false;
        }

        if (cleanWorkingDir != stage.cleanWorkingDir) {
            return false;
        }
        if (pipelineId != null ? !pipelineId.equals(stage.pipelineId) : stage.pipelineId != null) {
            return false;
        }
        if (approvalType != null ? !approvalType.equals(stage.approvalType) : stage.approvalType != null) {
            return false;
        }
        if (approvedBy != null ? !approvedBy.equals(stage.approvedBy) : stage.approvedBy != null) {
            return false;
        }
        if (createdTime != null ? !createdTime.equals(stage.createdTime) : stage.createdTime != null) {
            return false;
        }
//TODO: ChrisS We need to fix this once all the tests are saving JobInstances correctly.
//see PipelineDao.saveWithStages to see why we can't do this
//        if (jobInstances != null ? !jobInstances.equals(stage.jobInstances) : stage.jobInstances != null) {
//            return false;
//        }
        if (name != null ? !name.equals(stage.name) : stage.name != null) {
            return false;
        }
        if (result != stage.result) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result1 = pipelineId != null ? pipelineId.hashCode() : 0;
        result1 = 31 * result1 + (name != null ? name.hashCode() : 0);
        result1 = 31 * result1 + (jobInstances != null ? jobInstances.hashCode() : 0);
        result1 = 31 * result1 + (approvedBy != null ? approvedBy.hashCode() : 0);
        result1 = 31 * result1 + orderId;
        result1 = 31 * result1 + (createdTime != null ? createdTime.hashCode() : 0);
        result1 = 31 * result1 + (approvalType != null ? approvalType.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + counter;
        result1 = 31 * result1 + (identifier != null ? identifier.hashCode() : 0);
        result1 = 31 * result1 + (fetchMaterials ? 1 : 0);
        result1 = 31 * result1 + (cleanWorkingDir ? 1 : 0);
        return result1;
    }

    public JobInstance getFirstJob() {
        return jobInstances.first();
    }

    public JobInstances jobsWithResult(JobResult... results) {
        return jobInstances.filterByResult(results);
    }

    @Deprecated
    /**
     * for ibatis only
     */
    public void setCompletedByTransitionId(Long completedByTransitionId) {
        this.completedByTransitionId = completedByTransitionId;
    }

    public Long getCompletedByTransitionId() {
        return completedByTransitionId;
    }

    public void building() {
        if (state != null && !isReRun()) {
            LOG.warn("Expected stage [{}] to have no state, but was {}", identifier, state, new Exception().fillInStackTrace());
        }
        state = StageState.Building;
    }

    public boolean isLatestRun() {
        return latestRun;
    }

    /**
     * for ibatis
     */
    public void setLatestRun(boolean latestRun) {
        this.latestRun = latestRun;
    }

    public boolean shouldFetchMaterials() {
        return fetchMaterials;
    }

    public boolean shouldCleanWorkingDir() {
        return cleanWorkingDir;
    }

    public Stage createClone() {
        return CLONER.deepClone(this);
    }

    public void prepareForRerunOf(SchedulingContext context, String latestConfigVersion) {
        setId(-1);
        if (rerunOfCounter == null) {
            setRerunOfCounter(counter);
        }
        this.configVersion = latestConfigVersion;
        setApprovedBy(context.getApprovedBy());
        setLatestRun(true);
        resetResult();
        setCreatedTime(new Timestamp(DateTimeUtils.currentTimeMillis()));
        jobInstances.resetJobsIds();
    }

    private void resetResult() {
        result = DEFAULT_RESULT;
    }

    public boolean hasRerunJobs() {
        return rerunOfCounter != null;
    }

    public Integer getRerunOfCounter() {
        return rerunOfCounter;
    }

    public void setRerunOfCounter(Integer rerunOfCounter) {
        this.rerunOfCounter = rerunOfCounter;
    }

    public boolean isArtifactsDeleted() {
        return artifactsDeleted;
    }

    public void setArtifactsDeleted(boolean artifactsDeleted) {
        this.artifactsDeleted = artifactsDeleted;
    }

    public String getConfigVersion() {
        return this.configVersion;
    }

    /**
     * for tests only
     */
    @Deprecated
    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public void setCreatedTime(Timestamp timestamp) {
        this.createdTime = timestamp;
    }
}
