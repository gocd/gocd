/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

public class FileUtil {
    public static final String TMP_PARENT_DIR = "data";
    private static final String CRUISE_TMP_FOLDER = "cruise" + "-" + UUID.randomUUID();
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {}

    public static boolean isFolderEmpty(File folder) {
        if (folder == null) {
            return true;
        }
        File[] files = folder.listFiles();
        return files == null || files.length == 0;
    }

    public static String applyBaseDirIfRelativeAndNormalize(File baseDir, File actualFileToUse) {
        return FilenameUtils.separatorsToUnix(applyBaseDirIfRelative(baseDir, actualFileToUse).getPath());
    }

    public static File applyBaseDirIfRelative(File baseDir, File actualFileToUse) {
        if (actualFileToUse == null) {
            return baseDir;
        }
        if (actualFileToUse.isAbsolute()) {
            return actualFileToUse;
        }

        if (StringUtils.isBlank(baseDir.getPath())) {
            return actualFileToUse;
        }

        return new File(baseDir, actualFileToUse.getPath());

    }

    public static void validateAndCreateDirectory(File directory) {
        if (directory.exists()) {
            return;
        }
        try {
            FileUtils.forceMkdir(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder: " + directory.getAbsolutePath());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createParentFolderIfNotExist(File file) {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs();
        }
    }

    public static String toFileURI(File file) {
        URI uri = file.toURI();
        String uriString = uri.toASCIIString();
        return uriString.replaceAll("^file:/", "file:///");
    }

    public static String toFileURI(String path) {
        return toFileURI(new File(path));
    }

    public static boolean isSubdirectoryOf(File parent, File subdirectory) throws IOException {
        File parentFile = parent.getCanonicalFile();
        File current = subdirectory.getCanonicalFile();
        while (current != null) {
            if (current.equals(parentFile)) {
                return true;
            }
            current = current.getParentFile();
        }
        return false;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createFilesByPath(File baseDir, String... files) throws IOException {
        for (String file : files) {
            File file1 = new File(baseDir, file);
            if (file.endsWith("/")) {
                file1.mkdirs();
            } else {
                file1.getParentFile().mkdirs();
                file1.createNewFile();
            }
        }
    }

    public static String subtractPath(File rootPath, File file) {
        String fullPath = FilenameUtils.separatorsToUnix(file.getParentFile().getPath());
        String basePath = FilenameUtils.separatorsToUnix(rootPath.getPath());
        return StringUtils.removeStart(StringUtils.removeStart(fullPath, basePath), "/");
    }

    public static File createTempFolder() {
        File tempDir = new File(TMP_PARENT_DIR, CRUISE_TMP_FOLDER);
        File dir = new File(tempDir, UUID.randomUUID().toString());
        boolean ret = dir.mkdirs();
        if (!ret) {
            throw new RuntimeException("FileUtil#createTempFolder - Could not create temp folder");
        }
        return dir;
    }

    public static String getCanonicalPath(File workDir) {
        try {
            return workDir.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteDirectoryNoisily(File defaultDirectory) {
        if (!defaultDirectory.exists()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(defaultDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete directory: " + defaultDirectory.getAbsolutePath(), e);
        }
    }

    public static String join(File defaultWorkingDir, String actualFileToUse) {
        if (actualFileToUse == null) {
            LOGGER.trace("Using the default Directory->{}", defaultWorkingDir);
            return FilenameUtils.separatorsToUnix(defaultWorkingDir.getPath());
        }
        return applyBaseDirIfRelativeAndNormalize(defaultWorkingDir, new File(actualFileToUse));
    }
}


