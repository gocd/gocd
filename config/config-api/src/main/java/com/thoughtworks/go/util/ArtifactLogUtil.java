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
package com.thoughtworks.go.util;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class ArtifactLogUtil {
    public static final String CONSOLE_LOG_FILE_NAME = "console.log";
    public static final String CRUISE_OUTPUT_FOLDER = "cruise-output";
    public static final String MD5_CHECKSUM_FILENAME = "md5.checksum";
    public static final String PLUGGABLE_ARTIFACT_METADATA_FOLDER = "pluggable-artifact-metadata";

    public static String getPath(long buildId) {
        return getPath(buildId, CONSOLE_LOG_FILE_NAME);
    }

    public static String getConsoleOutputFolderAndFileNameUrl() {
        return getOutputFolderAndFileName(CONSOLE_LOG_FILE_NAME, "/");
    }

    public static String getConsoleOutputFolderAndFileName() {
        return CRUISE_OUTPUT_FOLDER + "/" + CONSOLE_LOG_FILE_NAME;
    }

    private static String getOutputFolderAndFileName(String fileName, String separator) {
        return separator + CRUISE_OUTPUT_FOLDER + separator + fileName;
    }

    private static String getPath(long buildId, String fileName) {
        String path = "" + buildId + File.separator + CRUISE_OUTPUT_FOLDER + File.separator + fileName;

        return FilenameUtils.separatorsToUnix(new File(path).getPath());
    }

    public static boolean isConsoleOutput(String filePath) {
        return getConsoleOutputFolderAndFileName().equalsIgnoreCase(filePath);
    }

}
