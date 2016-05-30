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

package com.thoughtworks.go.domain.materials.tfs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


class TfsAssertionsDirectoryWalker extends DirectoryWalker {

    private final List<String> files;
    private final List<String> excludeFiles;
    private final List<String> directories;
    private final String fileContents;

    public TfsAssertionsDirectoryWalker(List<String> includeFiles, List<String> excludeFiles, List<String> includeDirectories, String fileContents) {
        this.files = includeFiles;
        this.excludeFiles = excludeFiles;
        this.directories = includeDirectories;
        this.fileContents = fileContents;
    }

    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        assertThat(directories, hasItem(directory.getName()));
        return true;
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        if (files.contains(file.getName())) {
            assertThat(FileUtils.readFileToString(file), is(fileContents));
        }
        if (excludeFiles.contains(file.getName())) {
            fail(String.format("Excluded file [%s] found in directory", file.getName()));
        }
    }

    public void walk(File folder) throws IOException {
        walk(folder, null);
    }


}

