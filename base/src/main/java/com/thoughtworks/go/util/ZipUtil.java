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

package com.thoughtworks.go.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.util.zip.Deflater.BEST_SPEED;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ZipUtil {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ZipUtil.class);
    private static final int BUFFER_SIZE = 16 * 1024;
    private ZipEntryHandler zipEntryHandler = null;

    public ZipUtil() {
    }

    public ZipUtil(ZipEntryHandler zipEntryHandler) {
        this.zipEntryHandler = zipEntryHandler;
    }

    public File zip(File source, File destZipFile, int level) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destZipFile), BUFFER_SIZE)) {
            zip(source, outputStream, level);
        }
        return destZipFile;
    }

    public File zipFolderContents(File source, File destZipFile) throws IOException {
        return zipFolderContents(source, destZipFile, BEST_SPEED);
    }

    public File zipFolderContents(File source, File destZipFile, int level) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destZipFile), BUFFER_SIZE)) {
            zip(source, outputStream, level, null);
        }
        return destZipFile;
    }

    public void zip(File source, OutputStream outputStream, int level) throws IOException {
        zip(source, outputStream, level, source.getName());
    }

    public void zip(File source, OutputStream outputStream, int level, String baseDirInZipFile) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            zip.setLevel(level);
            addToZip(source, zip, baseDirInZipFile);
        }
    }

    public void addToZip(File source, ZipOutputStream zip, String baseDirInZipFile) throws IOException {
        walkDir(source, file -> {
            String entryPath = toEntrypath(source, baseDirInZipFile, file);

            ZipEntry entry = new ZipEntry(entryPath);
            entry.setTime(file.lastModified());
            zip.putNextEntry(entry);

            if (file.isFile()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    IOUtils.copy(fis, zip, BUFFER_SIZE);
                }
            }
        });
    }

    private String toEntrypath(File source, String baseDirInZipFile, File file) {
        String s = maybeAddBaseDir(baseDirInZipFile, FilenameUtils.separatorsToUnix(source.toPath().relativize(file.toPath()).toString()));
        if (file.isDirectory()) {
            return s + "/";
        } else {
            return s;
        }
    }

    private String maybeAddBaseDir(String baseDir, String path) {
        if (isBlank(baseDir) ) {//|| isBlank(Paths.get(baseDir).normalize().toString())){
            return path;
        } else {
            return baseDir + "/" + path;
        }
    }

    private void walkDir(File parent, ThrowingConsumer<File, IOException> callback) throws IOException {
        if (parent.isDirectory()) {
            for (File child : parent.listFiles()) {
                if (child.isDirectory()) {
                    callback.accept(child);
                    walkDir(child, callback);
                } else {
                    callback.accept(child);
                }
            }
        } else {
            callback.accept(parent);
        }
    }

    private void bombIfZipEntryPathContainsDirectoryTraversalCharacters(String filepath) {
        if (filepath.contains("..")) {
            throw new IllegalPathException(String.format("File %s is outside extraction target directory", filepath));
        }
    }

    public void unzip(ZipInputStream zipInputStream, File destDir) throws IOException {
        destDir.mkdirs();
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            extractTo(zipEntry, zipInputStream, destDir);
            zipEntry = zipInputStream.getNextEntry();
        }
        IOUtils.closeQuietly(zipInputStream);
    }

    public void unzip(File zip, File destDir) throws IOException {
        unzip(new ZipInputStream(new BufferedInputStream(new FileInputStream(zip))), destDir);
    }

    private void extractTo(ZipEntry entry, InputStream entryInputStream, File toDir) throws IOException {
        bombIfZipEntryPathContainsDirectoryTraversalCharacters(entry.getName());
        String entryName = nonRootedEntryName(entry);

        File outputFile = new File(toDir, entryName);
        if (entry.isDirectory()) {
            outputFile.mkdirs();
            return;
        }
        FileOutputStream os = null;
        try {
            outputFile.getParentFile().mkdirs();
            os = new FileOutputStream(outputFile);
            IOUtils.copyLarge(entryInputStream, os);
            if (zipEntryHandler != null) {
                FileInputStream stream = null;
                try {
                    stream = new FileInputStream(outputFile);
                    zipEntryHandler.handleEntry(entry, stream);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            LOGGER.warn("Failed to close the file-handle to file '{}' which was created as artifact download.", outputFile.getAbsolutePath(), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to unzip file [{}] to directory [{}]", entryName, toDir.getAbsolutePath(), e);
            throw e;
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private String nonRootedEntryName(ZipEntry entry) {
        String entryName = entry.getName();
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }
        return entryName;
    }

    public String getFileContentInsideZip(ZipInputStream zipInputStream, String file) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            if (new File(zipEntry.getName()).getName().equals(file)) {
                return IOUtils.toString(zipInputStream);
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        return null;
    }

    public static interface ZipEntryHandler {
        public void handleEntry(ZipEntry entry, InputStream stream) throws IOException;
    }

}
