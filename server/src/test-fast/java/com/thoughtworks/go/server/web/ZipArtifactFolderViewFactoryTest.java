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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.cache.ZipArtifactCache;
import com.thoughtworks.go.server.view.artifacts.PreparingArtifactFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZipArtifactFolderViewFactoryTest {
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier("pipeline-name", "label-111", "stage-name", 1, "job-name", 666L);
    @TempDir
    Path folder;
    private ZipArtifactFolderViewFactory folderViewFactory;
    private File cacheZipFile;

    @BeforeEach
    public void setUp(@TempDir Path cache) throws Exception {
        Files.createDirectory(folder.resolve("dir"));
        cacheZipFile = Files.createFile(cache.resolve("dir.zip")).toFile();
    }

    @Test public void shouldCreateArtifactCacheIfDoesNotExist() throws Exception {
        folderViewFactory = new ZipArtifactFolderViewFactory(cacheNotCreated());

        ModelAndView modelAndView = folderViewFactory.createView(JOB_IDENTIFIER, new ArtifactFolder(JOB_IDENTIFIER, folder.toFile(), "dir"));
        assertThat(modelAndView.getView(), is(instanceOf(PreparingArtifactFile.class)));
    }

    @Test public void shouldViewCachedZipArtifactIfAlreadyCreated() throws Exception {
        folderViewFactory = new ZipArtifactFolderViewFactory(cacheAlreadyCreated());

        ModelAndView modelAndView = folderViewFactory.createView(JOB_IDENTIFIER, new ArtifactFolder(JOB_IDENTIFIER, folder.toFile(), "dir"));
        assertThat(modelAndView.getViewName(), is("fileView"));
        File targetFile = (File) modelAndView.getModel().get("targetFile");
        assertThat(targetFile, is(cacheZipFile));
    }

    private ZipArtifactCache cacheAlreadyCreated() {
        return new ZipArtifactCache(null, null) {
            @Override
            public boolean cacheCreated(ArtifactFolder artifactFolder) {
                return true;
            }

            @Override
            public File cachedFile(ArtifactFolder artifactFolder) {
                return cacheZipFile;
            }
        };
    }

    private ZipArtifactCache cacheNotCreated() {
        return new ZipArtifactCache(null, null) {
            @Override
            public boolean cacheCreated(ArtifactFolder artifactFolder) {
                return false;
            }

            @Override
            public File cachedFile(ArtifactFolder artifactFolder) {
                throw new RuntimeException("Cache file " + artifactFolder + " not created yet");
            }
        };
    }
}
