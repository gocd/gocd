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
package com.thoughtworks.go.server.caching;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.web.ArtifactFolder;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.ZipUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.thoughtworks.go.util.TestUtils.doInterruptiblyQuietlyRethrowInterrupt;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZipArtifactCacheTest {
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier("pipeline-name", 1, "label-111", "stage-name", "1", "job-name", 666L);
    private static final String JOB_FOLDERS = "pipelines/pipeline-name/label-111/stage-name/1/job-name/666";

    @TempDir File tempDir;

    private ZipArtifactCache zipArtifactCache;

    private File testJobArtifactStorage;
    private ArtifactFolder testDirArtifactFolder;

    @BeforeEach
    public void setUp() throws Exception {
        testJobArtifactStorage = new File(tempDir, JOB_FOLDERS);
        testJobArtifactStorage.mkdirs();
        File dir = TestFileUtil.createTestFolder(testJobArtifactStorage, "dir");
        TestFileUtil.createTestFile(testJobArtifactStorage, "dir/file1");

        ArtifactsDirHolder artifactsDirHolder = mock(ArtifactsDirHolder.class);
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(tempDir);
        zipArtifactCache = new ZipArtifactCache(artifactsDirHolder, new ZipUtil());
        testDirArtifactFolder = new ArtifactFolder(JOB_IDENTIFIER, dir, "dir");
    }

    @Test
    public void shouldKnowWhenCacheAlreadyCreated() throws Exception {
        zipArtifactCache.createCachedFile(testDirArtifactFolder);

        assertThat(zipArtifactCache)
            .satisfies(cache -> assertThat(cache.cacheCreated(testDirArtifactFolder)).isTrue())
            .satisfies(cache -> assertThat(cache.cachedFile(testDirArtifactFolder).getName()).isEqualTo("dir.zip"));
    }

    @Test
    public void shouldCreateCacheWhenNotYetCreated() throws Exception {
        waitForCacheCreated();
        assertThat(zipArtifactCache)
            .satisfies(cache -> assertThat(cache.cacheCreated(testDirArtifactFolder)).isTrue())
            .satisfies(cache -> assertThat(cache.cachedFile(testDirArtifactFolder).getAbsolutePath().replace('\\', '/')).endsWith("cache/artifacts/" + JOB_FOLDERS + "/dir.zip"));
    }

    @Test
    public void shouldOnlyCreateCacheOnce() {

        List<FileCheckerThread> threads = IntStream.range(0, 10).mapToObj(i -> new FileCheckerThread()).toList();
        threads.forEach(Thread::start);

        try {
            threads.forEach(thread -> doInterruptiblyQuietlyRethrowInterrupt(() -> {
                thread.join(TimeUnit.SECONDS.toMillis(2));
                if (thread.isAlive()) {
                    fail("Timeout waiting for threads");
                }
            }));

            threads.forEach(thread -> {
                assertThat(thread.isDone()).isTrue();
                assertThat(thread.artifact.replace('\\', '/')).endsWith(JOB_FOLDERS + "/dir.zip");
            });
        } finally {
            threads.forEach(Thread::interrupt);
        }
    }

    @Test
    public void shouldRecoverFromOldZipTmpFile() throws Exception {
        File cacheDir = new File(tempDir, "cache/artifacts/" + JOB_FOLDERS);
        cacheDir.mkdirs();
        TestFileUtil.createTestFile(cacheDir, "dir.zip.tmp");

        waitForCacheCreated();
        assertThat(new File(cacheDir, "dir.zip.tmp")).doesNotExist();
        new ZipUtil().unzip(new File(cacheDir, "dir.zip"), cacheDir);
        assertThat(new File(cacheDir, "dir/file1")).exists();
    }

    private void waitForCacheCreated() throws Exception {
        long waitUntil = System.currentTimeMillis() + SECONDS.toMillis(2);
        while (System.currentTimeMillis() <= waitUntil && !zipArtifactCache.cacheCreated(testDirArtifactFolder)) {
            Thread.sleep(10);
        }
        if (System.currentTimeMillis() > waitUntil) {
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
                while (!zipArtifactCache.cacheCreated(testDirArtifactFolder)) {
                    Thread.sleep(10);
                }
                artifact = zipArtifactCache.cachedFile(testDirArtifactFolder).getAbsolutePath();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
