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

package com.thoughtworks.go.server.service.lookups;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandRepositoryDirectoryWalker extends DirectoryWalker {
    private static final String XML_EXTENSION = "xml";

    private final ServerHealthService serverHealthService;
    private SystemEnvironment systemEnvironment;
    private CommandSnippetXmlParser commandSnippetXmlParser;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRepositoryDirectoryWalker.class);
    private ThreadLocal<String> commandRepositoryBaseDirectory = new ThreadLocal<>();

    @Autowired
    public CommandRepositoryDirectoryWalker(ServerHealthService serverHealthService, SystemEnvironment systemEnvironment) {
        this.serverHealthService = serverHealthService;
        this.systemEnvironment = systemEnvironment;
        commandSnippetXmlParser = new CommandSnippetXmlParser();
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        String fileName = file.getName();
        if (!FilenameUtils.getExtension(fileName).equalsIgnoreCase(XML_EXTENSION)) {
            return;
        }

        String xmlContentOfFie = safeReadFileToString(file);
        if (xmlContentOfFie == null || !file.canRead()) {
            serverHealthService.update(ServerHealthState.warning("Command Repository", "Failed to access command snippet XML file located in GoCD Server Directory at " + file.getPath() +
                    ". GoCD does not have sufficient permissions to access it.", HealthStateType.commandRepositoryAccessibilityIssue(), systemEnvironment.getCommandRepoWarningTimeout()));
            LOGGER.warn("[Command Repository] Failed to access command snippet XML file located in GoCD Server Directory at " + file.getAbsolutePath() +
                    ". GoCD does not have sufficient permissions to access it.");
            return;
        }

        try {
            String relativeFilePath = FileUtil.removeLeadingPath(commandRepositoryBaseDirectory.get(), file.getAbsolutePath());
            results.add(commandSnippetXmlParser.parse(xmlContentOfFie, FilenameUtils.getBaseName(fileName), relativeFilePath));
        } catch (Exception e) {
            String msg = "Failed loading command snippet from " + file.getAbsolutePath();
            LOGGER.warn(msg);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(msg, e);
            }
        }
    }

    private String safeReadFileToString(File file){
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        if (FileUtil.isHidden(directory)) {
            return false;
        }
        if (!FileUtil.isDirectoryReadable(directory)) {
            String message = String.format("Failed to access command repository directory: %s.", directory.getPath());
            serverHealthService.update(ServerHealthState.warning("Command Repository", message, HealthStateType.commandRepositoryAccessibilityIssue(), systemEnvironment.getCommandRepoWarningTimeout()));
            LOGGER.warn(message);
            return false;
        }
        return true;
    }

    public CommandSnippets getAllCommandSnippets(String repositoryDirectory) {
        serverHealthService.update(ServerHealthState.success(HealthStateType.commandRepositoryAccessibilityIssue()));

        try {
            File commandRepositoryDirectory = new File(repositoryDirectory);

            //adding the exists check till packaging command repository with Go story is played.
            if (commandRepositoryDirectory.isDirectory() && commandRepositoryDirectory.canRead() && commandRepositoryDirectory.canExecute()) {
                return new CommandSnippets(walk(commandRepositoryDirectory));
            } else {
                throw new IOException("Failed to access command repository located in GoCD Server Directory at " + repositoryDirectory +
                        ". The directory does not exist or GoCD does not have sufficient permissions to access it.");
            }
        } catch (IOException e) {
            ServerHealthState serverHealthState = ServerHealthState.warning("Command Repository", e.getMessage(), HealthStateType.commandRepositoryAccessibilityIssue(),
                    systemEnvironment.getCommandRepoWarningTimeout());
            serverHealthService.update(serverHealthState);
            LOGGER.warn(e.getMessage());
        }
        return new CommandSnippets(new ArrayList<>());
    }

    private List<CommandSnippet> walk(File commandRepositoryDirectory) throws IOException {
        this.commandRepositoryBaseDirectory.set(commandRepositoryDirectory.getAbsolutePath());
        List<CommandSnippet> commandSnippets = new ArrayList<>();
        walk(commandRepositoryDirectory, commandSnippets);
        return commandSnippets;
    }
}
