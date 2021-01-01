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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import org.joda.time.Duration;

import java.util.Comparator;

public class JobInstanceModel {
    private final JobInstance instance;
    private final JobDurationStrategy jobDurationStrategy;
    private final AgentInfo agentInfo;

    public static final Comparator<JobInstanceModel> JOB_MODEL_COMPARATOR = (o1, o2) -> {
        int result = JobResult.JOB_RESULT_COMPARATOR.compare(o1.result(), o2.result());
        if (result != 0) {
            return result;
        }
        result = o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        return result;
    };

    public JobInstanceModel(JobInstance instance, JobDurationStrategy jobDurationStrategy, Agent agent) {
        this(instance, jobDurationStrategy, agent == null? null: new AgentInfo(agent.getHostname(), agent.getIpaddress(), agent.getUuid(), false));
    }

    public JobInstanceModel(JobInstance instance, JobDurationStrategy jobDurationStrategy, AgentInstance agentInstance) {
        this(instance, jobDurationStrategy, agentInstance == null? null: new AgentInfo(agentInstance.getHostname(), agentInstance.getIpAddress(), agentInstance.getUuid(), true));
    }

    public JobInstanceModel(JobInstance instance, JobDurationStrategy jobDurationStrategy) {
        this(instance, jobDurationStrategy, (AgentInfo) null);
    }

    private JobInstanceModel(JobInstance instance, JobDurationStrategy jobDurationStrategy, AgentInfo agentInfo) {
        this.instance = instance;
        this.jobDurationStrategy = jobDurationStrategy;
        this.agentInfo = agentInfo;
    }

    public JobIdentifier getIdentifier() {
        return instance.getIdentifier();
    }

    public JobState getState() {
        return instance.getState();
    }

    public String getStatus() {
        // we know switch is evil, but this is for generating css, we don't want to put it into domain model class
        switch (result()) {
            case Passed:
                return "Passed";
            case Failed:
                return "Failed";
            case Cancelled:
                return "Cancelled";
            default:
                return "Active";
        }
    }

    public JobResult result() {
        return instance.getResult();
    }

    public String getName() {
        return instance.getName();
    }

    public int getPercentComplete() {
        Duration eta = eta();
        if (eta.getMillis() == 0) {
            return 0;
        }
        if (eta.isShorterThan(getElapsedTime())) {
            return 100;
        }
        return (int) ((getElapsedTime().getMillis() * 100) / eta.getMillis());
    }

    public boolean isInprogress() {
        int complete = getPercentComplete();
        return complete > 0 && complete < 100;
    }

    private Duration eta() {
        return new Duration(jobDurationStrategy.getExpectedDurationMillis(getIdentifier().getPipelineName(), getIdentifier().getStageName(), instance));
    }

    public Duration getElapsedTime() {
        return instance.getElapsedTime();
    }

    public boolean isCompleted() {
        return instance.isCompleted();
    }

    public boolean hasLiveAgent() {
        return hasAgentInfo() && agentInfo.isAgentLive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobInstanceModel that = (JobInstanceModel) o;

        if (instance != null ? !instance.equals(that.instance) : that.instance != null) return false;
        if (jobDurationStrategy != null ? !jobDurationStrategy.equals(that.jobDurationStrategy) : that.jobDurationStrategy != null)
            return false;
        return agentInfo != null ? agentInfo.equals(that.agentInfo) : that.agentInfo == null;
    }

    @Override
    public int hashCode() {
        int result = instance != null ? instance.hashCode() : 0;
        result = 31 * result + (jobDurationStrategy != null ? jobDurationStrategy.hashCode() : 0);
        result = 31 * result + (agentInfo != null ? agentInfo.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobInstanceModel{" +
                "instance=" + instance +
                ", jobDurationStrategy=" + jobDurationStrategy +
                ", agentInfo=" + agentInfo +
                '}';
    }

    public String getHostname() {
        return agentInfo.hostname;
    }

    public String getIpAddress() {
        return agentInfo.ip;
    }

    public boolean hasAgentInfo() {
        return agentInfo != null;
    }

    public boolean isRerun() {
        return instance.isRerun();
    }

    public String getUuid() {
        return agentInfo.uuid;
    }

    public JobStateTransitions getTransitions() {
        return instance.getTransitions();
    }

    private static class AgentInfo {
        public String hostname;
        public String ip;
        public String uuid;
        public boolean isAgentLive;

        public AgentInfo(String hostname, String ip, String uuid, boolean isAgentLive) {
            this.hostname = hostname;
            this.ip = ip;
            this.uuid = uuid;
            this.isAgentLive = isAgentLive;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AgentInfo agentInfo = (AgentInfo) o;

            if (isAgentLive != agentInfo.isAgentLive) return false;
            if (!hostname.equals(agentInfo.hostname)) return false;
            if (!ip.equals(agentInfo.ip)) return false;
            return uuid.equals(agentInfo.uuid);
        }

        @Override
        public int hashCode() {
            int result = hostname.hashCode();
            result = 31 * result + ip.hashCode();
            result = 31 * result + uuid.hashCode();
            result = 31 * result + (isAgentLive ? 1 : 0);
            return result;
        }
    }
}
