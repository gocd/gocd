/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.ArtifactUrlReader;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.legacywrapper.LogParser;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.LogFile;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@Service
public class ArtifactsService implements ArtifactUrlReader {
    private final ArtifactsDirHolder artifactsDirHolder;
    private final ZipUtil zipUtil;
    private final JobResolverService jobResolverService;
    private final StageDao stageDao;
    private SystemService systemService;
    @Autowired
    private LogParser logParser;
    public static final Logger LOGGER = Logger.getLogger(ArtifactsService.class);
    public static final String LOG_XML_NAME = "log.xml";
    private ArtifactDirectoryChooser chooser;

    @Autowired
    public ArtifactsService(JobResolverService jobResolverService, StageDao stageDao,
                            ArtifactsDirHolder artifactsDirHolder, ZipUtil zipUtil, SystemService systemService) {
        this(jobResolverService, stageDao, artifactsDirHolder, zipUtil, systemService, new ArtifactDirectoryChooser());
    }

    protected ArtifactsService(JobResolverService jobResolverService, StageDao stageDao,
                               ArtifactsDirHolder artifactsDirHolder, ZipUtil zipUtil, SystemService systemService, ArtifactDirectoryChooser chooser) {
        this.artifactsDirHolder = artifactsDirHolder;
        this.zipUtil = zipUtil;
        this.jobResolverService = jobResolverService;
        this.stageDao = stageDao;
        this.systemService = systemService;

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
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Saving file [" + destPath + "]");
            }
            if (shouldUnzip) {
                zipUtil.unzip(new ZipInputStream(stream), dest);
            } else {
                systemService.streamToFile(stream, dest);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File [" + destPath + "] saved.");
            }
            return true;
        } catch (IOException e) {
            final String message = format("Failed to save the file to: [%s]", destPath);
            if (attempt < GoConstants.PUBLISH_MAX_RETRIES) {
                LOGGER.warn(message, e);
            } else {
                LOGGER.error(message, e);
            }
            return false;
        } catch (IllegalPathException e){
            final String message = format("Failed to save the file to: [%s]", destPath);
            LOGGER.error(message, e);
            return false;
        }
    }

    public boolean saveOrAppendFile(File dest, InputStream stream) {
        String destPath = dest.getAbsolutePath();
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Appending file [" + destPath + "]");
            }
            systemService.streamToFile(stream, dest);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File [" + destPath + "] appended.");
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save the file to : [" + destPath + "]", e);
            return false;
        }
    }

    public LogFile getInstanceLogFile(JobIdentifier jobIdentifier) throws IllegalArtifactLocationException {
        File outputFolder = findArtifact(jobIdentifier, ArtifactLogUtil.CRUISE_OUTPUT_FOLDER);
        return new LogFile(new File(outputFolder, LOG_XML_NAME));
    }

    public Map parseLogFile(LogFile logFile, boolean buildPassed) throws ArtifactsParseException {
        try {
            Map properties;
            File cacheFile = serializedPropertiesFile(logFile.getFile());
            if (cacheFile.exists()) {
                properties = (Map) ObjectUtil.readObject(cacheFile);
            } else if (logFile.getFile().exists()) {
                properties = logParser.parseLogFile(logFile, buildPassed);
                ObjectUtil.writeObject(properties, cacheFile);
            } else {
                properties = new HashMap();
            }
            return properties;
        } catch (Exception e) {
            LOGGER.error("Error parsing log file: ", e);
            String filePath = logFile == null ? "null log file" : logFile.getFile().getPath();
            throw new ArtifactsParseException("Error parsing log file: " + filePath, e);
        }

    }

    public File serializedPropertiesFile(File logFile) {
        return new File(logFile.getParent(), "." + logFile.getName() + ".ser");
    }

    public File findArtifact(JobIdentifier identifier, String path) throws IllegalArtifactLocationException {
        return chooser.findArtifact(identifier, path);
    }

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
            File cachedStageRoot = chooser.findCachedArtifact(stageIdentifier);
            deleteFile(cachedStageRoot);
            boolean didDelete = deleteArtifactsExceptCruiseOutput(stageRoot);

            if (!didDelete) {
                LOGGER.error(format("Artifacts for stage '%s' at path '%s' was not deleted", stageIdentifier.entityLocator(), stageRoot.getAbsolutePath()));
            }
        } catch (Exception e) {
            LOGGER.error(format("Error occurred while clearing artifacts for '%s'. Error: '%s'", stageIdentifier.entityLocator(), e.getMessage()), e);
        }
        stageDao.markArtifactsDeletedFor(stage);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Marked stage '%s' as artifacts deleted.", stageIdentifier.entityLocator()));
        }
    }

    private boolean deleteArtifactsExceptCruiseOutput(File stageRoot) throws IOException {
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
                if (artifact.isDirectory() && artifact.getName().equals(ArtifactLogUtil.CRUISE_OUTPUT_FOLDER)) {
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
