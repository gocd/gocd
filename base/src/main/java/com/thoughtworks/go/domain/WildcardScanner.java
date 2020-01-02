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
package com.thoughtworks.go.domain;

import org.apache.commons.io.FilenameUtils;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WildcardScanner  {
    private final File rootPath;
    private final String pattern;

    public WildcardScanner(File rootPath, String pattern) {
        this.rootPath = rootPath;
        this.pattern = FilenameUtils.separatorsToUnix(pattern);
    }

    public File[] getFiles() {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(rootPath);
        scanner.setIncludes(new String[]{pattern});
        scanner.scan();
        String[] allPaths = scanner.getIncludedFiles();
        List<File> allFiles = new ArrayList<>();
        String[] directories = scanner.getIncludedDirectories();
        for (String directory : directories) {
            allFiles.add(new File(rootPath, directory));
        }

        for (int i = 0; i < allPaths.length; i++) {
            File file = new File(rootPath, allPaths[i]);
            if (!allFiles.contains(file.getParentFile())) {
                allFiles.add(file);
            }
        }
        return allFiles.toArray(new File[]{});
    }
}
