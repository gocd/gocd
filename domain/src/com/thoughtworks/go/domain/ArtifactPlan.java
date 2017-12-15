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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ArtifactConfig;
import com.thoughtworks.go.config.ArtifactConfigs;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.FileUtil.subtractPath;
import static com.thoughtworks.go.util.SelectorUtils.rtrimStandardrizedWildcardTokens;
import static org.apache.commons.lang.StringUtils.removeStart;

public class ArtifactPlan extends PersistentObject {
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactPlan.class);
    private long buildId;
    private ArtifactType artifactType;
    private String src;
    private String dest;
    private static final String MERGED_TEST_RESULT_FOLDER = "result";
    protected final List<ArtifactPlan> testArtifactPlansForMerging = new ArrayList<>();

    public ArtifactPlan() {
    }

    public ArtifactPlan(ArtifactConfig artifactConfig) {
        this(artifactConfig.getArtifactType(), artifactConfig.getSource(), artifactConfig.getDestination());
    }

    public ArtifactPlan(ArtifactPlan artifactPlan) {
        this(artifactPlan.artifactType, artifactPlan.src, artifactPlan.dest);
    }

    public ArtifactPlan(ArtifactType artifactType, String src, String dest) {
        this.artifactType = artifactType;
        setSrc(src);
        setDest(dest);
    }

    public long getBuildId() {
        return buildId;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public String getSrc() {
        return FilenameUtils.separatorsToUnix(src);
    }

    public String getDest() {
        return FilenameUtils.separatorsToUnix(dest);
    }

    public void setBuildId(long buildId) {
        this.buildId = buildId;
    }

    public void setArtifactType(ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public void setSrc(String src) {
        this.src = StringUtils.trim(src);
    }

    public void setDest(String dest) {
        this.dest = StringUtils.trim(dest);
    }

    public void printArtifactInfo(StringBuilder builder) {
        builder.append('[').append(getSrc()).append(']');
    }

    public void publish(GoPublisher publisher, final File rootPath) {
        switch (artifactType) {
            case unit:
                publishTestArtifact(publisher, rootPath);
                break;
            case file:
                publishBuildArtifact(publisher, rootPath);
                break;
        }
    }

    private void publishBuildArtifact(GoPublisher publisher, File rootPath) {
        File[] files = getArtifactFiles(rootPath, ArtifactPlan.this);
        if (files.length == 0) {
            String message = "The rule [" + getSrc() + "] cannot match any resource under [" + rootPath + "]";
            publisher.taggedConsumeLineWithPrefix(GoPublisher.PUBLISH_ERR, message);
            throw new RuntimeException(message);
        }
        uploadArtifactFile(publisher, rootPath, getSrc(), getDest(), files);
    }

    private void publishTestArtifact(GoPublisher goPublisher, File rootPath) {
        mergeAndUploadTestResult(goPublisher, uploadTestResults(goPublisher, rootPath));
    }

    public List<File> uploadTestResults(GoPublisher publisher, File rootPath) {
        List<File> allFiles = new ArrayList<>();
        for (ArtifactPlan artifactPlan : testArtifactPlansForMerging) {
            File[] files = getArtifactFiles(rootPath, artifactPlan);
            if (files.length > 0) {
                allFiles.addAll(uploadArtifactFile(publisher, rootPath, artifactPlan.getSrc(), artifactPlan.getDest(), files));
            } else {
                final String message = MessageFormat.format("The Directory {0} specified as a test artifact was not found."
                        + " Please check your configuration", FilenameUtils.separatorsToUnix(artifactPlan.getSource(rootPath).getPath()));
                publisher.taggedConsumeLineWithPrefix(GoPublisher.PUBLISH_ERR, message);
                LOG.error(message);
            }
        }
        return allFiles;
    }

    private List<File> uploadArtifactFile(GoPublisher publisher, File rootPath, String src, String dest, File[] files) {
        final List<File> fileList = files == null ? new ArrayList<>() : Arrays.asList(files);
        for (File file : fileList) {
            publisher.upload(file, destinationURL(rootPath, file, src, dest));
        }
        return fileList;
    }

    private File[] getArtifactFiles(File rootPath, ArtifactPlan artifactPlan) {
        WildcardScanner wildcardScanner = new WildcardScanner(rootPath, artifactPlan.getSrc());
        return wildcardScanner.getFiles();
    }

    private void mergeAndUploadTestResult(GoPublisher publisher, List<File> allFiles) {
        if (allFiles.size() > 0) {
            File tempFolder = null;
            try {
                tempFolder = FileUtil.createTempFolder();
                File testResultSource = new File(tempFolder, MERGED_TEST_RESULT_FOLDER);
                testResultSource.mkdirs();
                UnitTestReportGenerator generator = new UnitTestReportGenerator(publisher, testResultSource);
                generator.generate(allFiles.toArray(new File[allFiles.size()]), "testoutput");
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

    protected File getSource(File rootPath) {
        return new File(FileUtil.applyBaseDirIfRelativeAndNormalize(rootPath, new File(getSrc())));
    }

    public String destinationURL(File rootPath, File file) {
        return destinationURL(rootPath, file, getSrc(), getDest());
    }

    protected String destinationURL(File rootPath, File file, String src, String dest) {
        String trimmedPattern = rtrimStandardrizedWildcardTokens(src);
        if (StringUtils.equals(FilenameUtils.separatorsToUnix(trimmedPattern), FilenameUtils.separatorsToUnix(src))) {
            return dest;
        }
        String trimmedPath = removeStart(subtractPath(rootPath, file), FilenameUtils.separatorsToUnix(trimmedPattern));
        if (!StringUtils.startsWith(trimmedPath, "/") && StringUtils.isNotEmpty(trimmedPath)) {
            trimmedPath = "/" + trimmedPath;
        }
        return dest + trimmedPath;
    }


    public static List<ArtifactPlan> toArtifactPlans(ArtifactConfigs artifactConfigs) {
        List<ArtifactPlan> artifactPlans = new ArrayList<>();
        for (ArtifactConfig artifactConfig : artifactConfigs) {
            artifactPlans.add(new ArtifactPlan(artifactConfig));
        }
        return artifactPlans;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactPlan)) return false;

        ArtifactPlan that = (ArtifactPlan) o;

        if (artifactType != that.artifactType) return false;
        if (src != null ? !src.equals(that.src) : that.src != null) return false;
        return dest != null ? dest.equals(that.dest) : that.dest == null;
    }

    @Override
    public int hashCode() {
        int result = artifactType != null ? artifactType.hashCode() : 0;
        result = 31 * result + (src != null ? src.hashCode() : 0);
        result = 31 * result + (dest != null ? dest.hashCode() : 0);
        return result;
    }
}
