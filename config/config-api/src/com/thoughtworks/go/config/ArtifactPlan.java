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

import com.thoughtworks.go.config.validation.FilePathTypeValidator;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.FileUtil.normalizePath;
import static com.thoughtworks.go.util.FileUtil.subtractPath;
import static com.thoughtworks.go.util.SelectorUtils.rtrimStandardrizedWildcardTokens;
import static org.apache.commons.lang.StringUtils.removeStart;

@ConfigTag("artifact")
public class ArtifactPlan extends PersistentObject implements Artifact {
    public static final File DEFAULT_ROOT = new File("");
    public static final String SRC = "src";
    public static final String DEST = "dest";

    @ConfigAttribute(value = "src", optional = false)
    private String src;
    @ConfigAttribute("dest")
    private String dest = DEFAULT_ROOT.getPath();

    private ArtifactType artifactType = ArtifactType.file;
    private long buildId;
    private ConfigErrors errors = new ConfigErrors();
    public static final String ARTIFACT_PLAN_DISPLAY_NAME = "Build Artifact";


    public ArtifactPlan() {
    }

    protected ArtifactPlan(ArtifactType artifactType, String source) {
        setSrc(source);
        this.artifactType = artifactType;
    }

    protected ArtifactPlan(ArtifactType artifactType, String source, String destination) {
        setSrc(source);
        setDest(destination);
        this.artifactType = artifactType;
    }
    public ArtifactPlan(String source, String destination) {
        setSrc(source);
        setDest(destination);
    }

    public ArtifactPlan(ArtifactPlan other) {
        this(other.artifactType, other.src, other.dest);
        this.errors = other.errors;
    }

    public File getSource(File rootPath) {
        return new File(FileUtil.applyBaseDirIfRelativeAndNormalize(rootPath, new File(getSrc())));
    }

    public String getDest() {
        return normalizePath(dest);
    }

    public void setBuildId(long buildInstanceId) {
        this.buildId = buildInstanceId;
    }

    public void setSrc(String src) {
        this.src = StringUtils.trim(src);
    }

    public String getSrc() {
        return normalizePath(src);
    }

    public void setDest(String dest) {
        this.dest = StringUtils.trim(dest);
    }

    public boolean equals(Object other) {
        return this == other || other != null && other instanceof ArtifactPlan && equals((ArtifactPlan) other);
    }

    private boolean equals(ArtifactPlan other) {
        if (dest != null ? !dest.equals(other.dest) : other.dest != null) {
            return false;
        }
        return !(src != null ? !src.equals(other.src) : other.src != null) && artifactType.equals(other.artifactType);

    }

    public int hashCode() {
        int result = 0;
        result = 31 * result + (src != null ? src.hashCode() : 0);
        result = 31 * result + (dest != null ? dest.hashCode() : 0);
        result = 31 * result + (artifactType != null ? artifactType.hashCode() : 0);
        return result;
    }

    public void publish(GoPublisher publisher, final File rootPath) {
        WildcardScanner scanner = getArtifactSrc(rootPath);
        File[] files = scanner.getFiles();
        if (files.length == 0) {
            String message = "The rule [" + getSrc() + "] cannot match any resource under [" + rootPath + "]";
            publisher.consumeLineWithPrefix(message);
            throw new RuntimeException(message);
        }
        for (File file : files) {
            publishWithProperties(publisher, file, rootPath);
        }
    }

    private WildcardScanner getArtifactSrc(File rootPath) {
        return new WildcardScanner(rootPath, getSrc());
    }

    protected void publishWithProperties(GoPublisher goPublisher, File fileToUpload, File rootPath) {
        goPublisher.upload(fileToUpload, destURL(rootPath, fileToUpload));
    }

    public String toString() {
        return MessageFormat.format("Artifact of type {0} copies from {1} to {2}", artifactType, src, dest);
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    protected void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public String getArtifactTypeValue() {
        return ARTIFACT_PLAN_DISPLAY_NAME;
    }

    public String destURL(File rootPath, File file) {
        return destURL(rootPath, file, getSrc(), getDest());
    }

    protected String destURL(File rootPath, File file, String src, String dest) {
        String trimmedPattern = rtrimStandardrizedWildcardTokens(src);
        if (StringUtils.equals(normalizePath(trimmedPattern), normalizePath(src))) {
            return dest;
        }
        String trimmedPath = removeStart(subtractPath(rootPath, file), normalizePath(trimmedPattern));
        if (!StringUtils.startsWith(trimmedPath, "/") && StringUtils.isNotEmpty(trimmedPath)) {
            trimmedPath = "/" + trimmedPath;
        }
        return dest + trimmedPath;
    }

    public void printSrc(StringBuilder builder) {
        builder.append('[').append(getSrc()).append(']');
    }

    public String effectiveDestinationPath() {
        File src = new File(getSrc());
        return normalizePath(StringUtils.isEmpty(getDest()) ? src.getName() : new File(getDest(), src.getName()).getPath());
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        if (!StringUtil.isBlank(dest) && (!(dest.equals(DEFAULT_ROOT.getPath()) || new FilePathTypeValidator().isPathValid(dest)))) {
            addError(DEST, "Invalid destination path. Destination path should match the pattern " + FilePathTypeValidator.PATH_PATTERN);
        }
        if (StringUtil.isBlank(src)) {
            addError(SRC, String.format("Job '%s' has an artifact with an empty source", validationContext.getJob().name()));
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public void validateUniqueness(List<ArtifactPlan> existingPlans) {
        for (ArtifactPlan existingPlan : existingPlans) {
            if (this.equals(existingPlan)) {
                this.addUniquenessViolationError();
                existingPlan.addUniquenessViolationError();
                return;
            }
        }
        existingPlans.add(this);
    }

    private void addUniquenessViolationError() {
        addError(SRC, "Duplicate artifacts defined.");
        addError(DEST, "Duplicate artifacts defined.");
    }

    public static ArtifactPlan create(ArtifactType artifactType, String src, String dest) {
        if (artifactType == ArtifactType.file) {
            return new ArtifactPlan(src, dest);
        } else if (artifactType == ArtifactType.unit) {
            return new TestArtifactPlan(src, dest);
        } else {
            throw bomb("ArtifactType not specified");
        }
    }
}
