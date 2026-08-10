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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class FileUtil {
    public static final String TMP_PARENT_DIR = "data";
    private static final String CRUISE_TMP_FOLDER = "cruise" + "-" + UUID.randomUUID();

    private FileUtil() {}

    public static boolean isFolderEmpty(File folder) {
        if (folder == null) {
            return true;
        }
        File[] files = folder.listFiles();
        return files == null || files.length == 0;
    }

    /**
     * Makes parent directories, ignoring if it already exists
     */
    public static void mkdirsParentQuietly(File file) {
        File directory = file.getParentFile();
        mkdirsQuietly(directory);
    }

    /**
     * Makes directories, ignoring if null or already exists
     */
    private static void mkdirsQuietly(File directory) {
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Deletes a file or directory (recursively), never throwing an exception. Equivalent to Commons IO
     * {@code FileUtils.deleteQuietly}.
     *
     * @return true if the file or directory was deleted, false otherwise
     */
    public static boolean deleteQuietly(File file) {
        if (file == null) {
            return false;
        }
        try {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates the file (and any missing parent directories) if it does not exist, or updates its last-modified
     * time if it does. Equivalent to Commons IO {@code FileUtils.touch}.
     */
    public static void touch(File file) throws IOException {
        mkdirsParentQuietly(file);
        // atomically creates the file if absent; closing alone doesn't bump the last-modified time
        Files.newOutputStream(file.toPath(), CREATE, APPEND).close();
        if (!file.setLastModified(System.currentTimeMillis())) {
            throw new IOException("Unable to update the last modified time of " + file);
        }
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

    public static File createTempFolder() {
        File tempDir = new File(TMP_PARENT_DIR, CRUISE_TMP_FOLDER);
        File dir = new File(tempDir, UUID.randomUUID().toString());
        boolean ret = dir.mkdirs();
        if (!ret) {
            throw new RuntimeException("FileUtil#createTempFolder - Could not create temp folder");
        }
        return dir;
    }
}


