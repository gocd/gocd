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

package com.thoughtworks.go.util.validators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.thoughtworks.go.util.validators.Validator;
import org.apache.commons.io.IOUtils;

/**
 * @understands
 */
public abstract class ZipValidator implements Validator {
    private byte[] fileBuffer = new byte[8092];

    protected void unzip(ZipInputStream zipInputStream, File destDir) throws IOException {
        destDir.mkdirs();
        try {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                extractTo(zipEntry, zipInputStream, destDir);
                zipEntry = zipInputStream.getNextEntry();
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
    }

    private void extractTo(ZipEntry entry, InputStream entryInputStream, File toDir) throws IOException {
        String entryName = nonRootedEntryName(entry);

        File outputFile = new File(toDir, entryName);
        if (isDirectory(entryName)) {
            outputFile.mkdirs();
            return;
        }
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outputFile);
            int bytes;
            while ((bytes = entryInputStream.read(fileBuffer)) > 0) {
                os.write(fileBuffer, 0, bytes);
            }
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

    String getFileContentInsideZip(ZipInputStream zipInputStream, String fileName) throws IOException {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (!new File(zipEntry.getName()).getName().equals(fileName)) {
            zipEntry = zipInputStream.getNextEntry();
        }
        return IOUtils.toString(zipInputStream);
    }

}
