/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@ConfigTag("artifact")
@ConfigCollection(value = ConfigurationProperty.class)
public class ArtifactConfig extends Configuration implements Serializable, Validatable {
    public static final String TEST_OUTPUT_FOLDER = "testoutput";
    public static final String SRC = "source";
    public static final String DEST = "destination";
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "src", allowNull = true)
    private String source;
    @ConfigAttribute(value = "dest", allowNull = true)
    private String destination;

    @ConfigAttribute(value = "id", allowNull = true)
    protected String id;
    @ConfigAttribute(value = "storeId", allowNull = true)
    private String storeId;

    @ConfigAttribute(value = "type", optional = false)
    private ArtifactType artifactType;

    public ArtifactConfig() {
    }

    public ArtifactConfig(ArtifactType artifactType, String source, String destination) {
        this.artifactType = artifactType;
        this.source = source;
        this.destination = destination;
    }

    public ArtifactConfig(String id, String storeId, ConfigurationProperty... configurationProperties) {
        super(configurationProperties);
        this.id = id;
        this.storeId = storeId;
        artifactType = ArtifactType.plugin;
    }

    public String getDestination() {
        return StringUtils.isBlank(destination) ? getDefaultFolder() : FilenameUtils.separatorsToUnix(destination);
    }

    public String getSource() {
        return FilenameUtils.separatorsToUnix(source);
    }

    public void setSource(String source) {
        this.source = StringUtils.trim(source);
    }

    public void setDestination(String destination) {
        this.destination = StringUtils.trim(destination);
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String toString() {
        return MessageFormat.format("Artifact of type {0} copies from {1} to {2}", artifactType, source, destination);
    }

    public String getArtifactTypeValue() {
        switch (artifactType) {
            case file:
                return "Build Artifact";
            case unit:
                return "Test Artifact";
            case plugin:
                return "Pluggable Artifact";
        }
        return null;
    }

    protected void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (artifactType == ArtifactType.plugin) {
            validatePluggableArtifact(validationContext);
        } else {
            validateBuiltInArtifact(validationContext);
        }
    }

    private void validatePluggableArtifact(ValidationContext validationContext) {
        super.validateUniqueness(getArtifactTypeValue());
        if (!new NameTypeValidator().isNameValid(storeId)) {
            errors.add("storeId", NameTypeValidator.errorMessage("pluggable artifact storeId", storeId));
        }

        if (isNotBlank(storeId)) {
            final ArtifactStore artifactStore = validationContext.artifactStores().find(storeId);

            if (artifactStore == null) {
                addError("storeId", String.format("Artifact store with id `%s` does not exist.", storeId));
            }
        }

        if (!new NameTypeValidator().isNameValid(id)) {
            errors.add("id", NameTypeValidator.errorMessage("pluggable artifact id", id));
        }
    }

    private void validateBuiltInArtifact(ValidationContext validationContext) {
        if (!StringUtils.isBlank(destination) && (!(destination.equals(getDefaultFolder()) || new FilePathTypeValidator().isPathValid(destination)))) {
            addError(DEST, "Invalid destination path. Destination path should match the pattern " + FilePathTypeValidator.PATH_PATTERN);
        }
        if (StringUtils.isBlank(source)) {
            addError(SRC, String.format("Job '%s' has an artifact with an empty source", validationContext.getJob().name()));
        }
    }

    public void validateUniqueness(List<ArtifactConfig> existingArtifactList) {
        for (ArtifactConfig existingArtifactConfig : existingArtifactList) {
            if (existingArtifactConfig.getArtifactType() != this.getArtifactType()) {
                continue;
            }

            if (existingArtifactConfig.getArtifactType() == ArtifactType.plugin) {
                validateUniquenessOfPluggableArtifacts(existingArtifactConfig);
            } else {
                validateUniquenessOfBuiltInArtifact(existingArtifactConfig);
            }

            if (hasErrors()) {
                return;
            }
        }
        existingArtifactList.add(this);
    }

    private void validateUniquenessOfPluggableArtifacts(ArtifactConfig existingArtifactConfig) {
        if (this.getId().equalsIgnoreCase(existingArtifactConfig.getId())) {
            this.addError("id", String.format("Duplicate pluggable artifacts  with id `%s` defined.", getId()));
            existingArtifactConfig.addError("id", String.format("Duplicate pluggable artifacts  with id `%s` defined.", getId()));
        }

        if (this.getStoreId().equalsIgnoreCase(existingArtifactConfig.getStoreId())) {
            if (this.size() == existingArtifactConfig.size() && this.containsAll(existingArtifactConfig)) {
                this.addError("id", "Duplicate pluggable artifacts  configuration defined.");
                existingArtifactConfig.addError("id", "Duplicate pluggable artifacts  configuration defined.");
            }
        }
    }

    private void validateUniquenessOfBuiltInArtifact(ArtifactConfig existingArtifactConfig) {
        if (this.equals(existingArtifactConfig)) {
            addError(SRC, "Duplicate artifacts defined.");
            addError(DEST, "Duplicate artifacts defined.");

            existingArtifactConfig.addError(SRC, "Duplicate artifacts defined.");
            existingArtifactConfig.addError(DEST, "Duplicate artifacts defined.");
        }
    }

    public String toJSON() {
        return this.artifactType.toJSON(this);
    }


    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public boolean hasErrors() {
        return super.hasErrors() || !errors.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactConfig)) return false;
        if (!super.equals(o)) return false;

        ArtifactConfig that = (ArtifactConfig) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (storeId != null ? !storeId.equals(that.storeId) : that.storeId != null) return false;
        return artifactType == that.artifactType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (storeId != null ? storeId.hashCode() : 0);
        result = 31 * result + (artifactType != null ? artifactType.hashCode() : 0);
        return result;
    }

    private String getDefaultFolder() {
        if (artifactType == ArtifactType.file) {
            return new File("").getPath();
        }

        if (artifactType == ArtifactType.unit) {
            return TEST_OUTPUT_FOLDER;
        }

        return null;
    }
}
