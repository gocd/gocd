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

package com.thoughtworks.go.plugin.api.hook.joblifecycle;

@Deprecated
public class JobContext {

    private final String pipelineName;
    private final String pipelineCounter;
    private final String pipelineLabel;
    private final String stageName;
    private final String stageCounter;
    private final String jobName;
    private final String jobCounter;
    private final String jobStatus;
    private final String agentUuid;

    public JobContext(String pipelineName, String pipelineCounter, String pipelineLabel, String stageName, String stageCounter, String jobName, String jobCounter) {
        this(pipelineName, pipelineCounter, pipelineLabel, stageName, stageCounter, jobName, jobCounter, "Unknown", null);
    }

    public JobContext(String pipelineName, String pipelineCounter, String pipelineLabel, String stageName, String stageCounter, String jobName, String jobCounter, String jobStatus,
                      String agentUuid) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = pipelineLabel;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
        this.jobName = jobName;
        this.jobCounter = jobCounter;
        this.jobStatus = jobStatus;
        this.agentUuid = agentUuid;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getPipelineCounter() {
        return pipelineCounter;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStageCounter() {
        return stageCounter;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobCounter() {
        return jobCounter;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public String getAgentUuid() {
        return agentUuid;
    }

    @Override public String toString() {
        return "JobContext{" +
                "pipelineName='" + pipelineName + '\'' +
                ", pipelineCounter='" + pipelineCounter + '\'' +
                ", pipelineLabel='" + pipelineLabel + '\'' +
                ", stageName='" + stageName + '\'' +
                ", stageCounter='" + stageCounter + '\'' +
                ", jobName='" + jobName + '\'' +
                ", jobCounter='" + jobCounter + '\'' +
                ", jobStatus=" + jobStatus +
                ", agentUuid='" + agentUuid + '\'' +
                '}';
    }
}
