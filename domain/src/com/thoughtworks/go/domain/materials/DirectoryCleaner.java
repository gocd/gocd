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

package com.thoughtworks.go.domain.materials;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.FileUtil;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

public class DirectoryCleaner {
    private File baseFolder;
    private final ConsoleOutputStreamConsumer consumer;
    private Set<File> allowed = new HashSet<>();
    private Set<File> check = new HashSet<>();

    public DirectoryCleaner(File baseFolder, ConsoleOutputStreamConsumer consumer) {
        this.baseFolder = baseFolder;
        this.consumer = consumer;
    }

    public void allowed(String... folders) {
        allowed(Arrays.asList(folders));
    }

    public void allowed(List<String> allowedFolders) {
        ArrayList<File> allowedDirs = convertToFiles(allowedFolders);
        for (File allowedDir : allowedDirs) {
            allowed(allowedDir, allowedDirs);
        }
    }

    public void allowed(File allowedDir, List<File> allowedDirs) {
        try {
            if (!FileUtil.isSubdirectoryOf(baseFolder, allowedDir)) {
                throw new RuntimeException(
                        "Cannot clean directory."
                        + " Folder " + allowedDir.getAbsolutePath() + " is outside the base folder " + baseFolder);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Cannot clean directory."
                    + " Folder " + allowedDir.getAbsolutePath() + " is outside the base folder " + baseFolder);
        }
        allow(allowedDir, allowedDirs);
    }

    private ArrayList<File> convertToFiles(List<String> allowedFolders) {
        ArrayList<File> allowedDirs = new ArrayList<>();
        for (String folder : allowedFolders) {
            allowedDirs.add(new File(baseFolder, folder));
        }
        return allowedDirs;
    }

    private void allow(File allowedDir, List<File> allowedDirs) {
        allowed.add(allowedDir);
        File parentDir = allowedDir.getParentFile();
        if (!parentDir.equals(baseFolder) && !allowed.contains(parentDir)) {
            if (!isContainedInOtherAllowedDirs(parentDir, allowedDirs)) {
                check(parentDir);
            }
            allow(parentDir, allowedDirs);
        }
    }

    private boolean isContainedInOtherAllowedDirs(File dir, List<File> allowedDirs) {
        for (File allowedDir : allowedDirs) {
            try {
                if (FileUtil.isSubdirectoryOf(allowedDir, dir)) {
                    return true;
                }
            } catch (IOException e) {
                throw bomb(String.format("Failed to check directory %s and %s for sandbox cleanup", allowedDir, dir), e);
            }
        }
        return false;
    }

    private void check(File dir) {
        check.add(dir);
    }

    public void clean() {
        if (allowed.isEmpty()) {
            return;
        }
        cleanFolder(baseFolder);
        for (File dir : check) {
            cleanFolder(dir);
        }
    }

    private void cleanFolder(File toClean) {
        File[] files = toClean.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!allowed.contains(file)) {
                consumer.stdOutput(String.format("[%s] Deleting folder %s", GoConstants.PRODUCT_NAME, file));
                FileUtil.deleteFolder(file);
            } else {
                consumer.stdOutput(String.format("[%s] Keeping folder %s", GoConstants.PRODUCT_NAME, file));
            }
        }
    }
}
