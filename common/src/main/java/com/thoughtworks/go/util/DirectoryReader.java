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
package com.thoughtworks.go.util;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import com.thoughtworks.go.domain.DirectoryEntries;
import com.thoughtworks.go.domain.FileDirectoryEntry;
import com.thoughtworks.go.domain.FolderDirectoryEntry;
import com.thoughtworks.go.domain.JobIdentifier;

public class DirectoryReader {
    private URLService urlService;
    private final JobIdentifier jobIdentifier;

    private static final FileFilter VISIBLE_NON_SERIALIZED_FILES = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return !(file.isHidden() || isSerializedObjectFile(file.getName()));
        }

        private boolean isSerializedObjectFile(String filename) {
            return filename.startsWith(".") && filename.endsWith(".ser");
        }

    };

    public DirectoryReader(JobIdentifier jobIdentifier) {
        this.jobIdentifier = jobIdentifier;
        urlService = new URLService();
    }

    /**
     * Recursively builds a tree of the specified rootFolder
     * TODO: ChrisS : Note that the URL stuff is completely wrong and should NOT be here - that is view, this is model
     */
    public DirectoryEntries listEntries(File rootFolder, String relativePath) {
        DirectoryEntries entries = new DirectoryEntries();

        if (rootFolder == null) {
            return entries;
        }
        File[] files = rootFolder.listFiles(VISIBLE_NON_SERIALIZED_FILES);

        if (files == null) {
            return entries;
        }
        Arrays.sort(files, new FileComparator());
        for (File file : files) {
            String name = file.getName();
            String url = getUrl(relativePath, name);
            entries.add(file.isDirectory() ?
                    new FolderDirectoryEntry(name, url, listEntries(file, getCurrentPath(relativePath) + name)) :
                    new FileDirectoryEntry(name, url));
        }

        return entries;
    }

    private String getUrl(String currentPath, String name) {
        return urlService.getRestfulArtifactUrl(jobIdentifier, getCurrentPath(currentPath) + name);
    }

    private String getCurrentPath(String currentPath) {
        return "".equals(currentPath) ? "" : currentPath + "/";
    }
}
