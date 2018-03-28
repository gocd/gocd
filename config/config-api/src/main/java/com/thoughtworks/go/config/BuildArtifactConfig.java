/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

@AttributeAwareConfigTag(value = "artifact", attribute = "type", attributeValue = "build")
public class BuildArtifactConfig implements ArtifactConfig {
    public static final String ARTIFACT_PLAN_DISPLAY_NAME = "Build Artifact";
    public static final File DEFAULT_ROOT = new File("");
    public static final String SRC = "source";
    public static final String DEST = "destination";

    @ConfigAttribute(value = "src", optional = false)
    private String source;
    @ConfigAttribute("dest")
    private String destination = DEFAULT_ROOT.getPath();

    private ConfigErrors errors = new ConfigErrors();

    public BuildArtifactConfig() {
    }

    public BuildArtifactConfig(String source, String destination) {
        setSource(source);
        setDestination(destination);
    }

    public String getDestination() {
        return StringUtils.isBlank(destination) ? DEFAULT_ROOT.getPath() : FilenameUtils.separatorsToUnix(destination);
    }

    public void setSource(String source) {
        this.source = StringUtils.trim(source);
    }

    public String getSource() {
        return FilenameUtils.separatorsToUnix(source);
    }

    public void setDestination(String destination) {
        this.destination = StringUtils.trim(destination);
    }

    public String toString() {
        return MessageFormat.format("Artifact of type {0} copies from {1} to {2}", getArtifactType(), source, destination);
    }

    public ArtifactType getArtifactType() {
        return ArtifactType.build;
    }

    public String getArtifactTypeValue() {
        return ARTIFACT_PLAN_DISPLAY_NAME;
    }

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        if (!StringUtils.isBlank(destination) && (!(destination.equals(DEFAULT_ROOT.getPath()) || new FilePathTypeValidator().isPathValid(destination)))) {
            addError(DEST, "Invalid destination path. Destination path should match the pattern " + FilePathTypeValidator.PATH_PATTERN);
        }
        if (StringUtils.isBlank(source)) {
            addError(SRC, String.format("Job '%s' has an artifact with an empty source", validationContext.getJob().name()));
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public void validateUniqueness(List<ArtifactConfig> existingArtifactConfigList) {
        for (ArtifactConfig existingPlan : existingArtifactConfigList) {
            if (this.equals(existingPlan)) {
                addError(SRC, "Duplicate artifacts defined.");
                addError(DEST, "Duplicate artifacts defined.");

                existingPlan.addError(SRC, "Duplicate artifacts defined.");
                existingPlan.addError(DEST, "Duplicate artifacts defined.");
                return;
            }
        }
        existingArtifactConfigList.add(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildArtifactConfig)) return false;

        BuildArtifactConfig that = (BuildArtifactConfig) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        return destination != null ? destination.equals(that.destination) : that.destination == null;
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }
}
