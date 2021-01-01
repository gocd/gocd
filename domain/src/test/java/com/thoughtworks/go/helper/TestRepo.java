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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public abstract class TestRepo {

    protected static List<File> tmpFolders = new ArrayList<>();

    protected final TemporaryFolder temporaryFolder;

    public TestRepo(TemporaryFolder temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
    }

    public static void internalTearDown() {
        for (File tmpFolder : tmpFolders) {
            FileUtils.deleteQuietly(tmpFolder);
        }
    }

    public abstract String projectRepositoryUrl();

    public void tearDown() {
        TestRepo.internalTearDown();
        onTearDown();
    }

    public void onTearDown() {
    }

    public abstract List<Modification> checkInOneFile(String fileName, String comment) throws Exception;

    public abstract List<Modification> latestModification() throws IOException;

    public void onSetup() throws Exception {

    }

    public abstract Material material();
}


