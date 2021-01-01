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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.TimeProvider;
import org.joda.time.Duration;

import java.io.Serializable;
import java.util.Date;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;


public class JobInstance extends PersistentObject implements Serializable, Comparable, BuildStateAware, Cloneable {
    public static final JobInstance NULL = new NullJobInstance("");

    private Clock timeProvider = new TimeProvider();

    private long stageId = 0;
    private String name;
    private JobState state;
    private JobResult result = JobResult.Unknown;
    private String agentUuid;
    private JobStateTransitions stateTransitions = new JobStateTransitions();
    private Date scheduledDate;
    private boolean ignored;

    private JobIdentifier identifier;
    private boolean runOnAllAgents;
    private boolean runMultipleInstance;
    private Long originalJobId;
    private boolean rerun;
    private boolean pipelineStillConfigured;

    public JobInstance(String jobConfigName) {
        this(jobConfigName, new TimeProvider());
    }

    public JobInstance(String jobConfigName, Clock clock) {
        this.name = jobConfigName;
        this.timeProvider = clock;
        schedule();
    }

    /**
     * @deprecated Only use for IBatis
     */
    public JobInstance() {
    }


    public void schedule() {
        this.scheduledDate = timeProvider.currentTime();
        changeState(JobState.Scheduled, this.scheduledDate);
    }

    public void reschedule() {
        schedule();
        this.agentUuid = null;
        this.result = JobResult.Unknown;
    }

    public void assign(String agentUuid, Date time) {
        changeState(JobState.Assigned, time);
        this.agentUuid = agentUuid;
    }

