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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.ArtifactUrlReader;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@Service
public class ArtifactsService implements ArtifactUrlReader {
    private final ArtifactsDirHolder artifactsDirHolder;
    private final ZipUtil zipUtil;
    private final JobResolverService jobResolverService;
    private final StageDao stageDao;
    public static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsService.class);
    public static final String LOG_XML_NAME = "log.xml";
    private ArtifactDirectoryChooser chooser;

    @Autowired
    public ArtifactsService(JobResolverService jobResolverService, StageDao stageDao,
                            ArtifactsDirHolder artifactsDirHolder, ZipUtil zipUtil) {
        this(jobResolverService, stageDao, artifactsDirHolder, zipUtil, new ArtifactDirectoryChooser());
    }

    protected ArtifactsService(JobResolverService jobResolverService, StageDao stageDao,
                               ArtifactsDirHolder artifactsDirHolder, ZipUtil zipUtil, ArtifactDirectoryChooser chooser) {
        this.artifactsDirHolder = artifactsDirHolder;
        this.zipUtil = zipUtil;
        this.jobResolverService = jobResolverService;
        this.stageDao = stageDao;

        //This is a Chain of Responsibility to decide which view should be shown for a particular artifact URL
        this.chooser = chooser;

    }

    public void initialize() {
        chooser.add(new PathBasedArtifactsLocator(artifactsDirHolder.getArtifactsDir()));
        chooser.add(new BuildIdArtifactLocator(artifactsDirHolder.getArtifactsDir()));
    }

    public boolean saveFile(File dest, InputStream stream, boolean shouldUnzip, int attempt) {
        String destPath = dest.getAbsolutePath();
        try {
            LOGGER.trace("Saving file [{}]", destPath);
            if (shouldUnzip) {
                zipUtil.unzip(new ZipInputStream(stream), dest);
            } else {
                try (FileOutputStream out = FileUtils.openOutputStream(dest, true)) {
                    IOUtils.copyLarge(stream, out);
                }
            }
            LOGGER.trace("File [{}] saved.", destPath);
            return true;
        } catch (IOException e) {
            final String message = format("Failed to save the file to: [%s]", destPath);
            if (attempt < GoConstants.PUBLISH_MAX_RETRIES) {
                LOGGER.warn(message, e);
            } else {
                LOGGER.error(message, e);
            }
            return false;
        } catch (IllegalPathException e) {
            final String message = format("Failed to save the file to: [%s]", destPath);
            LOGGER.error(message, e);
            return false;
        }
    }

    public boolean saveOrAppendFile(File dest, InputStream stream) {
        String destPath = dest.getAbsolutePath();
        try {
            LOGGER.trace("Appending file [{}]", destPath);
            try (FileOutputStream out = FileUtils.openOutputStream(dest, true)) {
                IOUtils.copyLarge(stream, out);
            }
            LOGGER.trace("File [{}] appended.", destPath);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save the file to : [{}]", destPath, e);
            return false;
        }
    }

    public File findArtifact(JobIdentifier identifier, String path) throws IllegalArtifactLocationException {
        return chooser.findArtifact(identifier, path);
    }

    @Override
    public String findArtifactRoot(JobIdentifier identifier) throws IllegalArtifactLocationException {
        JobIdentifier id = jobResolverService.actualJobIdentifier(identifier);
        try {
            String fullArtifactPath = chooser.findArtifact(id, "").getCanonicalPath();
            String artifactRoot = artifactsDirHolder.getArtifactsDir().getCanonicalPath();
            String relativePath = fullArtifactPath.replace(artifactRoot, "");
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.replaceFirst("\\" + File.separator, "");
            }
            return relativePath;
        } catch (IOException e) {
            throw new IllegalArtifactLocationException("No artifact found.", e);
        }
    }

    @Override
    public String findArtifactUrl(JobIdentifier jobIdentifier) {
        JobIdentifier actualId = jobResolverService.actualJobIdentifier(jobIdentifier);
        return format("/files/%s", actualId.buildLocator());
    }

    public String findArtifactUrl(JobIdentifier jobIdentifier, String path) {
        return format("%s/%s", findArtifactUrl(jobIdentifier), path);
    }

    public File getArtifactLocation(String path) throws IllegalArtifactLocationException {
        try {
            File file = new File(artifactsDirHolder.getArtifactsDir(), path);
            if (!FileUtil.isSubdirectoryOf(artifactsDirHolder.getArtifactsDir(), file)) {
                throw new IllegalArtifactLocationException("Illegal artifact path " + path);
            }
            return file;
        } catch (Exception e) {
            throw new IllegalArtifactLocationException("Illegal artifact path " + path);
        }
    }

    public void purgeOldArtifacts(int purgeThreshold, HttpLocalizedOperationResult result) {
        List<Stage> stages = stageDao.oldestStagesHavingArtifacts();
        for (Stage stage : stages) {
            purgeOldArtifactsForStage(purgeThreshold, stage, result);
        }
        if (result.isSuccessful()) {
            result.setMessage("eligible artifacts purged");
        }
    }

    private void purgeOldArtifactsForStage(int purgeThreshold, Stage stage, HttpLocalizedOperationResult result) {
        StageIdentifier stageIdentifier = stage.getIdentifier();
        try {
            File stageRoot = chooser.findArtifact(stageIdentifier, "");
            int existing = deleteOldArtifacts(purgeThreshold, stageIdentifier, stageRoot);
            if (existing == 0) {
                stageDao.markArtifactsDeletedFor(stage);
                LOGGER.debug("Marked stage '{}' as artifacts deleted.", stageIdentifier.entityLocator());
            }
        } catch (Exception e) {
            result.internalServerError(String.format("Error occurred while clearing old artifacts for stage %s: %s. Check the logs for more information.", stage.getIdentifier(), e.getMessage()));
            LOGGER.error("Error occurred while clearing artifacts for '{}'. Error: '{}'", stageIdentifier.entityLocator(), e.getMessage(), e);
        }
    }

    public void purgeArtifactsForStage(Stage stage) {
        StageIdentifier stageIdentifier = stage.getIdentifier();
        try {
            File stageRoot = chooser.findArtifact(stageIdentifier, "");
            File cachedStageRoot = chooser.findCachedArtifact(stageIdentifier);
            deleteFile(cachedStageRoot);
            boolean didDelete = deleteArtifactsExceptCruiseOutputAndPluggableArtifactMetadata(stageRoot);

            if (!didDelete) {
                LOGGER.error("Artifacts for stage '{}' at path '{}' was not deleted", stageIdentifier.entityLocator(), stageRoot.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while clearing artifacts for '{}'. Error: '{}'", stageIdentifier.entityLocator(), e.getMessage(), e);
        }
        stageDao.markArtifactsDeletedFor(stage);
        LOGGER.debug("Marked stage '{}' as artifacts deleted.", stageIdentifier.entityLocator());
    }

    private int deleteOldArtifacts(int purgeThreshold, StageIdentifier stageIdentifier, File stageRoot) throws IOException {
        File[] jobs = stageRoot.listFiles();
        if (jobs == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + stageRoot);
        }
        int deleted = 0;
        int userArtifacts = 0;

        for (File jobRoot : jobs) {
            File[] artifacts = jobRoot.listFiles();
            if (artifacts == null) {  // null if security restricted
                throw new IOException("Failed to list contents of " + stageRoot);
            }

            for (File artifact : artifacts) {
                if (artifact.isDirectory() && (artifact.getName().equals(ArtifactLogUtil.CRUISE_OUTPUT_FOLDER) || artifact.getName().equals(ArtifactLogUtil.PLUGGABLE_ARTIFACT_METADATA_FOLDER))) {
                    continue;
                }

                userArtifacts++;

                var lastModified = new DateTime(artifact.lastModified());
                if (lastModified.isAfter(new DateTime().minusHours(purgeThreshold))) {
                    continue;
                }
                if (deleteFile(artifact)) {
                 deleted++;
                } else {
                    LOGGER.error("An old artifact for stage '{}' at path '{}' was not deleted", stageIdentifier.entityLocator(), stageRoot.getAbsolutePath());
                }
            }

        }
        return userArtifacts - deleted;
    }

    private boolean deleteArtifactsExceptCruiseOutputAndPluggableArtifactMetadata(File stageRoot) throws IOException {
        File[] jobs = stageRoot.listFiles();
        if (jobs == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + stageRoot);
        }

        boolean didDelete = true;

        for (File jobRoot : jobs) {
            File[] artifacts = jobRoot.listFiles();
            if (artifacts == null) {  // null if security restricted
                throw new IOException("Failed to list contents of " + stageRoot);
            }
            for (File artifact : artifacts) {
                if (artifact.isDirectory() && (artifact.getName().equals(ArtifactLogUtil.CRUISE_OUTPUT_FOLDER) || artifact.getName().equals(ArtifactLogUtil.PLUGGABLE_ARTIFACT_METADATA_FOLDER))) {
                    continue;
                }
                didDelete &= deleteFile(artifact);
            }
        }
        return didDelete;
    }

    private boolean deleteFile(File file) {
        return FileUtils.deleteQuietly(file);
    }

}
