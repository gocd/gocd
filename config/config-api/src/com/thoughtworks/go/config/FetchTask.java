/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO - #2541 - Implementing serializable here because we need to send

@ConfigTag(value = "fetchartifact")
public class FetchTask extends AbstractTask implements Serializable {

    @ConfigAttribute(value = "pipeline", allowNull = true)
    private PathFromAncestor pipelineName;
    @ConfigAttribute(value = "stage")
    private CaseInsensitiveString stage;
    @ConfigAttribute(value = "job")
    private CaseInsensitiveString job;
    @ConfigAttribute(value = "srcfile", optional = true, allowNull = true)
    @ValidationErrorKey(value = "src")
    private String srcfile;
    @ConfigAttribute(value = "srcdir", optional = true, allowNull = true)
    @ValidationErrorKey(value = "src")
    private String srcdir;
    @ConfigAttribute(value = "dest", optional = true, allowNull = true)
    private String dest;

    public static final String PIPELINE_NAME = "pipelineName";
    public static final String PIPELINE = "pipeline";
    public static final String STAGE = "stage";
    public static final String JOB = "job";
    public static final String DEST = "dest";
    public static final String SRC = "src";
    public static final String ORIGIN = "origin";
    public static final String TYPE="fetch";

    public static final String IS_SOURCE_A_FILE = "isSourceAFile";
    private final String FETCH_ARTIFACT = "Fetch Artifact";

    public FetchTask() {
    }

    public FetchTask(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName, CaseInsensitiveString job, String srcfile, String dest) {
        this();
        this.pipelineName = new PathFromAncestor(pipelineName);
        this.stage = stageName;
        this.srcfile = srcfile;
        this.job = job;
        this.dest = dest;
    }

    public FetchTask(final CaseInsensitiveString stageName, final CaseInsensitiveString job, String srcfile, String dest) {
        this(null, stageName, job, srcfile, dest);
    }

    public CaseInsensitiveString getTargetPipelineName() {
        return pipelineName == null ? null : pipelineName.getAncestorName();
    }

    public CaseInsensitiveString getDirectParentInAncestorPath() {
        return pipelineName == null ? null : pipelineName.getDirectParentName();
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

    public boolean isSourceAFile() {
        return !StringUtil.isBlank(srcfile);
    }

    public String getDest() {
        return StringUtils.isEmpty(dest) ? "" : FileUtil.normalizePath(dest);
    }

    public String getSrcdir() {
        return FileUtil.normalizePath(srcdir);
    }

    public String getRawSrcdir() {
        return srcdir;
    }

    public CaseInsensitiveString getJob() {
        return job;
    }

    public String getSrcfile() {
        return FileUtil.normalizePath(srcfile);
    }

    public String getRawSrcfile() {
        return srcfile;
    }

    public void setSrcfile(String srcfile) {
        this.srcfile = srcfile;
    }

    public String getSrc() {
        return StringUtils.isNotEmpty(srcfile) ? getSrcfile() : getSrcdir();
    }

    public void setSrcdir(String srcdir) {
        this.srcdir = srcdir;
    }

    public void setPipelineName(CaseInsensitiveString pipelineName) {
        this.pipelineName = new PathFromAncestor(pipelineName);
    }

    @Override
    public String getTaskType() {
        return "fetch";
    }

    public String getTypeForDisplay() {
        return FETCH_ARTIFACT;
    }

    public List<TaskProperty> getPropertiesForDisplay() {
        ArrayList<TaskProperty> taskProperties = new ArrayList<>();
        if (pipelineName != null && !CaseInsensitiveString.isBlank(pipelineName.getPath())) {
            taskProperties.add(new TaskProperty("PIPELINE_NAME", CaseInsensitiveString.str(pipelineName.getPath())));
        }
        taskProperties.add(new TaskProperty("STAGE_NAME", CaseInsensitiveString.str(stage)));
        taskProperties.add(new TaskProperty("JOB_NAME", job.toString()));
        if (!StringUtil.isBlank(srcfile)) {
            taskProperties.add(new TaskProperty("SRC_FILE", srcfile));
        }
        if (!StringUtil.isBlank(srcdir)) {
            taskProperties.add(new TaskProperty("SRC_DIR", srcdir));
        }
        if (!StringUtil.isBlank(dest)) {
            taskProperties.add(new TaskProperty("DEST_FILE", dest));
        }
        return taskProperties;
    }

    public File artifactDest(String pipelineName, final String fileName) {
        return new File(destOnAgent(pipelineName), fileName);
    }

    public File destOnAgent(String pipelineName) {
        return new File("pipelines" + '/' + pipelineName + '/' + getDest());
    }

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

        if (attributeMap.containsKey(SRC)) {
            boolean isFile = "1".equals(attributeMap.get(IS_SOURCE_A_FILE));
            String fileOrDir = (String) attributeMap.get(SRC);
            if (isFile) {
                this.srcfile = fileOrDir.equals("") ? null : fileOrDir;
                this.srcdir = null;
            } else  {
                this.srcdir = fileOrDir.equals("") ? null : fileOrDir;
                this.srcfile = null;
            }
        }
        if (attributeMap.containsKey(DEST)) {
            String dest = (String) attributeMap.get(DEST);
            setDest(dest);
        }
    }

