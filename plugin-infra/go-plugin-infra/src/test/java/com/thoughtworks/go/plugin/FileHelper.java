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
package com.thoughtworks.go.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class FileHelper {
    private File rootDir;

    public FileHelper(final File rootDir) {
        this.rootDir = rootDir;
    }

    public File newFolder() throws IOException {
        return newFolder(File.createTempFile("junit", "", this.rootDir));
    }

    public File getRoot() {
        return rootDir;
    }

    public File newFolder(String... folderNames) throws IOException {
        if (folderNames.length == 0) {
            return newFolder();
        }

        File file = getRoot();
        List<String> of = List.of(folderNames);
        for (int i = 0; i < of.size(); i++) {
            String name = of.get(i);
            file = new File(file, name);
            if (!file.mkdir() && isLastElementInArray(i, folderNames)) {
                throw new IOException(
                        "a folder with the name \'" + name + "\' already exists");
            }
        }

        return file;
    }

    private boolean isLastElementInArray(int index, String[] array) {
        return index == array.length - 1;
    }

    public File newFolder(String folder) {
        return newFolder(new File(getRoot(), folder));
    }

    public File newFile(String fileName) throws IOException {
        File file = new File(getRoot(), fileName);
        if (!file.createNewFile()) {
            throw new IOException(format("a file with the name '%s' already exists in the test folder", fileName));
        }
        return file;
    }

    private File newFolder(File createdFolder) {
        createdFolder.delete();
        createdFolder.mkdir();
        return createdFolder;
    }
}
