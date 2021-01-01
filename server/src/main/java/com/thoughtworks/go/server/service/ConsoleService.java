/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.ConsoleConsumer;
import com.thoughtworks.go.domain.ConsoleStreamer;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.LocatableEntity;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;

import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileName;

@Component
public class ConsoleService {

    public static final Logger LOGGER = LoggerFactory.getLogger(ConsoleService.class);
    private ArtifactDirectoryChooser chooser;
    public static final int DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE = 1024;
    private ArtifactsDirHolder artifactsDirHolder;


    public ConsoleService(ArtifactDirectoryChooser chooser) {
        this.chooser = chooser;
    }

    @Autowired
    public ConsoleService(ArtifactsDirHolder artifactsDirHolder, JobInstanceDao jobInstanceDao) {
        this(new ArtifactDirectoryChooser());
        this.artifactsDirHolder = artifactsDirHolder;
    }

    public void initialize() {
        chooser.add(new PathBasedArtifactsLocator(artifactsDirHolder.getArtifactsDir()));
        chooser.add(new BuildIdArtifactLocator(artifactsDirHolder.getArtifactsDir()));
    }

    public ConsoleConsumer getStreamer(long startingLine, JobIdentifier identifier) throws IllegalArtifactLocationException {
        Path path = consoleLogFile(identifier).toPath();
        return new ConsoleStreamer(path, startingLine);
    }

    public File consoleLogArtifact(LocatableEntity jobIdentifier) throws IllegalArtifactLocationException {
        return chooser.findArtifact(jobIdentifier, getConsoleOutputFolderAndFileName());
    }

    public boolean doesLogExist(JobIdentifier jobIdentifier) {
        try {
            return consoleLogFile(jobIdentifier).exists();
        } catch (IllegalArtifactLocationException e) {
            return false;
        }
    }

    public File consoleLogFile(LocatableEntity jobIdentifier) throws IllegalArtifactLocationException {
        File artifact = consoleLogArtifact(jobIdentifier);
        return artifact.exists() ? artifact : chooser.temporaryConsoleFile(jobIdentifier);
    }

    public void appendToConsoleLog(JobIdentifier jobIdentifier, String text) throws IllegalArtifactLocationException, IOException {
        updateConsoleLog(consoleLogFile(jobIdentifier), new ByteArrayInputStream(text.getBytes()));
    }

    public boolean updateConsoleLog(File dest, InputStream in) {
        File parentFile = dest.getParentFile();
        parentFile.mkdirs();

        LOGGER.trace("Updating console log [{}]", dest.getAbsolutePath());
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dest, dest.exists()))) {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            LOGGER.error("Failed to update console log at : [{}]", dest.getAbsolutePath(), e);
            return false;
        }
        LOGGER.trace("Console log [{}] saved.", dest.getAbsolutePath());
        return true;
    }

    public void moveConsoleArtifacts(LocatableEntity locatableEntity) {
        try {
            File from = chooser.temporaryConsoleFile(locatableEntity);

            // Job cancellation skips temporary file creation. Force create one if it does not exist.
            FileUtils.touch(from);

            File to = consoleLogArtifact(locatableEntity);
            FileUtils.moveFile(from, to);
        } catch (IOException | IllegalArtifactLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
