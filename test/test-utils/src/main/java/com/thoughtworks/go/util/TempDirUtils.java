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

package com.thoughtworks.go.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class TempDirUtils {
    private static final String DIR_PREFIX = "testDir";

    private TempDirUtils() {
    }


    public static Path createTempDirectoryIn(Path tempDir, String folderName) throws IOException {
        return isBlank(folderName) ? Files.createTempDirectory(tempDir, DIR_PREFIX) : Files.createDirectories(tempDir.resolve(folderName));
    }

    public static Path createRandomDirectoryIn(Path tempDir) throws IOException {
        return createTempDirectoryIn(tempDir, null);
    }

    public static File newFile(Path file) throws IOException {
        return newFile(file.toFile());
    }

    public static File newFile(File file) throws IOException {
        if (!file.createNewFile()) {
            throw new IOException(String.format("a file with the name '%s' already exists", file));
        }
        return file;
    }


}
