/*
 * Copyright Thoughtworks, Inc.
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

import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.ArtifactTypeConfig;
import com.thoughtworks.go.config.ArtifactTypeConfigs;
import com.thoughtworks.go.config.BuiltinArtifactConfig;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.FilenameUtil;
import com.thoughtworks.go.util.json.JsonHelper;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.lang3.Strings.CS;

public class ArtifactPlan extends PersistentObject {
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactPlan.class);
    private static final String MERGED_TEST_RESULT_FOLDER = "result";

    protected final List<ArtifactPlan> testArtifactPlansForMerging = new ArrayList<>();

    private long buildId;
    private ArtifactPlanType artifactPlanType;
    private String src;
    private String dest;
    private String pluggableArtifactConfigJson;

    public ArtifactPlan() {
    }

    public ArtifactPlan(ArtifactTypeConfig artifactTypeConfig) {
        this.artifactPlanType = ArtifactPlanType.fromArtifactType(artifactTypeConfig.getArtifactType());
        if (artifactTypeConfig instanceof PluggableArtifactConfig pluggableArtifactConfig) {
            this.pluggableArtifactConfigJson = pluggableArtifactConfig.toJSON();
        } else {
            BuiltinArtifactConfig buildArtifactConfig = (BuiltinArtifactConfig) artifactTypeConfig;
            setSrc(buildArtifactConfig.getSource());
            setDest(buildArtifactConfig.getDestination());
        }
    }

    public ArtifactPlan(long buildId, ArtifactPlan artifactPlan) {
        this(artifactPlan.artifactPlanType, artifactPlan.src, artifactPlan.dest);
        this.pluggableArtifactConfigJson = artifactPlan.pluggableArtifactConfigJson;
        this.buildId = buildId;
    }

    public ArtifactPlan(ArtifactPlanType artifactType, String src, String dest) {
        this.artifactPlanType = artifactType;
        setSrc(src);
        setDest(dest);
    }

    public ArtifactPlan(String pluggableArtifactConfigJson) {
        artifactPlanType = ArtifactPlanType.external;
        this.pluggableArtifactConfigJson = pluggableArtifactConfigJson;
    }

    public long getBuildId() {
        return buildId;
    }

    public ArtifactPlanType getArtifactPlanType() {
        return artifactPlanType;
    }

    public String getSrc() {
        return separatorsToUnix(src);
    }

    public String getDest() {
        return separatorsToUnix(dest);
    }

    public void setBuildId(long buildId) {
        this.buildId = buildId;
    }

    public void setArtifactPlanType(ArtifactPlanType artifactType) {
        this.artifactPlanType = artifactType;
    }

    public void setSrc(String src) {
        this.src = src == null ? null : src.trim();
    }

    public void setDest(String dest) {
        this.dest = dest == null ? null : dest.trim();
    }

    public void printArtifactInfo(StringBuilder builder) {
        builder.append('[');
        switch (artifactPlanType) {
            case file, unit -> builder.append(getSrc());
            case external -> builder.append(getPluggableArtifactConfiguration().get("id"));
        }
        builder.append(']');
    }

    public void publishBuiltInArtifacts(GoPublisher publisher, final File rootPath) {
        switch (artifactPlanType) {
            case unit -> publishTestArtifact(publisher, rootPath);
            case file -> publishBuildArtifact(publisher, rootPath);
        }
    }

    private void publishBuildArtifact(GoPublisher publisher, File rootPath) {
        List<File> files = getArtifactFiles(rootPath);
        if (files.isEmpty()) {
            String message = "The rule [" + getSrc() + "] cannot match any resource under [" + rootPath + "]";
            publisher.taggedConsumeLineWithPrefix(GoPublisher.PUBLISH_ERR, message);
            throw new RuntimeException(message);
        }
        uploadArtifactFiles(publisher, rootPath, files);
    }

    private void publishTestArtifact(GoPublisher goPublisher, File rootPath) {
        mergeAndUploadTestResult(goPublisher, uploadTestResults(goPublisher, rootPath));
    }

    private List<File> uploadTestResults(GoPublisher publisher, File rootPath) {
        return testArtifactPlansForMerging.stream()
            .flatMap(testPlan -> testPlan.uploadTestResult(publisher, rootPath).stream())
            .toList();
    }

    private List<File> uploadTestResult(GoPublisher publisher, File rootPath) {
        List<File> files = getArtifactFiles(rootPath);

        if (files.isEmpty()) {
            final String message = MessageFormat.format("The directory {0} specified as a test artifact was not found."
                    + " Please check your configuration", separatorsToUnix(getSource(rootPath).getPath()));
            publisher.taggedConsumeLineWithPrefix(GoPublisher.PUBLISH_ERR, message);
            LOG.warn(message);
            return Collections.emptyList();
        }

        uploadArtifactFiles(publisher, rootPath, files);
        return files;
    }

    private void uploadArtifactFiles(GoPublisher publisher, File rootPath, List<File> files) {
        for (File file : files) {
            publisher.upload(file, destinationUrl(rootPath, file));
        }
    }

    private List<File> getArtifactFiles(File rootPath) {
        return List.of(new WildcardScanner(rootPath, getSrc()).getFiles());
    }

    private void mergeAndUploadTestResult(GoPublisher publisher, List<File> allFiles) {
        if (!allFiles.isEmpty()) {
            File tempFolder = null;
            try {
                tempFolder = FileUtil.createTempFolder();
                File testResultSource = new File(tempFolder, MERGED_TEST_RESULT_FOLDER);
                testResultSource.mkdirs();
                UnitTestReportGenerator generator = new UnitTestReportGenerator(publisher, testResultSource);
                generator.generate(allFiles.toArray(new File[0]), "testoutput");
                publisher.upload(testResultSource, "testoutput");
            } finally {
                if (tempFolder != null) {
                    FileUtils.deleteQuietly(tempFolder);
                }
            }

        } else {
            String message = "No files were found in the Test Results folders";
            publisher.taggedConsumeLineWithPrefix(GoPublisher.PUBLISH_ERR, message);
            LOG.warn(message);
        }
    }

    private File getSource(File rootPath) {
        return new File(FilenameUtil.applyBaseDirIfRelativeAndNormalize(rootPath, new File(getSrc())));
    }

    @VisibleForTesting
    String destinationUrl(File rootPath, File file) {
        String unixSrc = getSrc();
        String unixSrcBeforePattern = removeAntPatternFromEnd(unixSrc);

        if (unixSrcBeforePattern.equals(unixSrc)) {
            return getDest();
        }

        // Get the bits before any pattern and map relative to dest
        String destRelativeUnixPath = CS.removeStart(toUnixPathWithoutRoot(file, rootPath), unixSrcBeforePattern);
        return destRelativeUnixPath.isEmpty() || destRelativeUnixPath.charAt(0) == '/'
            ? getDest() + destRelativeUnixPath
            : getDest() + "/" + destRelativeUnixPath;
    }

    private static @NotNull String removeAntPatternFromEnd(String unixPattern) {
        String prefix = Arrays.stream(StringUtils.split(unixPattern, '/'))
            .takeWhile(token -> !StringUtils.containsAny(token, '*', '?'))
            .collect(Collectors.joining("/"));

        // Restore any leading `/` removed by the split since these seem to be allowed
        return unixPattern.startsWith("/") ? "/" + prefix : prefix;
    }

    private static @NotNull String toUnixPathWithoutRoot(File file, File rootPath) {
        String fullPath = separatorsToUnix(file.getParentFile().getPath());
        String basePath = separatorsToUnix(rootPath.getPath());
        return CS.removeStart(CS.removeStart(fullPath, basePath), "/");
    }

    public static List<ArtifactPlan> toArtifactPlans(ArtifactTypeConfigs artifactConfigs) {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        for (ArtifactTypeConfig artifactTypeConfig : artifactConfigs) {
            artifactPlans.add(new ArtifactPlan(artifactTypeConfig));
        }
        return artifactPlans;
    }

    public Map<String, Object> getPluggableArtifactConfiguration() {
        return JsonHelper.fromJson(pluggableArtifactConfigJson, new TypeToken<Map<String, Object>>() {}.getType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ArtifactPlan that &&
            artifactPlanType == that.artifactPlanType &&
            Objects.equals(src, that.src) &&
            Objects.equals(dest, that.dest) &&
            Objects.equals(pluggableArtifactConfigJson, that.pluggableArtifactConfigJson);

    }

    @Override
    public int hashCode() {
        int result = artifactPlanType != null ? artifactPlanType.hashCode() : 0;
        result = 31 * result + (src != null ? src.hashCode() : 0);
        result = 31 * result + (dest != null ? dest.hashCode() : 0);
        result = 31 * result + (pluggableArtifactConfigJson != null ? pluggableArtifactConfigJson.hashCode() : 0);
        return result;
    }
}
