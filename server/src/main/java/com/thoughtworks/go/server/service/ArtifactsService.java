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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.ArtifactUrlReader;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import com.thoughtworks.go.util.ArtifactUtil;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.IllegalPathException;
import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@Service
public class ArtifactsService implements ArtifactUrlReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsService.class);
    static final int PUBLISH_MAX_RETRIES = 3;

    private final ArtifactsDirHolder artifactsDirHolder;
    private final ZipUtil zipUtil;
    private final JobResolverService jobResolverService;
    private final StageDao stageDao;
    private final ArtifactDirectoryChooser chooser;

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
                zipUtil.unzip(new ZipInputStream(new BufferedInputStream(stream)), dest);
            } else {
                try (FileOutputStream out = FileUtils.openOutputStream(dest, true)) {
                    stream.transferTo(out);
                }
            }
            LOGGER.trace("File [{}] saved.", destPath);
            return true;
        } catch (IOException e) {
            final String message = format("Failed to save the file to: [%s]", destPath);
            if (attempt < PUBLISH_MAX_RETRIES) {
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
                stream.transferTo(out);
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

    public void purgeArtifactsForStage(Stage stage) {
        StageIdentifier stageIdentifier = stage.getIdentifier();
        try {
            File stageRoot = chooser.findArtifact(stageIdentifier, "");
            FileUtils.deleteQuietly(chooser.findCachedArtifact(stageIdentifier));

            if (!deleteNonSystemManagedArtifacts(stageRoot)) {
                LOGGER.error("Some artifacts for stage '{}' at path '{}' was not successfully deleted", stageIdentifier.entityLocator(), stageRoot.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while clearing artifacts for '{}'. Error: '{}'", stageIdentifier.entityLocator(), e.getMessage(), e);
        }
        stageDao.markArtifactsDeletedFor(stage);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Marked stage '{}' as artifacts deleted.", stageIdentifier.entityLocator());
        }
    }

    /**
     * Purges everything that is stored for a stage rather than only its artifacts. This removes the directory of the
     * stage along with the console logs and pluggable artifact metadata of its jobs, as well as any directories of the
     * pipeline instance that are left empty as a result. The directories are removed even if the artifacts of the stage
     * were purged earlier, which makes it safe to call this repeatedly for the same stage.
     *
     * @return true if there was a directory for the stage to remove
     */
    public boolean purgeArtifactDirectoriesForStage(Stage stage) {
        StageIdentifier stageIdentifier = stage.getIdentifier();
        boolean purged = false;
        try {
            deleteDirectoryAndEmptyParents(chooser.preferredCachedArtifact(stageIdentifier));
            purged = deleteDirectoryAndEmptyParents(chooser.preferredRoot(stageIdentifier));
        } catch (Exception e) {
            LOGGER.error("Error occurred while clearing artifact directories for '{}'. Error: '{}'", stageIdentifier.entityLocator(), e.getMessage(), e);
        }
        if (!stage.isArtifactsDeleted()) {
            stageDao.markArtifactsDeletedFor(stage);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Marked stage '{}' as artifacts deleted.", stageIdentifier.entityLocator());
            }
        }
        return purged;
    }

    private static boolean deleteDirectoryAndEmptyParents(File stageDirectory) {
        if (stageDirectory == null) {
            return false;
        }
        File stageNameDirectory = stageDirectory.getParentFile();
        File pipelineCounterDirectory = stageNameDirectory.getParentFile();
        if (!pipelineCounterDirectory.exists()) {
            return false;
        }
        boolean exists = stageDirectory.exists();
        if (exists && !FileUtils.deleteQuietly(stageDirectory)) {
            LOGGER.error("Directory '{}' was not successfully deleted", stageDirectory.getAbsolutePath());
        }
        // These are shared with the other stage runs of the pipeline instance, so they only go once the last of those is gone
        deleteIfEmpty(stageNameDirectory);
        deleteIfEmpty(pipelineCounterDirectory);
        return exists;
    }

    private static void deleteIfEmpty(File directory) {
        // File#delete leaves a directory alone unless it is empty, but would remove a link to a directory regardless of its contents
        if (!Files.isSymbolicLink(directory.toPath())) {
            directory.delete();
        }
    }

    private boolean deleteNonSystemManagedArtifacts(File stageRoot) throws IOException {
        File[] jobs = stageRoot.listFiles();
        if (jobs == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + stageRoot);
        }

        boolean deletePartiallyFailed = false;

        for (File jobRoot : jobs) {
            File[] artifacts = jobRoot.listFiles();
            if (artifacts == null) {  // null if security restricted
                throw new IOException("Failed to list contents of " + stageRoot);
            }
            for (File artifact : artifacts) {
                if (shouldDeleteArtifact(artifact) && !FileUtils.deleteQuietly(artifact)) {
                    deletePartiallyFailed = true;
                }
            }
        }
        return !deletePartiallyFailed;
    }

    private static boolean shouldDeleteArtifact(File artifact) {
        return !artifact.isDirectory() || !ArtifactUtil.artifactDirectoryIsSystemManaged(artifact.getName());
    }

}
