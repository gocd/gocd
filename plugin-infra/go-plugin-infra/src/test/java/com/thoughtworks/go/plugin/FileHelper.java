/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import java.util.Arrays;
import java.util.Objects;

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

    public File newFolders(String... folderNames) throws IOException {
        if (folderNames.length == 0) {
            return newFolder();
        }

        Arrays.stream(folderNames)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(folder -> newFolder(new File(folder)));

        return getRoot();
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

    public void delete() {
        this.rootDir.delete();
    }
}