    public void setDest(String dest) {
        if (StringUtils.isBlank(dest)) {
            this.dest = null;
        } else {
            this.dest = dest;
        }
    }

    public void setJob(CaseInsensitiveString job) {
        this.job = job;
    }

    public void setStage(CaseInsensitiveString stage) {
        this.stage = stage;
    }

    protected void validateTask(ValidationContext validationContext) {
        validateAttributes(validationContext);
        if ( stageAndOrJobIsBlank()){
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
            if(pipelineName.isAncestor()){
                validatePathFromAncestor(currentPipeline, validationContext);
            } else if (currentPipeline.name().equals(pipelineName.getPath())) {
                validateStagesOfSamePipeline(validationContext, currentPipeline);
            } else {
                validateDependencies(validationContext, currentPipeline);
            }
        }
    }

    private void validatePathFromAncestor(PipelineConfig currentPipeline, ValidationContext validationContext) {
        List<CaseInsensitiveString> parentPipelineNames = pipelineName.pathIncludingAncestor();
        PipelineConfig pipeline = currentPipeline;
        CaseInsensitiveString dependencyStage = null;
        for (CaseInsensitiveString parentPipelineName : parentPipelineNames) {
            if (validationContext.getPipelineConfigByName(parentPipelineName) == null) {
                addError(FetchTask.PIPELINE_NAME, String.format("Pipeline named '%s' which is declared ancestor of '%s' through path '%s' does not exist.", parentPipelineName, currentPipeline.name(), pipelineName.getPath()));
                return;
            }
            DependencyMaterialConfig matchingDependencyMaterial = findMatchingDependencyMaterial(pipeline, parentPipelineName);
            if (matchingDependencyMaterial != null) {
                dependencyStage = matchingDependencyMaterial.getStageName();
                pipeline = validationContext.getPipelineConfigByName(matchingDependencyMaterial.getPipelineName());
            } else {
                addError(FetchTask.PIPELINE_NAME,
                        String.format("Pipeline named '%s' exists, but is not an ancestor of '%s' as declared in '%s'.", parentPipelineName, currentPipeline.name(), pipelineName.getPath()));
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
        if(CaseInsensitiveString.isBlank(stage)){
            atLeastOneBlank = true;
            addError(STAGE, "Stage is a required field.");
        }
        if(CaseInsensitiveString.isBlank(job)){
            atLeastOneBlank = true;
            addError(JOB, "Job is a required field.");
        }
        return atLeastOneBlank;
    }

    private void validateAttributes(ValidationContext validationContext) {
        if (StringUtils.isNotEmpty(srcdir) && StringUtils.isNotEmpty(srcfile)) {
            addError(SRC, "Only one of srcfile or srcdir is allowed at a time");
        }
        if (StringUtils.isEmpty(srcdir) && StringUtils.isEmpty(srcfile)) {
            addError(SRC, "Should provide either srcdir or srcfile");
        }
        validateFilePath(validationContext, srcfile, SRC);
        validateFilePath(validationContext, srcdir, SRC);
        validateFilePath(validationContext, dest, DEST);
    }

    private void validateFilePath(ValidationContext validationContext, String path, String propertyName) {
        if (path == null) {
            return;
        }
        if (!FileUtil.isFolderInsideSandbox(path)) {
            String parentType = validationContext.isWithinPipelines() ? "pipeline" : "template";
            CaseInsensitiveString parentName = validationContext.isWithinPipelines() ? validationContext.getPipeline().name() : validationContext.getTemplate().name();
            String message = String.format("Task of job '%s' in stage '%s' of %s '%s' has %s path '%s' which is outside the working directory.",
                    validationContext.getJob().name(), validationContext.getStage().name(), parentType, parentName, propertyName, path);
            addError(propertyName, message);
        }
    }

    private void validateDependencies(ValidationContext validationContext, PipelineConfig currentPipeline) {
        DependencyMaterialConfig matchingMaterial = findMatchingDependencyMaterial(currentPipeline, pipelineName.getAncestorName());

        PipelineConfig ancestor = validationContext.getPipelineConfigByName(pipelineName.getAncestorName());
        if (matchingMaterial == null) {
            addError(PIPELINE_NAME, String.format("Pipeline \"%s\" tries to fetch artifact from pipeline "
                    + "\"%s\" which is not an upstream pipeline", currentPipeline.name(), pipelineName));
            return;
        }
        List<StageConfig> validStages = ancestor.validStagesForFetchArtifact(currentPipeline, validationContext.getStage().name());
        if (!validStages.contains(ancestor.findBy(stage))) {
            addStageMayNotCompleteBeforeDownstreamError(currentPipeline, validationContext);
        }
    }

    private DependencyMaterialConfig findMatchingDependencyMaterial(PipelineConfig pipeline, final CaseInsensitiveString ancestorName) {
        return ListUtil.find(pipeline.dependencyMaterialConfigs(), new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                DependencyMaterialConfig dependencyMaterialConfig = (DependencyMaterialConfig) item;
                return dependencyMaterialConfig.getPipelineName().equals(ancestorName);
            }
        });
    }

