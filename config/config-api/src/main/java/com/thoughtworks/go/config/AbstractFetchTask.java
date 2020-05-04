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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.TaskProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractFetchTask extends AbstractTask implements FetchArtifactConfig {
    public static final String PIPELINE_NAME = "pipelineName";
    public static final String PIPELINE = "pipeline";
    public static final String STAGE = "stage";
    public static final String JOB = "job";
    public static final String TYPE = "fetch";
    public static final String ARTIFACT_ORIGIN = "artifact_origin";

    @ConfigAttribute(value = "pipeline", allowNull = true)
    protected PathFromAncestor pipelineName;
    @ConfigAttribute(value = "stage")
    protected CaseInsensitiveString stage;
    @ConfigAttribute(value = "job")
    protected CaseInsensitiveString job;

    public AbstractFetchTask() {
    }

    protected AbstractFetchTask(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stage, CaseInsensitiveString job) {
        this.pipelineName = new PathFromAncestor(pipelineName);
        this.stage = stage;
        this.job = job;
    }

    public AbstractFetchTask(CaseInsensitiveString stage, CaseInsensitiveString job) {
        this.stage = stage;
        this.job = job;
    }

    public CaseInsensitiveString getPipelineName() {
        return pipelineName == null ? null : pipelineName.getPath();
    }

    public PathFromAncestor getPipelineNamePathFromAncestor() {
        return pipelineName;
    }

    public CaseInsensitiveString getStage() {
        return stage;
    }

    public CaseInsensitiveString getJob() {
        return job;
    }

    public void setPipelineName(PathFromAncestor pipelineName) {
        this.pipelineName = pipelineName;
    }

    public void setPipelineName(CaseInsensitiveString pipelineName) {
        this.pipelineName = new PathFromAncestor(pipelineName);
    }

    public void setStage(CaseInsensitiveString stage) {
        this.stage = stage;
    }

    public void setJob(CaseInsensitiveString job) {
        this.job = job;
    }

    public CaseInsensitiveString getTargetPipelineName() {
        return pipelineName == null ? null : pipelineName.getAncestorName();
    }

    public CaseInsensitiveString getDirectParentInAncestorPath() {
        return pipelineName == null ? null : pipelineName.getDirectParentName();
    }

    public File artifactDest(String pipelineName, final String fileName) {
        return new File(destOnAgent(pipelineName), fileName);
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        List<TaskProperty> taskProperties = new ArrayList<>();
        if (pipelineName != null && !CaseInsensitiveString.isBlank(pipelineName.getPath())) {
            taskProperties.add(new TaskProperty("Pipeline Name", CaseInsensitiveString.str(pipelineName.getPath())));
        }
        taskProperties.add(new TaskProperty("Stage Name", CaseInsensitiveString.str(stage)));
        taskProperties.add(new TaskProperty("Job Name", job.toString()));
        return taskProperties;
    }

    @Override
    public String getTaskType() {
        return TYPE;
    }


    protected abstract File destOnAgent(String pipelineName);

    public abstract String getArtifactOrigin();

    @Override
    protected void validateTask(ValidationContext validationContext) {
        if (stageAndOrJobIsBlank()) {
            return;
        }
        if (validationContext.isWithinPipelines()) {
            PipelineConfig currentPipeline = validationContext.getPipeline();
            if (pipelineName == null || CaseInsensitiveString.isBlank(pipelineName.getPath())) {
                pipelineName = new PathFromAncestor(currentPipeline.name());
            }
            if (validateExistenceAndOrigin(currentPipeline, validationContext)) {
                return;
            }
            if (pipelineName.isAncestor()) {
                validatePathFromAncestor(currentPipeline, validationContext);
            } else if (currentPipeline.name().equals(pipelineName.getPath())) {
                validateStagesOfSamePipeline(validationContext, currentPipeline);
            } else {
                validateDependencies(validationContext, currentPipeline);
            }
        }
        validateAttributes(validationContext);
    }

    protected abstract void validateAttributes(ValidationContext validationContext);

    private void validatePathFromAncestor(PipelineConfig currentPipeline, ValidationContext validationContext) {
        List<CaseInsensitiveString> parentPipelineNames = pipelineName.pathIncludingAncestor();
        PipelineConfig pipeline = currentPipeline;
        CaseInsensitiveString dependencyStage = null;
        for (CaseInsensitiveString parentPipelineName : parentPipelineNames) {
            if (validationContext.getPipelineConfigByName(parentPipelineName) == null) {
                String message = String.format("Pipeline named '%s' which is declared ancestor of '%s' through path '%s' does not exist.", parentPipelineName, currentPipeline.name(), pipelineName.getPath());
                addError(FetchTask.PIPELINE_NAME, message);
                currentPipeline.addError("base", message);
                return;
            }
            DependencyMaterialConfig matchingDependencyMaterial = findMatchingDependencyMaterial(pipeline, parentPipelineName);
            if (matchingDependencyMaterial != null) {
                dependencyStage = matchingDependencyMaterial.getStageName();
                pipeline = validationContext.getPipelineConfigByName(matchingDependencyMaterial.getPipelineName());
            } else {
                String message = String.format("Pipeline named '%s' exists, but is not an ancestor of '%s' as declared in '%s'.", parentPipelineName, currentPipeline.name(), pipelineName.getPath());
                addError(FetchTask.PIPELINE_NAME, message);
                currentPipeline.addError("base", message);
                return;
            }
        }

        boolean foundStageAtOrBeforeDependency = dependencyStage.equals(stage);
        if (!foundStageAtOrBeforeDependency) {
            for (StageConfig stageConfig : pipeline.allStagesBefore(dependencyStage)) {
                foundStageAtOrBeforeDependency = stage.equals(stageConfig.name());
                if (foundStageAtOrBeforeDependency) {
                    break;
                }
            }
        }

        if (!foundStageAtOrBeforeDependency) {
            addStageMayNotCompleteBeforeDownstreamError(currentPipeline, validationContext);
        }
    }

    private boolean stageAndOrJobIsBlank() {
        boolean atLeastOneBlank = false;
        if (CaseInsensitiveString.isBlank(stage)) {
            atLeastOneBlank = true;
            addError(STAGE, "Stage is a required field.");
        }
        if (CaseInsensitiveString.isBlank(job)) {
            atLeastOneBlank = true;
            addError(JOB, "Job is a required field.");
        }
        return atLeastOneBlank;
    }

    private void validateDependencies(ValidationContext validationContext, PipelineConfig currentPipeline) {
        DependencyMaterialConfig matchingMaterial = findMatchingDependencyMaterial(currentPipeline, pipelineName.getAncestorName());

        PipelineConfig ancestor = validationContext.getPipelineConfigByName(pipelineName.getAncestorName());
        if (matchingMaterial == null) {
            String message = String.format("Pipeline \"%s\" tries to fetch artifact from pipeline \"%s\" which is not an upstream pipeline", currentPipeline.name(), pipelineName);
            addError(PIPELINE_NAME, message);
            currentPipeline.addError("base", message);
            return;
        }
        List<StageConfig> validStages = ancestor.validStagesForFetchArtifact(currentPipeline, validationContext.getStage().name());
        if (!validStages.contains(ancestor.findBy(stage))) {
            addStageMayNotCompleteBeforeDownstreamError(currentPipeline, validationContext);
        }
    }

    private DependencyMaterialConfig findMatchingDependencyMaterial(PipelineConfig pipeline, final CaseInsensitiveString ancestorName) {
        return pipeline.dependencyMaterialConfigs().stream().filter(dependencyMaterialConfig -> dependencyMaterialConfig.getPipelineName().equals(ancestorName)).findFirst().orElse(null);
    }

    private void addStageMayNotCompleteBeforeDownstreamError(PipelineConfig currentPipeline, ValidationContext validationContext) {
        addError(STAGE, String.format("\"%s :: %s :: %s\" tries to fetch artifact from stage \"%s :: %s\" which does not complete before \"%s\" pipeline's dependencies."
                , currentPipeline.name(), validationContext.getStage().name(), validationContext.getJob().name(), pipelineName.getAncestorName(), stage, currentPipeline.name()));
    }

    private void validateStagesOfSamePipeline(ValidationContext validationContext, PipelineConfig currentPipeline) {
        List<StageConfig> validStages = currentPipeline.validStagesForFetchArtifact(currentPipeline, validationContext.getStage().name());
        StageConfig matchingStage = validStages.stream().filter(stageConfig -> stageConfig.name().equals(stage)).findFirst().orElse(null);
        if (matchingStage == null) {
            addError(STAGE, String.format("\"%s :: %s :: %s\" tries to fetch artifact from its stage \"%s\" which does not complete before the current stage \"%s\"."
                    , currentPipeline.name(), validationContext.getStage().name(), validationContext.getJob().name(), stage, validationContext.getStage().name()));
        }
    }

    private boolean validateExistenceAndOrigin(PipelineConfig currentPipeline, ValidationContext validationContext) {
        PipelineConfig srcPipeline = validationContext.getPipelineConfigByName(pipelineName.getAncestorName());

        if (srcPipeline == null) {
            //"ProdDeploy :: deploy :: scp" tries|attempts to fetch artifact from pipeline "not-found" which does not exist.
            addError(PIPELINE, String.format("\"%s :: %s :: %s\" tries to fetch artifact from pipeline \"%s\" which does not exist."
                    , currentPipeline.name(), validationContext.getStage().name(), validationContext.getJob().name(), pipelineName.getAncestorName()));
            return true;
        } else {
            StageConfig srcStage = srcPipeline.findBy(stage);
            if (srcStage == null) {
                addError(STAGE, String.format("\"%s :: %s :: %s\" tries to fetch artifact from stage \"%s :: %s\" which does not exist."
                        , currentPipeline.name(), validationContext.getStage().name(), validationContext.getJob().name(), pipelineName.getAncestorName(), stage));
                return true;
            } else {
                if (srcStage.jobConfigByInstanceName(CaseInsensitiveString.str(job), true) == null) {
                    addError(JOB, String.format("\"%s :: %s :: %s\" tries to fetch artifact from job \"%s :: %s :: %s\" which does not exist.", currentPipeline.name(), validationContext.getStage().name(), validationContext.getJob().name(), pipelineName.getAncestorName(), stage, job));
                    return true;
                }

            }
            if (validationContext.shouldCheckConfigRepo()) {
                if (!validationContext.getConfigRepos().isReferenceAllowed(currentPipeline.getOrigin(), srcPipeline.getOrigin())) {
                    addError(ARTIFACT_ORIGIN, String.format("\"%s :: %s :: %s\" tries to fetch artifact from job \"%s :: %s :: %s\" which is defined in %s - reference is not allowed",
                            currentPipeline.name(), validationContext.getStage().name(), validationContext.getJob().name(), pipelineName.getAncestorName(), stage, job, displayNameFor(srcPipeline.getOrigin())));
                    return true;
                }
            }
        }

        return false;
    }

    private String displayNameFor(ConfigOrigin origin) {
        return origin != null ? origin.displayName() : "cruise-config.xml";
    }

    @Override
    public boolean equals(Object o) {
        //TODO: compare abstract tasks for correct implementation -jj
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AbstractFetchTask that = (AbstractFetchTask) o;

        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) return false;
        if (stage != null ? !stage.equals(that.stage) : that.stage != null) return false;
        if (job != null ? !job.equals(that.job) : that.job != null) return false;
        return super.equals(that);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stage != null ? stage.hashCode() : 0);
        result = 31 * result + (job != null ? job.hashCode() : 0);
        return result;
    }

    @Override
    protected void setTaskConfigAttributes(Map attributeMap) {
        if (attributeMap == null || attributeMap.isEmpty()) {
            return;
        }
        if (attributeMap.containsKey(PIPELINE_NAME)) {
            this.pipelineName = new PathFromAncestor(new CaseInsensitiveString((String) attributeMap.get(PIPELINE_NAME)));
        }
        if (attributeMap.containsKey(STAGE)) {
            setStage(new CaseInsensitiveString((String) attributeMap.get(STAGE)));
        }
        if (attributeMap.containsKey(JOB)) {
            String jobString = (String) attributeMap.get(JOB);
            setJob(new CaseInsensitiveString(jobString));
        }
        setFetchTaskAttributes(attributeMap);
    }

    protected abstract void setFetchTaskAttributes(Map attributeMap);

    public String checksumPath() {
        return String.format("%s_%s_%s_md5.checksum", pipelineName, stage, job);
    }
}
