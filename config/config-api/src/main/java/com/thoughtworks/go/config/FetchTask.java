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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.util.FilenameUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

// TODO - #2541 - Implementing serializable here because we need to send

@AttributeAwareConfigTag(value = "fetchartifact", attribute = "artifactOrigin", attributeValue = "gocd")
public class FetchTask extends AbstractFetchTask {
    @ConfigAttribute(value = "srcfile", optional = true, allowNull = true)
    @ValidationErrorKey(value = "src")
    private String srcfile;
    @ConfigAttribute(value = "srcdir", optional = true, allowNull = true)
    @ValidationErrorKey(value = "src")
    private String srcdir;
    @ConfigAttribute(value = "dest", optional = true, allowNull = true)
    private String dest;

    public static final String DEST = "dest";
    public static final String SRC = "src";

    public static final String IS_SOURCE_A_FILE = "isSourceAFile";
    private final String FETCH_ARTIFACT = "Fetch Artifact";

    public FetchTask() {
    }

    public FetchTask(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName, CaseInsensitiveString job, String srcfile, String dest) {
        super(pipelineName, stageName, job);
        this.srcfile = srcfile;
        this.dest = dest;
    }

    public FetchTask(final CaseInsensitiveString stageName, final CaseInsensitiveString job, String srcfile, String dest) {
        this(null, stageName, job, srcfile, dest);
    }

    public boolean isSourceAFile() {
        return !StringUtils.isBlank(srcfile);
    }

    public String getDest() {
        return StringUtils.isEmpty(dest) ? "" : FilenameUtils.separatorsToUnix(dest);
    }

    public String getSrcdir() {
        return FilenameUtils.separatorsToUnix(srcdir);
    }

    public String getRawSrcdir() {
        return srcdir;
    }

    public String getSrcfile() {
        return FilenameUtils.separatorsToUnix(srcfile);
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

    @Override
    public String getTypeForDisplay() {
        return FETCH_ARTIFACT;
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        List<TaskProperty> taskProperties = super.getPropertiesForDisplay();
        if (!StringUtils.isBlank(srcfile)) {
            taskProperties.add(new TaskProperty("Source File", srcfile));
        }
        if (!StringUtils.isBlank(srcdir)) {
            taskProperties.add(new TaskProperty("Source Directory", srcdir));
        }
        if (!StringUtils.isBlank(dest)) {
            taskProperties.add(new TaskProperty("Destination", dest));
        }
        return taskProperties;
    }

    @Override
    public String describe() {
        return String.format("fetch artifact [%s] => [%s] from [%s/%s/%s]", getSrc(), getDest(), getPipelineName(), getStage(), getJob());
    }

    @Override
    public File destOnAgent(String pipelineName) {
        return new File("pipelines" + '/' + pipelineName + '/' + getDest());
    }

    @Override
    public String getArtifactOrigin() {
        return "gocd";
    }

    @Override
    protected void setFetchTaskAttributes(Map attributeMap) {
        if (attributeMap.containsKey(SRC)) {
            boolean isFile = "1".equals(attributeMap.get(IS_SOURCE_A_FILE));
            String fileOrDir = (String) attributeMap.get(SRC);
            if (isFile) {
                this.srcfile = fileOrDir.equals("") ? null : fileOrDir;
                this.srcdir = null;
            } else {
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

    @Override
    protected void validateAttributes(ValidationContext validationContext) {
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
        if (!FilenameUtil.isNormalizedPathOutsideWorkingDir(path)) {
            String parentType = validationContext.isWithinPipelines() ? "pipeline" : "template";
            CaseInsensitiveString parentName = validationContext.isWithinPipelines() ? validationContext.getPipeline().name() : validationContext.getTemplate().name();
            String message = String.format("Task of job '%s' in stage '%s' of %s '%s' has %s path '%s' which is outside the working directory.",
                    validationContext.getJob().name(), validationContext.getStage().name(), parentType, parentName, propertyName, path);
            addError(propertyName, message);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FetchTask fetchTask = (FetchTask) o;

        if (dest != null ? !dest.equals(fetchTask.dest) : fetchTask.dest != null) {
            return false;
        }

        if (srcdir != null ? !srcdir.equals(fetchTask.srcdir) : fetchTask.srcdir != null) {
            return false;
        }
        if (srcfile != null ? !srcfile.equals(fetchTask.srcfile) : fetchTask.srcfile != null) {
            return false;
        }

        return super.equals(fetchTask);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (srcfile != null ? srcfile.hashCode() : 0);
        result = 31 * result + (srcdir != null ? srcdir.hashCode() : 0);
        result = 31 * result + (dest != null ? dest.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FetchTask{" +
                "dest='" + dest + '\'' +
                ", pipelineName='" + pipelineName + '\'' +
                ", stage='" + stage + '\'' +
                ", job='" + job + '\'' +
                ", srcfile='" + srcfile + '\'' +
                ", srcdir='" + srcdir + '\'' +
                '}';
    }
}
