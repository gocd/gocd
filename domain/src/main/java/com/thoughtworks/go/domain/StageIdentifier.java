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
package com.thoughtworks.go.domain;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

public class StageIdentifier implements Serializable, LocatableEntity {
    private String pipelineName;
    private Integer pipelineCounter;
    private String pipelineLabel;
    private String stageName;
    private String stageCounter;
    private Long id;

    public static final StageIdentifier NULL = new StageIdentifier(new NullPipeline(),new NullStage(null));
    public StageIdentifier() {
    }

    public StageIdentifier(Pipeline pipeline, Stage stage) {
        this(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter()));
    }

    public StageIdentifier(String pipelineName, Integer pipelineCounter, String pipelineLabel, Long stageId, String stageName, String stageCounter) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = pipelineLabel;
        id = stageId;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
    }

    public StageIdentifier(String pipelineName, Integer pipelineCounter, String pipelineLabel, String stageName, String stageCounter) {
        String label = StringUtils.isBlank(pipelineLabel) ? "latest" : pipelineLabel;
        setLocatorAttributes(pipelineName, pipelineCounter, label, stageName, stageCounter);
    }

    public StageIdentifier(String pipelineName, int pipelineCounter, String stageName, String stageCounter) {
        setLocatorAttributes(pipelineName, pipelineCounter, null, stageName, stageCounter);
    }

    public StageIdentifier(PipelineIdentifier pipelineIdentifier, String name, String counter) {
        this(pipelineIdentifier.getName(), pipelineIdentifier.getCounter(), pipelineIdentifier.getLabel(), name, counter);
    }

    public StageIdentifier(String stageLocator) {
        String[] locatorElements = stageLocator.split("/");
        String counter = locatorElements[1];
        setLocatorAttributes(locatorElements[0], Integer.parseInt(counter), null, locatorElements[2], locatorElements[3]);
    }

    private void setLocatorAttributes(String pipelineName, Integer pipelineCounter, String label, String stageName, String stageCounter) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = label;
        this.stageName = stageName;
        this.stageCounter = StringUtils.isBlank(stageCounter) ? "latest" : stageCounter;
    }

    public String stageLocator() throws RuntimeException {
        return String.format("%s/%s/%s", pipelineIdentifier().pipelineLocator(), stageName, stageCounter);
    }

    public PipelineIdentifier pipelineIdentifier() {
        return new PipelineIdentifier(pipelineName, pipelineCounter, pipelineLabel);
    }

    public String stageLocatorForDisplay() throws RuntimeException {
        PipelineIdentifier pipeline = pipelineIdentifier();
        return String.format("%s/%s/%s", pipeline.pipelineLocatorForDisplay(), stageName, stageCounter);
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    public String getStageCounter() {
        return stageCounter;
    }

    @Override
    public String toString() {
        return String.format("StageIdentifier[%s, %s, %s, %s, %s]", pipelineName, pipelineCounter, pipelineLabel,
                stageName, stageCounter);
    }

    public String getStageLocator() {
        return String.format("%s/%s/%s/%s", getPipelineName(), getPipelineCounter(), getStageName(), getStageCounter());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageIdentifier that = (StageIdentifier) o;

        if (pipelineCounter != null ? !pipelineCounter.equals(that.pipelineCounter) : that.pipelineCounter != null) {
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

    @Override
    public int hashCode() {
        int result;
        result = (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (pipelineCounter != null ? pipelineCounter.hashCode() : 0);
        result = 31 * result + (pipelineLabel != null ? pipelineLabel.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + (stageCounter != null ? stageCounter.hashCode() : 0);
        return result;
    }

    public String ccProjectName() {
        return String.format("%s :: %s", getPipelineName(), getStageName());
    }

    public String webUrl() {
        return "pipelines/" + stageLocator();
    }

    public String stageLocatorByLabelOrCounter() {
        return String.format("%s/%s/%s", pipelineIdentifier().pipelineLocatorByLabelOrCounter(), stageName, stageCounter);
    }

    public String ccTrayLastBuildLabel() {
        return Integer.parseInt(stageCounter) > 1 ? String.format("%s :: %s", pipelineLabel,
                stageCounter) : pipelineLabel;
    }

    public StageConfigIdentifier stageConfigIdentifier() {
        return new StageConfigIdentifier(pipelineName, stageName);
    }

    public Integer getPipelineCounter() {
        return pipelineCounter;
    }

    /**
     * @deprecated only for iBatis
     */
    void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    /**
     * @deprecated only for iBatis
     */
    void setPipelineLabel(String pipelineLabel) {
        this.pipelineLabel = pipelineLabel;
    }

    /**
     * @deprecated only for iBatis
     */
    void setStageName(String stageName) {
        this.stageName = stageName;
    }

    /**
     * @deprecated only for iBatis
     */
    void setStageCounter(String stageCounter) {
        this.stageCounter = stageCounter;
    }

    /**
     * @deprecated only for iBatis
     */
    void setPipelineCounter(Integer pipelineCounter) {
        this.pipelineCounter = pipelineCounter;
    }

    public String asURN() {
        return String.format("urn:x-go.studios.thoughtworks.com:stage-id:%s:%s:%s:%s", pipelineName, pipelineCounter, stageName, stageCounter);
    }


    @Override
    public String entityLocator() {
        return getStageLocator();
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * @deprecated only for iBatis
     */
    public void setId(Long id) {
        this.id = id;
    }
}
