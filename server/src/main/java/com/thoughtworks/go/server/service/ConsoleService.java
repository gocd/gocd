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

import com.thoughtworks.go.domain.ConsoleConsumer;
import com.thoughtworks.go.domain.ConsoleStreamer;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.LocatableEntity;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import com.thoughtworks.go.util.ArtifactUtil;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Component
public class ConsoleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleService.class);

    private final ArtifactDirectoryChooser chooser;
    private final ArtifactsDirHolder artifactsDirHolder;

    @Autowired
    public ConsoleService(ArtifactsDirHolder artifactsDirHolder) {
        this(new ArtifactDirectoryChooser(), artifactsDirHolder);
    }

    @VisibleForTesting
    ConsoleService(ArtifactDirectoryChooser chooser, ArtifactsDirHolder artifactsDirHolder) {
        this.chooser = chooser;
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

    public boolean doesLogExist(JobIdentifier jobIdentifier) {
        try {
            return consoleLogFile(jobIdentifier).exists();
        } catch (IllegalArtifactLocationException e) {
            return false;
        }
    }

    public @NotNull File consoleLogFile(LocatableEntity jobIdentifier) throws IllegalArtifactLocationException {
        File artifact = consoleLogArtifact(jobIdentifier);
        return artifact.exists() ? artifact : chooser.temporaryConsoleFile(jobIdentifier);
    }

    private @NotNull File consoleLogArtifact(LocatableEntity locatableEntity) throws IllegalArtifactLocationException {
        return chooser.findArtifact(locatableEntity, ArtifactUtil.CONSOLE_LOG_FILE_RELATIVE_PATH);
    }

    private @NotNull File consoleLogArtifactUnchecked(LocatableEntity locatableEntity) {
        try {
            return consoleLogArtifact(locatableEntity);
        } catch (IllegalArtifactLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public void appendToConsoleLogIoSafe(JobIdentifier jobIdentifier, String text) throws IllegalArtifactLocationException {
        appendToConsoleLogIoSafe(consoleLogFile(jobIdentifier), new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean appendToConsoleLogIoSafe(File dest, InputStream in) {
        FileUtil.mkdirsParentQuietly(dest);
        try (OutputStream out = new FileOutputStream(dest, dest.exists())) {
            in.transferTo(out);
        } catch (IOException e) {
            LOGGER.error("Failed to update console log at : [{}]", dest.getAbsolutePath(), e);
            return false;
        }
        return true;
    }

    void appendToConsoleLogSafe(JobIdentifier jobIdentifier, String errorMessage) {
        try {
            appendToConsoleLogIoSafe(jobIdentifier, errorMessage);
        } catch (IllegalArtifactLocationException e) {
            LOGGER.error("Failed to add message({}) to the job({}) console log", errorMessage, jobIdentifier, e);
        }
    }

    void appendToConsoleLogUnchecked(JobIdentifier identifier, String errorMessage) {
        try {
            appendToConsoleLogIoSafe(identifier, errorMessage);
        } catch (IllegalArtifactLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public void moveConsoleArtifacts(LocatableEntity locatableEntity) {
        File from = chooser.temporaryConsoleFile(locatableEntity);
        File to = consoleLogArtifactUnchecked(locatableEntity);
        try {
            // Job cancellation can skip temporary file creation. Force create one if it does not exist.
            FileUtils.touch(from);
            FileUtils.moveFile(from, to);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error moving console log from temporary location [%s] to permanent artifact location [%s]".formatted(from, to), e);
        }
    }
}
