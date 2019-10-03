/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public abstract class BuiltinArtifactConfig implements ArtifactTypeConfig {

    public static final String SRC = "source";
    public static final String DEST = "destination";
    static final File DEFAULT_ROOT = new File("");

    @ConfigAttribute(value = "src", optional = false)
    protected String source;
    @ConfigAttribute("dest")
    protected String destination = DEFAULT_ROOT.getPath();

    protected ConfigErrors errors = new ConfigErrors();

    public abstract String getDestination();

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (!StringUtils.isBlank(destination) && (!(destination.equals(DEFAULT_ROOT.getPath()) || new FilePathTypeValidator().isPathValid(destination)))) {
            addError(DEST, "Invalid destination path. Destination path should match the pattern " + FilePathTypeValidator.PATH_PATTERN);
        }
        if (StringUtils.isBlank(source)) {
            addError(SRC, String.format("Job '%s' has an artifact with an empty source", validationContext.getJob().name()));
        }
    }

    @Override
    public void validateUniqueness(List<ArtifactTypeConfig> existingArtifactTypeConfigList) {
        for (ArtifactTypeConfig existingPlan : existingArtifactTypeConfigList) {
            if (this.equals(existingPlan)) {
                addError(SRC, "Duplicate artifacts defined.");
                addError(DEST, "Duplicate artifacts defined.");

                existingPlan.addError(SRC, "Duplicate artifacts defined.");
                existingPlan.addError(DEST, "Duplicate artifacts defined.");
                return;
            }
        }
        existingArtifactTypeConfigList.add(this);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public void setSource(String source) {
        this.source = StringUtils.trim(source);
    }

    public String getSource() {
        return FilenameUtils.separatorsToUnix(source);
    }

    public void setDestination(String destination) {
        if (StringUtils.isNotBlank(destination)) {
            this.destination = StringUtils.trim(destination);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuiltinArtifactConfig that = (BuiltinArtifactConfig) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + getArtifactType().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return MessageFormat.format("Artifact of type {0} copies from {1} to {2}", getArtifactType(), getSource(), getDestination());
    }
}
