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
package com.thoughtworks.go.utils;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.DisposableBean;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public abstract class TestRepoFixture implements DisposableBean{
    protected File templateRepo;
    protected final TemporaryFolder temporaryFolder;
    protected File testRepo;

    public TestRepoFixture(String templateRepoPath, TemporaryFolder temporaryFolder) {
        templateRepo = new File(templateRepoPath);
        this.temporaryFolder = temporaryFolder;
    }

    public File createRepository() throws IOException {
        if (testRepo == null) {
            File repoLocation = temporaryFolder.newFolder("testRepo");
            return createRepository(repoLocation);
        }
        return testRepo;
    }

    private File createRepository(File repoLocation) {
        if (testRepo == null) {
            testRepo = repoLocation;
            try {
                FileUtils.copyDirectory(templateRepo, testRepo);
            } catch (IOException e) {
                bomb(e);
            }
        }
        return testRepo;
    }

    public File currentRepository() throws IOException {
        if (testRepo == null) {
            createRepository();
        }
        return testRepo;
    }

    @Override
    public void destroy() {
        FileUtils.deleteQuietly(testRepo);
    }
}