    private void addStageMayNotCompleteBeforeDownstreamError(PipelineConfig currentPipeline, ValidationContext validationContext) {
        addError(STAGE, String.format("\"%s :: %s :: %s\" tries to fetch artifact from stage \"%s :: %s\" which does not complete before \"%s\" pipeline's dependencies."
                , currentPipeline.name(), validationContext.getStage().name(), validationContext.getJob().name(), pipelineName.getAncestorName(), stage, currentPipeline.name()));
    }

    private void validateStagesOfSamePipeline(ValidationContext validationContext, PipelineConfig currentPipeline) {
        List<StageConfig> validStages = currentPipeline.validStagesForFetchArtifact(currentPipeline, validationContext.getStage().name());
        StageConfig matchingStage = ListUtil.find(validStages, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                StageConfig valid = (StageConfig) item;
                return valid.name().equals(stage);
            }
        });
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
                    addError(ORIGIN, String.format("\"%s :: %s :: %s\" tries to fetch artifact from job \"%s :: %s :: %s\" which is defined in %s - reference is not allowed",
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

    public boolean isFetchPipeline(final CaseInsensitiveString caseInsensitiveString) {
        return caseInsensitiveString.equals(this.pipelineName);
    }

    public boolean isFetchStage(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        return pipelineName.equals(this.pipelineName) && stageName.equals(this.stage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        //TODO: compare abstract tasks for correct implementation -jj

        FetchTask fetchTask = (FetchTask) o;

        if (dest != null ? !dest.equals(fetchTask.dest) : fetchTask.dest != null) {
            return false;
        }
        if (job != null ? !job.equals(fetchTask.job) : fetchTask.job != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(fetchTask.pipelineName) : fetchTask.pipelineName != null) {
            return false;
        }
        if (srcdir != null ? !srcdir.equals(fetchTask.srcdir) : fetchTask.srcdir != null) {
            return false;
        }
        if (srcfile != null ? !srcfile.equals(fetchTask.srcfile) : fetchTask.srcfile != null) {
            return false;
        }
        if (stage != null ? !stage.equals(fetchTask.stage) : fetchTask.stage != null) {
            return false;
        }

        return super.equals(fetchTask);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stage != null ? stage.hashCode() : 0);
        result = 31 * result + (job != null ? job.hashCode() : 0);
        result = 31 * result + (srcfile != null ? srcfile.hashCode() : 0);
        result = 31 * result + (srcdir != null ? srcdir.hashCode() : 0);
        result = 31 * result + (dest != null ? dest.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "FetchTask{" +
                "dest='" + dest + '\'' +
                ", pipelineName='" + pipelineName + '\'' +
                ", stage='" + stage + '\'' +
                ", job='" + job + '\'' +
                ", srcfile='" + srcfile + '\'' +
                ", srcdir='" + srcdir + '\'' +
                '}';
    }

    public String checksumPath() {
        return String.format("%s_%s_%s_md5.checksum", pipelineName, stage, job);
    }
}
