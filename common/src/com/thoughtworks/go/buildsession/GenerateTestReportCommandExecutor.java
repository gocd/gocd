/*
 * Copyright 2016 ThoughtWorks, Inc.
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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.UnitTestReportGenerator;
import com.thoughtworks.go.domain.WildcardScanner;
import com.thoughtworks.go.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenerateTestReportCommandExecutor implements BuildCommandExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTestReportCommandExecutor.class);

    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        File workingDirectory = buildSession.resolveRelativeDir(command.getWorkingDirectory());
        String uploadPath = command.getStringArg("uploadPath");
        String[] sources = command.getArrayArg("srcs");
        ArrayList<File> allFiles = findMatchedSourceFiles(buildSession, workingDirectory, sources);
        if (allFiles.size() > 0) {
            File tempFolder = null;
            try {
                tempFolder = FileUtil.createTempFolder();
                File testResultSource = new File(tempFolder, "result");
                testResultSource.mkdirs();
                UnitTestReportGenerator generator = new UnitTestReportGenerator(buildSession.getPublisher(), testResultSource);
                generator.generate(allFiles.toArray(new File[allFiles.size()]), uploadPath);
            } finally {
                if (tempFolder != null) {
                    FileUtil.deleteFolder(tempFolder);
                }
            }

        } else {
            String message = "No files were found in the Test Results folders";
            buildSession.printlnWithPrefix(message);
            LOG.warn(message);
        }
        return true;
    }

    private ArrayList<File> findMatchedSourceFiles(BuildSession buildSession, File workingDirectory, String[] sources) {
        ArrayList<File> allFiles = new ArrayList<>();
        for (String src : sources) {
            File source = new File(FileUtil.applyBaseDirIfRelativeAndNormalize(workingDirectory, new File(src)));

            WildcardScanner wildcardScanner = new WildcardScanner(workingDirectory, src);
            File[] files = wildcardScanner.getFiles();

            if (files.length > 0) {
                final List<File> fileList = files == null ? new ArrayList<File>() : Arrays.asList(files);
                allFiles.addAll(fileList);
            } else {
                final String message = MessageFormat.format("The Directory {0} specified as a test artifact was not found."
                        + " Please check your configuration", FileUtil.normalizePath(source));
                buildSession.printlnWithPrefix(message);
                LOG.error(message);
            }
        }
        return allFiles;
    }
}
