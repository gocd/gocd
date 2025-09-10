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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.TempDirUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;


public abstract class TestRepo {
    private Path tempDir;

    public TestRepo(Path tempDir) {
        this.tempDir = tempDir;
    }

    public static String toFileURI(File file) {
        URI uri = file.toURI();
        String uriString = uri.toASCIIString();
        return uriString.replaceAll("^file:/", "file:///");
    }

    public static String toFileURI(String path) {
        return toFileURI(new File(path));
    }

    public Path createRandomTempDirectory() throws IOException {
        return TempDirUtils.createRandomDirectoryIn(tempDir);
    }

    public Path createTempDirectory(String folderName) throws IOException {
        return TempDirUtils.createTempDirectoryIn(tempDir, folderName);
    }

    public abstract String projectRepositoryUrl();

    public void tearDown() {
    }

    public abstract List<Modification> checkInOneFile(String fileName, String comment) throws Exception;

    public abstract List<Modification> latestModification() throws IOException;

    public TestRepo onSetup() {
        return null;
    }

    public abstract Material material();
}


