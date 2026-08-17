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
package com.thoughtworks.go.domain;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        Set<File> includedDirs = new LinkedHashSet<>();
        for (String directory : scanner.getIncludedDirectories()) {
            includedDirs.add(new File(rootPath, directory));
        }

        List<File> allFiles = new ArrayList<>(includedDirs);
        for (String path : scanner.getIncludedFiles()) {
            File file = new File(rootPath, path);
            if (!includedDirs.contains(file.getParentFile())) {
                allFiles.add(file);
            }
        }
        return allFiles.toArray(new File[0]);
    }
}
