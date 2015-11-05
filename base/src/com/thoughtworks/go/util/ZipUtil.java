/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;

public class ZipUtil {
    private static final Logger LOGGER = Logger.getLogger(ZipUtil.class);
    private ZipEntryHandler zipEntryHandler = null;

    public ZipUtil() {
    }

    public ZipUtil(ZipEntryHandler zipEntryHandler) {
        this.zipEntryHandler = zipEntryHandler;
    }

    public File zip(File source, File destZipFile, int level) throws IOException {
        zipContents(source, new FileOutputStream(destZipFile), level, false);
        return destZipFile;
    }

    public File zipFolderContents(File source, File destZipFile, int level) throws IOException {
        zipContents(source, new FileOutputStream(destZipFile), level, true);
        return destZipFile;
    }

    public ZipBuilder zipContentsOfMultipleFolders(File destZipFile, boolean excludeRootDir) throws IOException {
        return new ZipBuilder(this, 0, new FileOutputStream(destZipFile), excludeRootDir);
    }

    public void zipFolderContents(File destDir, File destZipFile) throws IOException {
        zipFolderContents(destDir, destZipFile, Deflater.BEST_SPEED);
    }

    public void zip(File file, OutputStream output, int level) throws IOException {
        zipContents(file, output, level, false);
    }

    private void zipContents(File file, OutputStream output, int level, boolean excludeRootDir) throws IOException {
        new ZipBuilder(this, level, output, excludeRootDir).add("", file).done();
    }

    private void addFolderToZip(ZipPath path, File source, ZipOutputStream zip, boolean excludeRootDir) throws IOException {
        ZipPath newPath = path.with(source);
        if (source.isFile()) {
            addToZip(newPath, source, zip, false);
        } else {
            addDirectory(path, source, zip, excludeRootDir);
        }
    }

    private void addDirectory(ZipPath path, File source, ZipOutputStream zip, boolean excludeRootDir) throws IOException {
        if (excludeRootDir) {
            addDirContents(path, source, zip);
            return;
        }
        ZipPath newPath = path.with(source);
        zip.putNextEntry(newPath.asZipEntryDirectory());
        addDirContents(newPath, source, zip);
    }

    private void addDirContents(ZipPath path, File source, ZipOutputStream zip) throws IOException {
        for (File file : source.listFiles()) {
            addToZip(path, file, zip, false);
        }
    }

    void addToZip(ZipPath path, File srcFile, ZipOutputStream zip, boolean excludeRootDir) throws IOException {
        if (srcFile.isDirectory()) {
            addFolderToZip(path, srcFile, zip, excludeRootDir);
        } else {
            byte[] buff = new byte[4096];
            BufferedInputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(new FileInputStream(srcFile));
                ZipEntry zipEntry = path.with(srcFile).asZipEntry();
                zipEntry.setTime(srcFile.lastModified());
                zip.putNextEntry(zipEntry);
                int len;
                while ((len = inputStream.read(buff)) > 0) {
                    zip.write(buff, 0, len);
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
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
        if (isDirectory(entryName)) {
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
                            LOGGER.warn(String.format("Failed to close the file-handle to file '%s' which was created as artifact download.", outputFile.getAbsolutePath()), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(format("Failed to unzip file [%s] to directory [%s]", entryName, toDir.getAbsolutePath()), e);
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

    private boolean isDirectory(String zipName) {
        return zipName.endsWith("/");
    }

    public String getFileContentInsideZip(ZipInputStream zipInputStream, String file) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (!new File(zipEntry.getName()).getName().equals(file)) {
            zipEntry = zipInputStream.getNextEntry();
        }
        return IOUtils.toString(zipInputStream);
    }

    public static interface ZipEntryHandler {
        public void handleEntry(ZipEntry entry, InputStream stream) throws IOException;
    }

}
