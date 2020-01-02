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

import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.util.FilenameUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BuildTask extends AbstractTask implements CommandTask {
    @ConfigAttribute(value = "buildfile", allowNull = true)
    protected String buildFile;
    @ConfigAttribute(value = "target", allowNull = true)
    protected String target;
    @ConfigAttribute(value = "workingdir", allowNull = true)
    protected String workingDirectory;
    public static final String BUILD_FILE = "buildFile";
    public static final String TARGET = "target";
    public static final String WORKING_DIRECTORY = "workingDirectory";

    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String workingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDir) {
        this.workingDirectory = workingDir;
    }

    @Override
    public String describe() {
        List<String> description = new ArrayList<>();
        description.add(command());

        if (!"".equals(arguments())) {
            description.add(arguments());
        }

        if (null != workingDirectory()) {
            description.add(String.format("(workingDirectory: %s)", workingDirectory()));
        }

        return StringUtils.join(description, " ");
    }

    @Override
    protected final void setTaskConfigAttributes(Map attributeMap) {
        buildFile = inferValueFromMap(attributeMap, BUILD_FILE);
        target = inferValueFromMap(attributeMap, TARGET);
        workingDirectory = inferValueFromMap(attributeMap, WORKING_DIRECTORY);
        setBuildTaskConfigAttributes(attributeMap);
    }

    protected String inferValueFromMap(Map attributeMap, String key) {
        String value = null;
        if (attributeMap.containsKey(key) && !StringUtils.isBlank((String) attributeMap.get(key))) {
            value = (String) attributeMap.get(key);
        }
        return value;
    }

    protected void setBuildTaskConfigAttributes(Map attributeMap) {
    }

    @Override
    public void validateTask(ValidationContext validationContext) {
        if (validationContext.isWithinPipelines()) {
            validateWorkingDirectory(validationContext, "pipeline", validationContext.getPipeline().name());
        } else {
            validateWorkingDirectory(validationContext, "template", validationContext.getTemplate().name());
        }
    }

    private void validateWorkingDirectory(ValidationContext validationContext, String stageParentType, Object stageParentName) {
        if (workingDirectory != null && !FilenameUtil.isNormalizedPathOutsideWorkingDir(workingDirectory)) {
            errors.add(WORKING_DIRECTORY, String.format("Task of job '%s' in stage '%s' of %s '%s' has path '%s' which is outside the working directory.", validationContext.getJob().name(),
                    validationContext.getStage().name(), stageParentType, stageParentName, workingDirectory));
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
        if (!super.equals(o)) {
            return false;
        }

        BuildTask buildTask = (BuildTask) o;

        if (buildFile != null ? !buildFile.equals(buildTask.buildFile) : buildTask.buildFile != null) {
            return false;
        }
        if (target != null ? !target.equals(buildTask.target) : buildTask.target != null) {
            return false;
        }
        if (workingDirectory != null ? !workingDirectory.equals(buildTask.workingDirectory) : buildTask.workingDirectory != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (buildFile != null ? buildFile.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (workingDirectory != null ? workingDirectory.hashCode() : 0);
        return result;
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        ArrayList<TaskProperty> taskProperties = new ArrayList<>();
        if (!StringUtils.isBlank(buildFile)) {
            taskProperties.add(new TaskProperty("Build File", buildFile));
        }
        if (!StringUtils.isBlank(target)) {
            taskProperties.add(new TaskProperty("Target", target));
        }
        if (!StringUtils.isBlank(workingDirectory)) {
            taskProperties.add(new TaskProperty("Working Directory", workingDirectory));
        }
        return taskProperties;
    }
}
