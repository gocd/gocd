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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestFileUtil {
    private static TempFiles tempFiles = new TempFiles();

    public static File createTempFolder(String folderName) {
        return tempFiles.mkdir(folderName);
    }

    public static File createUniqueTempFile(String prefix) {
        return tempFiles.createUniqueFile(prefix);
    }

    public static File createTempFile(String fileName) throws IOException {
        return tempFiles.createFile(fileName);
    }

    public static File createTestFile(File testFolder, String path) throws Exception {
        File subfile = new File(testFolder, path);
        subfile.createNewFile();
        subfile.deleteOnExit();
        return subfile;
    }

    public static File createTestFolder(File parent, String folderName) {
        File subDir = new File(parent, folderName);
        subDir.mkdirs();
        subDir.deleteOnExit();
        return subDir;
    }

    public static File writeStringToTempFileInFolder(String directoryName, String fileName, String contents)
            throws Exception {
        File folderToCreate =  tempFiles.createUniqueFolder(directoryName);
        File fileToCreate = new File(folderToCreate, fileName);
        folderToCreate.mkdirs();
        FileUtils.writeStringToFile(fileToCreate, contents, UTF_8);
        fileToCreate.deleteOnExit();
        folderToCreate.deleteOnExit();
        return fileToCreate;
    }

}
