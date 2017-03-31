/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.io.File;
import java.util.Date;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.label.PipelineLabel;

import static com.thoughtworks.go.domain.label.PipelineLabel.COUNT;

public class Pipeline extends PersistentObject implements PipelineInfo {

    private String pipelineName = "";
    private Integer counter;
    private PipelineLabel pipelineLabel = PipelineLabel.defaultLabel();
    private Stages stages = new Stages();
    private BuildCause buildCause;
    private double naturalOrder;

    public Pipeline() {
        this(new Stages());
    }

    private Pipeline(Stages stages) {
        this.stages = stages;
        this.counter = 0;//counter can never be null, as pipeline identifier creation with null counter is not allowed
    }

    public Pipeline(String pipelineName, String labelTemplate, BuildCause buildCause, Stage... stages) {
        this(new Stages(stages));
        this.pipelineName = pipelineName;
        this.pipelineLabel = PipelineLabel.create(labelTemplate);
        this.buildCause = buildCause;
    }

    public Pipeline(String pipelineName, BuildCause buildCause, Stage... stages) {
        this(pipelineName, PipelineLabel.COUNT_TEMPLATE, buildCause, stages);
    }

    public File defaultWorkingFolder() {
        return new File(pipelineName);
    }

    public String getName() {
        return pipelineName;
    }

    public void setName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public Integer getCounter() {
        return counter;
    }

    public Stages getStages() {
        return stages;
    }

    public void setStages(Stages stages) {
        this.stages = stages;
    }

    public Stage getFirstStage() {
        return stages.first() == null ? new NullStage("unknown") : stages.first();
    }

    public Materials getMaterials() {
        return buildCause.materials();
    }

    public BuildCause getBuildCause() {
        return buildCause;
    }

    public void setBuildCause(BuildCause buildCause) {
        this.buildCause = buildCause;
    }

    public void setModificationsOnBuildCause(MaterialRevisions materialRevisions) {
        this.buildCause.setMaterialRevisions(materialRevisions);
    }

    public String getBuildCauseMessage() {
        if (buildCause == null) {
            return "Unknown";
        }
        //TODO: ChrisS: Replace this with encapsulation and null checks for the buildCause
        //also, we may choos to persist the real username and buildcause rather than throwing
        // away that information as we do now
        String message = "Unknown";
        if (getBuildCause() != null && getBuildCause().getBuildCauseMessage() != null) {
            message = getBuildCause().getBuildCauseMessage();
        }
        return message;
    }

    public ModificationSummaries toModificationSummaries() {
        if (buildCause == null) {
            return new ModificationSummaries();
        } else {
            return buildCause.toModificationSummaries();
        }
    }

    public boolean hasStageBeenRun(String stageName) {
        return stages.hasStage(stageName);
    }

    public String getLabel() {
        return pipelineLabel.toString();
    }

    /**
     * Will not apply template logic; to be used only in testing or by iBatis
     *
     * @param label
     */
    public void setLabel(String label) {
        this.pipelineLabel.setLabel(label);
    }

    public void updateCounter(Integer lastCount) {
        counter = lastCount + 1;
        updateLabel();
    }

    private void updateLabel() {
        Map<CaseInsensitiveString, String> namedRevisions = this.getMaterialRevisions().getNamedRevisions();
        namedRevisions.put(new CaseInsensitiveString(COUNT), counter.toString());
        this.pipelineLabel.updateLabel(namedRevisions);
    }

    public boolean isAnyStageActive() {
        return stages.isAnyStageActive();
    }

    public String nextStageName(String stageName) {
        return stages.nextStageName(stageName);
    }

    public Stage findStage(String stageName) {
        return stages.byName(stageName);
    }

    private void setBuildCauseMessage(String buildCauseMessage) {
        buildCause.setMessage(buildCauseMessage);
    }

    private void setApprovedBy(String approvedBy) {
        buildCause.setApprover(approvedBy);
    }

    public MaterialRevisions getMaterialRevisions() {
        return buildCause.getMaterialRevisions();
    }

    public Date getModifiedDate() {
        return buildCause.getModifiedDate();
    }

    public PipelineIdentifier getIdentifier() {
        return new PipelineIdentifier(pipelineName, counter, getLabel());
    }

    public double getNaturalOrder() {
        return naturalOrder;
    }

    public void setNaturalOrder(double naturalOrder) {
        this.naturalOrder = naturalOrder;
    }

    public EnvironmentVariablesConfig scheduleTimeVariables() {
        return buildCause.getVariables();
    }

    public boolean isBisect() {
         return naturalOrderIsNotAnInteger();
    }

    private boolean naturalOrderIsNotAnInteger() {
        return naturalOrder - new Double(naturalOrder).intValue() > 0;
    }
}
