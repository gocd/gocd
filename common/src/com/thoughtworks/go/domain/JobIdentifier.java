/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.util.UrlUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.io.Serializable;

public class JobIdentifier implements Serializable, LocatableEntity {
    @Expose
    private String pipelineName;
    @Expose
    private Integer pipelineCounter;
    @Expose
    private String pipelineLabel;
    @Expose
    private String stageName;
    @Expose
    private String buildName;

    @Expose
    private Long buildId;
    @Expose
    private String stageCounter;
    public static final String LATEST = "latest";
    @Expose
    private Integer rerunOfCounter;

    public JobIdentifier(Pipeline pipeline, Stage stage, JobInstance jobInstance) {
        this(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter()), jobInstance.getName(), jobInstance.getId());
    }

    public JobIdentifier(StageIdentifier stageIdentifier, JobInstance job) {
        this(stageIdentifier.getPipelineName(), stageIdentifier.getPipelineCounter(),
                stageIdentifier.getPipelineLabel(), stageIdentifier.getStageName(),
                stageIdentifier.getStageCounter(), job.getName(), job.getId());
    }

    public JobIdentifier(StageIdentifier stage, String jobName) {
        this(stage.getPipelineName(), stage.getPipelineCounter(), stage.getPipelineLabel(), stage.getStageName(),
                stage.getStageCounter(), jobName, 0L);
    }

    public JobIdentifier(StageIdentifier stage, String jobName, Long jobId) {
        this(stage.getPipelineName(), stage.getPipelineCounter(), stage.getPipelineLabel(), stage.getStageName(),
                stage.getStageCounter(), jobName, jobId);
    }

    public JobIdentifier(String pipelineName, int pipelineCounter, String pipelineLabel, String staqeName, String stageCounter, String jobName) {
        this(pipelineName, pipelineCounter, pipelineLabel, staqeName, stageCounter, jobName, -1L);
    }

    public static JobIdentifier invalidIdentifier(String pipelineName, String pipelineLabel, String stageName,
                                                  String stageCounter, String buildName) {
        return new JobIdentifier(pipelineName, pipelineLabel, stageName, stageCounter, buildName, null);
    }

    @Deprecated // should use pipeline counter
    public JobIdentifier(String pipelineName, String pipelineLabel, String stageName,
                         String counter, String buildName) {
        this(pipelineName, pipelineLabel, stageName, counter, buildName, 0L);
    }

    @Deprecated // should use pipeline counter
    public JobIdentifier(String pipelineName, String pipelineLabel, String stageName, int stageCounter, String buildName, Long buildId) {
        this(pipelineName, pipelineLabel, stageName, String.valueOf(stageCounter), buildName, buildId);
    }

    public JobIdentifier(String pipelineName, Integer pipelineCounter, String pipelineLabel, String stageName, String stageCounter, String buildName, Long buildId) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = pipelineLabel;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
        this.buildName = buildName;
        this.buildId = buildId;
    }

    /*this constructor is for ibatis*/
    public JobIdentifier() {
    }

    @Deprecated // should use pipeline counter
    public JobIdentifier(String pipelineName, String pipelineLabel, String stageName, String stageCounter, String buildName, Long buildId) {
        this(pipelineName, null, pipelineLabel, stageName, stageCounter, buildName, buildId);
        this.buildId = buildId;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public void setPipelineLabel(String pipelineLabel) {
        this.pipelineLabel = pipelineLabel;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setStageCounter(String stageCounter) {
        this.stageCounter = stageCounter;
    }

    public String getPipelineName() {
        return pipelineName;
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

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getBuildName() {
        return buildName;
    }

    public Long getBuildId() {
        return buildId;
    }

    public void setBuildId(Long buildId) {
        this.buildId = buildId;
    }

    public String toString() {
        return String.format("JobIdentifier[%s, %s, %s, %s, %s, %s, %s]", pipelineName, pipelineCounter, pipelineLabel,
                stageName, stageCounter, buildName, buildId);
    }

    public String toFullString() {
        return "Build [" + buildLocator() + "/" + getBuildId() + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobIdentifier that = (JobIdentifier) o;

        if (buildId != null ? !buildId.equals(that.buildId) : that.buildId != null) {
            return false;
        }
        if (buildName != null ? !buildName.equals(that.buildName) : that.buildName != null) {
            return false;
        }
        if (pipelineCounter != null ? !pipelineCounter.equals(that.pipelineCounter) : that.pipelineCounter != null) {
            return false;
        }
        if (pipelineLabel != null ? !pipelineLabel.equals(that.pipelineLabel) : that.pipelineLabel != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageCounter != null ? !stageCounter.equals(that.stageCounter) : that.stageCounter != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (pipelineLabel != null ? pipelineLabel.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + (buildName != null ? buildName.hashCode() : 0);
        result = 31 * result + (buildId != null ? buildId.hashCode() : 0);
        result = 31 * result + (stageCounter != null ? stageCounter.hashCode() : 0);
        result = 31 * result + (pipelineCounter != null ? pipelineCounter.hashCode() : 0);
        return result;
    }

    public String buildLocator() {
        //TODO: the encoding logic should be moved to presentation layer
        return UrlUtil.encodeInUtf8(String.format("%s/%s", stageLocator(), buildName));
    }

    private String stageLocator() {
        return getStageIdentifier().stageLocatorByLabelOrCounter();
    }

    public String buildLocatorForDisplay() {
        return String.format("%s/%s", getStageIdentifier().stageLocatorForDisplay(), buildName);
    }

    public String propertyLocator(String propertyName) {
        return UrlUtil.encodeInUtf8(String.format("%s/%s/%s", stageLocator(), buildName, propertyName));
    }

    public String artifactLocator(String filePath) {
        //TODO: we should make sure data is valid at the beginning instead of fixing it here
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        return UrlUtil.encodeInUtf8(String.format("%s/%s/%s", stageLocator(), buildName, filePath));
    }

    public boolean needTranslateJob() {
        return buildId == null || buildId <= 0L;
    }

    public StageIdentifier getStageIdentifier() {
        return new StageIdentifier(pipelineName, pipelineCounter, pipelineLabel, stageName,
                stageCounter);
    }

    public String ccProjectName() {
        return String.format("%s :: %s :: %s", getPipelineName(), getStageName(), getBuildName());
    }

    public String webUrl() {
        return "tab/build/detail/" + buildLocator();
    }

    public JobConfigIdentifier jobConfigIdentifier() {
        return new JobConfigIdentifier(pipelineName, stageName, buildName);
    }

    public Integer getPipelineCounter() {
        return pipelineCounter;
    }

    public void setPipelineCounter(Integer pipelineCounter) {
        this.pipelineCounter = pipelineCounter;
    }

    public boolean isSameStageConfig(JobIdentifier other) {
        return getPipelineName().equalsIgnoreCase(other.getPipelineName()) 
                && getStageName().equalsIgnoreCase(other.getStageName());

    }

    public void populateEnvironmentVariables(EnvironmentVariableContext environmentVariableContext) {
        environmentVariableContext.setProperty("GO_PIPELINE_NAME", getPipelineName(), false);
        environmentVariableContext.setProperty("GO_PIPELINE_COUNTER", String.valueOf(getPipelineCounter()), false);
        environmentVariableContext.setProperty("GO_PIPELINE_LABEL", getPipelineLabel(), false);
        environmentVariableContext.setProperty("GO_STAGE_NAME", getStageName(), false);
        environmentVariableContext.setProperty("GO_STAGE_COUNTER", getStageCounter(), false);
        if (getRerunOfCounter() != null) {
            environmentVariableContext.setProperty("GO_RERUN_OF_STAGE_COUNTER", String.valueOf(getRerunOfCounter()), false);
        }
        environmentVariableContext.setProperty("GO_JOB_NAME", getBuildName(), false);
    }

    public String asURN() {
        return String.format("urn:x-go.studios.thoughtworks.com:job-id:%s:%s:%s:%s:%s", pipelineName, pipelineCounter, stageName, stageCounter, buildName);
    }

    public Integer getRerunOfCounter() {
        return rerunOfCounter;
    }

    public void setRerunOfCounter(Integer rerunOfCounter) {
        this.rerunOfCounter = rerunOfCounter;
    }

    public String entityLocator() {
        return buildLocator();
    }

    public Long getId() {
        return getBuildId();
    }
}
