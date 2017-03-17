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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.ConsoleStreamer;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.LocatableEntity;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;

import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileName;

@Component
public class ConsoleService {

    public static final Logger LOGGER = Logger.getLogger(ConsoleService.class);
    private ArtifactDirectoryChooser chooser;
    public static final int DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE = 1024;
    private ArtifactsDirHolder artifactsDirHolder;


    public ConsoleService(ArtifactDirectoryChooser chooser) {
        this.chooser = chooser;
    }

    @Autowired
    public ConsoleService(ArtifactsDirHolder artifactsDirHolder) {
        this(new ArtifactDirectoryChooser());
        this.artifactsDirHolder = artifactsDirHolder;
    }

    public void initialize() {
        chooser.add(new PathBasedArtifactsLocator(artifactsDirHolder.getArtifactsDir()));
        chooser.add(new BuildIdArtifactLocator(artifactsDirHolder.getArtifactsDir()));
    }

    public ConsoleStreamer getStreamer(long startingLine, JobIdentifier identifier) throws IllegalArtifactLocationException {
        Path path = findConsoleArtifact(identifier).toPath();
        return new ConsoleStreamer(path, startingLine);
    }

    public File findConsoleArtifact(JobIdentifier identifier) throws IllegalArtifactLocationException {
        File file = chooser.temporaryConsoleFile(identifier);
        if (!file.exists()) {
            file = chooser.findArtifact(identifier, getConsoleOutputFolderAndFileName());
        }
        return file;
    }

    public File consoleLogFile(JobIdentifier jobIdentifier) throws IllegalArtifactLocationException {
        File file = chooser.temporaryConsoleFile(jobIdentifier);
        if (file.exists()) {
            return file;
        }
        File finalConsole = chooser.findArtifact(jobIdentifier, getConsoleOutputFolderAndFileName());
        if (finalConsole.exists()) return finalConsole;
        return file;
    }

    public void appendToConsoleLog(JobIdentifier jobIdentifier, String text) throws IllegalArtifactLocationException, IOException {
        File file = findConsoleArtifact(jobIdentifier);
        updateConsoleLog(file, new ByteArrayInputStream(text.getBytes()));
    }

    public boolean updateConsoleLog(File dest, InputStream in) throws IOException {
        File parentFile = dest.getParentFile();
        parentFile.mkdirs();

        LOGGER.trace("Updating console log [" + dest.getAbsolutePath() + "]");
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dest, dest.exists()))) {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            LOGGER.error("Failed to update console log at : [" + dest.getAbsolutePath() + "]", e);
            return false;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Console log [" + dest.getAbsolutePath() + "] saved.");
        }
        return true;
    }

    public void moveConsoleArtifacts(LocatableEntity locatableEntity) {
        try {
            File from = chooser.temporaryConsoleFile(locatableEntity);

            // Job cancellation skips temporary file creation. Force create one if it does not exist.
            FileUtils.touch(from);

            File to = chooser.findArtifact(locatableEntity, getConsoleOutputFolderAndFileName());
            FileUtils.moveFile(from, to);
        } catch (IOException | IllegalArtifactLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
