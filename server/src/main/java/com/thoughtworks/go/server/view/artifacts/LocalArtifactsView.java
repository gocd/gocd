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
package com.thoughtworks.go.server.view.artifacts;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.web.ArtifactFolder;
import com.thoughtworks.go.server.web.ArtifactFolderViewFactory;
import com.thoughtworks.go.server.web.FileModelAndView;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;

import static com.thoughtworks.go.util.ArtifactLogUtil.isConsoleOutput;

public class LocalArtifactsView implements ArtifactsView {
    private ArtifactsService artifactsService;
    private final JobIdentifier translatedId;
    private ConsoleService consoleService;
    protected ArtifactFolderViewFactory folderViewFactory;

    public LocalArtifactsView(ArtifactFolderViewFactory folderViewFactory, ArtifactsService artifactsService,
                              JobIdentifier translatedId, ConsoleService consoleService) {
        this.folderViewFactory = folderViewFactory;
        this.artifactsService = artifactsService;
        this.translatedId = translatedId;
        this.consoleService = consoleService;
    }

    @Override
    public final ModelAndView createView(String filePath, String sha) throws Exception {
        //return the artifact itself if this is a single file
        File file = isConsoleOutput(filePath) ? consoleService.consoleLogFile(translatedId)
                : artifactsService.findArtifact(translatedId, filePath);

        if (file.exists() && file.isFile()) {
            return FileModelAndView.createFileView(file, sha);
        }

        //return the contents of the specified directory formatted as required
        //NOTE THAT THIS IS ONLY CALLED FOR JSON OR HTML
        String convertedURL = filePath.replaceFirst("\\.(html|json|zip)$", "");
        File directory = artifactsService.findArtifact(translatedId, convertedURL);
        if (directory.exists() && directory.isDirectory()) {
            ArtifactFolder folder = new ArtifactFolder(translatedId, directory, convertedURL);
            return folderViewFactory.createView(translatedId, folder);
        }

        return FileModelAndView.fileNotFound(filePath);

    }

}
