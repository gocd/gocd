/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.server.cache;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.web.ArtifactFolder;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZipArtifactCacheTest {
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier("pipeline-name", 1, "label-111", "stage-name", "1", "job-name", 666L);
    private static final String JOB_FOLDERS = "pipelines/pipeline-name/label-111/stage-name/1/job-name/666";

    private ZipArtifactCache zipArtifactCache;
    @TempDir
    File folder;
    private ArtifactFolder artifactFolder;
    private ArtifactsDirHolder artifactsDirHolder;

    @BeforeEach
    public void setUp() throws Exception {
        File artifact = new File(folder, JOB_FOLDERS);
        artifact.mkdirs();
        TestFileUtil.createTestFolder(artifact, "dir");
        TestFileUtil.createTestFile(artifact, "dir/file1");

        artifactsDirHolder = mock(ArtifactsDirHolder.class);
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(folder);
        zipArtifactCache = new ZipArtifactCache(this.artifactsDirHolder, new ZipUtil());
        artifactFolder = new ArtifactFolder(JOB_IDENTIFIER, new File(artifact, "dir"), "dir");
    }

    @Test
    public void shouldKnowWhenCacheAlreadyCreated() throws Exception {
        zipArtifactCache.createCachedFile(artifactFolder);

        assertThat(zipArtifactCache)
            .satisfies(cache -> assertThat(cache.cacheCreated(artifactFolder)).isTrue())
            .satisfies(cache -> assertThat(cache.cachedFile(artifactFolder).getName()).isEqualTo("dir.zip"));
    }

    @Test
    public void shouldCreateCacheWhenNotYetCreated() throws Exception {
        waitForCacheCreated();
        assertThat(zipArtifactCache)
            .satisfies(cache -> assertThat(cache.cacheCreated(artifactFolder)).isTrue())
            .satisfies(cache -> assertThat(cache.cachedFile(artifactFolder).getAbsolutePath().replaceAll("\\\\", "/")).endsWith("cache/artifacts/" + JOB_FOLDERS + "/dir.zip"));
    }

    @Test
    public void shouldOnlyCreateCacheOnce() throws Exception {
        List<FileCheckerThread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new FileCheckerThread());
        }
        for (FileCheckerThread thread : threads) {
            thread.start();
        }
        for (FileCheckerThread thread : threads) {
            thread.join(1000);
            if (thread.isAlive()) {
                fail("Timeout waiting for threads");
            }
        }

        for (FileCheckerThread thread : threads) {
            assertThat(thread.isDone()).isTrue();
            assertThat(thread.artifact.replaceAll("\\\\", "/")).endsWith(JOB_FOLDERS + "/dir.zip");
        }
    }

    @Test
    public void shouldRecoverFromOldZipTmpFile() throws Exception {
        File cacheDir = new File(folder, "cache/artifacts/" + JOB_FOLDERS);
        cacheDir.mkdirs();
        TestFileUtil.createTestFile(cacheDir, "dir.zip.tmp");

        waitForCacheCreated();
        assertThat(new File(cacheDir, "dir.zip.tmp")).doesNotExist();
        new ZipUtil().unzip(new File(cacheDir, "dir.zip"), cacheDir);
        assertThat(new File(cacheDir, "dir/file1")).exists();
    }

    private void waitForCacheCreated() throws Exception {
        int timesTried = 10;
        while (timesTried > 0 && !zipArtifactCache.cacheCreated(artifactFolder)) {
            Thread.sleep(100);
            timesTried--;
        }
        if (timesTried <= 0) {
            fail("Timeout creating cache");
        }
    }

    private class FileCheckerThread extends Thread {
        String artifact;

        public boolean isDone() {
            return artifact != null;
        }

        @Override
        public void run() {
            try {
                while (!zipArtifactCache.cacheCreated(artifactFolder)) {
                    Thread.sleep(1);
                }
                artifact = zipArtifactCache.cachedFile(artifactFolder).getAbsolutePath();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
