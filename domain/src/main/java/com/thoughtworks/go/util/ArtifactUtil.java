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
package com.thoughtworks.go.util;

public class ArtifactUtil {
    public static final String CRUISE_OUTPUT_FOLDER = "cruise-output";
    public static final String PLUGGABLE_ARTIFACT_METADATA_FOLDER = "pluggable-artifact-metadata";

    public static final String CONSOLE_LOG_FILE_NAME = "console.log";
    public static final String CONSOLE_LOG_FILE_RELATIVE_PATH = CRUISE_OUTPUT_FOLDER + "/" + CONSOLE_LOG_FILE_NAME;

    public static final String MD5_CHECKSUM_FILENAME = "md5.checksum";

    public static boolean isConsoleOutput(String filePath) {
        return CONSOLE_LOG_FILE_RELATIVE_PATH.equalsIgnoreCase(filePath);
    }

    public static boolean artifactDirectoryIsSystemManaged(String filePath) {
        return filePath.equals(CRUISE_OUTPUT_FOLDER) || filePath.equals(PLUGGABLE_ARTIFACT_METADATA_FOLDER);
    }
}