    public void setClock(Clock timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Deprecated //this API is ONLY designed for ibatis.
    public void setState(JobState state) {
        this.state = state;
    }

    /**
     * @deprecated the JobInstance should not have a timeProvider.
     * Please pass in the date and remove this method
     */
    public void changeState(JobState newState) {
        changeState(newState, timeProvider.currentTime());
    }

    public void changeState(JobState newState, Date stateChangeTime) {
        if (this.state != newState) {
            this.state = newState;
            stateTransitions.add(new JobStateTransition(newState, stateChangeTime));
        }
    }

    public JobStateTransition getTransition(JobState state) {
        return stateTransitions.byState(state);
    }

    public String getName() {
        return name;
    }

    @Override
    public JobState getState() {
        return state;
    }

    @Override
    public JobResult getResult() {
        return result;
    }

    @Deprecated //Only for iBatis & Mothers / Tests
    public void setResult(JobResult result) {
        this.result = result;
    }

    public String getAgentUuid() {
        return agentUuid;
    }

    public void setAgentUuid(String agentUuid) {
        this.agentUuid = agentUuid;
    }

    @Override public String toString() {
        return "JobInstance{" +
                "stageId=" + stageId +
                ", name='" + name + '\'' +
                ", state=" + state +
                ", result=" + result +
                ", agentUuid='" + agentUuid + '\'' +
                ", stateTransitions=" + stateTransitions +
                ", scheduledDate=" + scheduledDate +
                ", timeProvider=" + timeProvider +
                ", ignored=" + ignored +
                ", identifier=" + identifier +
                ", plan=" + plan +
                ", runOnAllAgents=" + runOnAllAgents +
                ", runMultipleInstance=" + runMultipleInstance +
                '}';
    }

    public boolean isNull() {
        return false;
    }

    public boolean isPassed() {
        return result.isPassed();
    }

    public boolean isCompleted() {
        return JobState.Completed.equals(getState());
    }

    public boolean isRescheduled() {
        return JobState.Rescheduled.equals(getState());
    }

    public boolean isPreparing() {
        return getState().isPreparing();
    }

    public void completing(JobResult result, Date completionDate) {
        if (isPreparing()) {
            this.changeState(JobState.Building, completionDate);
        }
        this.result = result;
        this.changeState(JobState.Completing, completionDate);
    }

    public void completing(JobResult result) {
        completing(result, timeProvider.currentTime());
    }

    @Override
    public int compareTo(Object o) {
        return getStartedDateFor(JobState.Building)
                .compareTo(((JobInstance) o).getStartedDateFor(JobState.Building));
    }

    public void setStageId(long stageId) {
        this.stageId = stageId;
    }

    public long getStageId() {
        return stageId;
    }

    public JobStateTransitions getTransitions() {
        return stateTransitions;
    }

    public void setTransitions(JobStateTransitions transitions) {
        this.stateTransitions = transitions;
    }

    public JobState currentStatus() {
        return state;
    }

    public void completed(Date completionDate) {
        if (result==JobResult.Unknown) {
            throw new RuntimeException("Result is still unknown. Make sure completing(...) is called and then this if you are doing this through a test.");
        }
        this.changeState(JobState.Completed, completionDate);
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public String displayStatusWithResult() {
        return state == JobState.Completed ? result.toString().toLowerCase() : state.toString().toLowerCase();
    }

    public JobInstance mostRecentPassed(JobInstance champion) {
        if (!champion.isPassed()) {
            return this;
        }
        if (!this.isPassed()) {
            return champion;
        }
        return mostRecentCompleted(champion);
    }

    public JobInstance mostRecentCompleted(JobInstance champion) {
        try {
            if (getCompletedDate() != null && this.moreRecent(champion)) {
                return this;
            }
        } catch (Exception ex) {
            throw bomb(ex);
        }
        return champion;
    }

    private boolean moreRecent(JobInstance champion) {
        return getCompletedDate().compareTo(champion.getCompletedDate()) >= 0;
    }

    public void discontinue() {
        changeState(JobState.Discontinued);
    }

    public boolean cancel() {
        if (!state.isCompleted()) {
            changeState(JobState.Completed);
            result = JobResult.Cancelled;
            return true;
        }
        return false;
    }

    public boolean isFailed() {
        return this.getResult().isFailed();
    }

    // Begin Date / Time Related Methods

    public Long durationOfCompletedBuildInSeconds() {
        Date buildingDate = getStartedDateFor(JobState.Building);
        Date completedDate = getCompletedDate();
        if (buildingDate == null || completedDate == null) {
            return 0L;
        }
        Long elapsed = completedDate.getTime() - buildingDate.getTime();
        int elapsedSeconds = Math.round(elapsed / 1000);
        return Long.valueOf(elapsedSeconds);
    }

    public String getCurrentBuildDuration() {
        return String.valueOf(elapsedSeconds());
    }

    private Long elapsedSeconds() {
        if (state.isCompleted()) {
            return durationOfCompletedBuildInSeconds();
        }

        Date buildingDate = getStartedDateFor(JobState.Building);
        if (buildingDate != null) {
            long elapsed = timeProvider.currentTime().getTime() - buildingDate.getTime();
            return (long) Math.round(elapsed / 1000);
        } else {
            return 0L;
        }
    }

    public Date getStartedDateFor(JobState state) {
        JobStateTransition transition = this.stateTransitions.byState(state);
        return transition == null ? null : transition.getStateChangeTime();
    }

    public Duration getElapsedTime() {
        return new Duration(elapsedSeconds() * 1000);
    }

    public RunDuration getDuration() {
        if (!isCompleted()) {
            return RunDuration.IN_PROGRESS_DURATION;
        }
        Date scheduleStartTime = getStartedDateFor(JobState.Scheduled);
        Date completedTime = getCompletedDate();
        return new RunDuration.ActualDuration(new Duration(completedTime.getTime() - scheduleStartTime.getTime()));
    }

    /**
     * Scheduled date is duplicated in BUILDINSTANCE table to simplify sql statements.
     */
    public Date getScheduledDate() {
        return scheduledDate;
    }

    @Deprecated //Only for iBatis and BuildInstanceMother.
    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    // End Date / Time Related Methods th
    @Override
    public JobInstance clone() {
        try {
            return (JobInstance) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void fail() {
        Date completionDate = timeProvider.currentTime();
        completing(JobResult.Failed, completionDate);
        completed(completionDate);
    }

    public String getPipelineName() {
        return identifier.getPipelineName();
    }

	public Integer getPipelineCounter() {
	        return identifier.getPipelineCounter();
	    }

    public String getStageName() {
        return identifier.getStageName();
    }

    public String getStageCounter() {
        return identifier.getStageCounter();
    }

    public void setIdentifier(JobIdentifier identifier) {
        this.identifier = identifier;
    }

    public JobIdentifier getIdentifier() {
        return identifier;
    }

    public String getBuildDurationKey(String pipelineName, String stageName) {
        return String.format("BUILD_DURATION: %s_%s_%s_%s",
                pipelineName,
                stageName,
                getName(),
                getAgentUuid());
    }

    public boolean isAssignedToAgent() {
        return getAgentUuid() != null;
    }

    //TODO: #2846 this is a temporary hack to enable us to save these objects properly
    private JobPlan plan;

    public void setPlan(JobPlan plan) {
        this.plan = plan;
    }

    public JobPlan getPlan() {
        return plan;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobInstance instance = (JobInstance) o;

        if (ignored != instance.ignored) {
            return false;
        }
        if (stageId != instance.stageId) {
            return false;
        }
        if (agentUuid != null ? !agentUuid.equals(instance.agentUuid) : instance.agentUuid != null) {
            return false;
        }
        if (identifier != null ? !identifier.equals(instance.identifier) : instance.identifier != null) {
            return false;
        }
        if (name != null ? !name.equals(instance.name) : instance.name != null) {
            return false;
        }
        if (result != instance.result) {
            return false;
        }
        if (scheduledDate != null ? !scheduledDate.equals(instance.scheduledDate) : instance.scheduledDate != null) {
            return false;
        }
        if (state != instance.state) {
            return false;
        }
        if (stateTransitions != null ? !stateTransitions.equals(
                instance.stateTransitions) : instance.stateTransitions != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result1;
        result1 = (int) (stageId ^ (stageId >>> 32));
        result1 = 31 * result1 + (name != null ? name.hashCode() : 0);
        result1 = 31 * result1 + (state != null ? state.hashCode() : 0);
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (agentUuid != null ? agentUuid.hashCode() : 0);
        result1 = 31 * result1 + (stateTransitions != null ? stateTransitions.hashCode() : 0);
        result1 = 31 * result1 + (scheduledDate != null ? scheduledDate.hashCode() : 0);
        result1 = 31 * result1 + (timeProvider != null ? timeProvider.hashCode() : 0);
        result1 = 31 * result1 + (ignored ? 1 : 0);
        result1 = 31 * result1 + (identifier != null ? identifier.hashCode() : 0);
        result1 = 31 * result1 + (plan != null ? plan.hashCode() : 0);
        return result1;
    }

    public String buildLocator() {
        return identifier.buildLocator();
    }

    public String buildLocatorForDisplay() {
        return identifier.buildLocatorForDisplay();
    }

    public void setRunOnAllAgents(boolean runOnAllAgents) {
        this.runOnAllAgents = runOnAllAgents;
    }

    public boolean isRunOnAllAgents() {
        return runOnAllAgents;
    }

	public boolean isRunMultipleInstance() {
		return runMultipleInstance;
	}

	public void setRunMultipleInstance(boolean runMultipleInstance) {
		this.runMultipleInstance = runMultipleInstance;
	}

	public boolean matches(JobConfigIdentifier identifier) {
        if (!getPipelineName().equalsIgnoreCase(identifier.getPipelineName())) {
            return false;
        }
        if (!getStageName().equalsIgnoreCase(identifier.getStageName())) {
            return false;
        }
        return jobType().isInstanceOf(name, true, identifier.getJobName());
    }

    JobType jobType() {
        if (runOnAllAgents) {
            return new RunOnAllAgents();
		} else if (runMultipleInstance) {
			return new RunMultipleInstance();
		} else {
            return new SingleJobInstance();
        }
    }

    public boolean isSameStageConfig(JobInstance other) {
        return this.getIdentifier().isSameStageConfig(other.getIdentifier());
    }

    public boolean isSamePipelineInstance(JobInstance other) {
        return (getIdentifier().getPipelineLabel().equals(other.getIdentifier().getPipelineLabel()));
    }

    public String getTitle() {
        return getIdentifier().buildLocatorForDisplay();
    }

    public Date latestTransitionDate() {
        return stateTransitions.latestTransitionDate();
    }

    public long latestTransitionId() {
        return stateTransitions.latestTransitionId();
    }

    public Long getOriginalJobId() {
        return originalJobId;
    }

    public void setOriginalJobId(Long originalJobId) {
        this.originalJobId = originalJobId;
    }

    void resetForCopy() {
        if (!isCopy()) {
            setOriginalJobId(getId());
        }
        setId(-1);
        rerun = false;
        stateTransitions.resetTransitionIds();
    }

    public boolean isCopy() {
        return originalJobId != null;
    }

    public boolean isRerun() {
        return rerun;
    }

    public void setRerun(boolean rerun) {
        this.rerun = rerun;
    }

    public Date getAssignedDate() {
        return getStartedDateFor(JobState.Assigned);
    }

    public Date getCompletedDate() {
        return getStartedDateFor(JobState.Completed);
    }

    public boolean isPipelineStillConfigured() {
        return pipelineStillConfigured;
    }

    public void setPipelineStillConfigured(boolean isConfigured) {
        pipelineStillConfigured = isConfigured;
    }
}
