/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.web.ArtifactFolder;
import com.thoughtworks.go.util.*;
import org.apache.log4j.Level;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.matchers.FileExistsMatcher.exists;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(JMock.class)
public class ZipArtifactCacheTest {
    private static final JobIdentifier JOB_IDENTIFIER = new JobIdentifier("pipeline-name", "label-111", "stage-name", 1, "job-name", 666L);
    private static final String JOB_FOLDERS = "pipelines/pipeline-name/label-111/stage-name/1/job-name/666";

    ClassMockery context = new ClassMockery();

    private ZipArtifactCache zipArtifactCache;
    private File folder;
    private ArtifactFolder artifactFolder;
    private ArtifactsDirHolder artifactsDirHolder;
    private LogFixture logFixture;

    @Before public void setUp() throws Exception {
        folder = TestFileUtil.createTempFolder("ZipArtifactCacheTest-" + System.currentTimeMillis());
        File artifact = new File(folder, JOB_FOLDERS);
        artifact.mkdirs();
        TestFileUtil.createTestFolder(artifact, "dir");
        TestFileUtil.createTestFile(artifact, "dir/file1");

        artifactsDirHolder = context.mock(ArtifactsDirHolder.class);
        context.checking(new Expectations() {{
            allowing(artifactsDirHolder).getArtifactsDir();
            will(returnValue(folder));
        }});
        zipArtifactCache = new ZipArtifactCache(this.artifactsDirHolder, new ZipUtil());
        artifactFolder = new ArtifactFolder(JOB_IDENTIFIER, new File(artifact, "dir"), "dir");
        logFixture = LogFixture.startListening(Level.ALL);
    }

    @After public void tearDown() throws Exception {
        FileUtil.deleteFolder(folder);
        logFixture.stopListening();
    }

    @Test public void shouldKnowWhenCacheAlreadyCreated() throws Exception {
        zipArtifactCache.createCachedFile(artifactFolder);

        assertThat(zipArtifactCache, cacheCreated(artifactFolder));
        assertThat(zipArtifactCache.cachedFile(artifactFolder).getName(), is("dir.zip"));
    }

    @Test public void shouldCreateCacheWhenNotYetCreated() throws Exception {
        waitForCacheCreated();
        assertThat(zipArtifactCache, cacheCreated(artifactFolder));
        File zipFile = zipArtifactCache.cachedFile(artifactFolder);
        assertThat(zipFile.getAbsolutePath().replaceAll("\\\\", "/"), endsWith("cache/artifacts/" + JOB_FOLDERS + "/dir.zip"));
    }

    @Test public void shouldOnlyCreateCacheOnce() throws Exception {
        ArrayList<FileCheckerThread> threads = new ArrayList<FileCheckerThread>();
        for (int i = 0; i < 10; i++) {
            threads.add(new FileCheckerThread());
        }
        for (FileCheckerThread thread : threads) { thread.start(); }
        for (FileCheckerThread thread : threads) {
            thread.join(1000);
            if (thread.isAlive()) { fail("Timeout waiting for threads"); }
        }

        for (FileCheckerThread thread : threads) {
            assertThat(thread.isDone(), is(true));
            assertThat(thread.artifact.replaceAll("\\\\", "/"), endsWith(JOB_FOLDERS + "/dir.zip"));
        }
    }

    @Test
    public void shouldLogWhenExceptionIsThrownWhileCreatingCacheFile() throws Exception {
        ZipArtifactCache zipArtifactCache1 = spy(zipArtifactCache);
        doThrow(FileAlreadyExistsException.class).when(zipArtifactCache1).createCachedFile(artifactFolder);
        long threadId = zipArtifactCache1.startCacheCreationThread(artifactFolder);
        waitForCacheFileCreation(threadId);
        assertThat(logFixture.allLogs(), containsString("An error occurred while trying to create the artifact zip file of directory"));
        assertThat(logFixture.allLogs(), containsString("FileAlreadyExistsException"));
    }



    @Test public void shouldRecoverFromOldZipTmpFile() throws Exception {
        File cacheDir = new File(folder, "cache/artifacts/" + JOB_FOLDERS);
        cacheDir.mkdirs();
        TestFileUtil.createTestFile(cacheDir, "dir.zip.tmp");

        waitForCacheCreated();
        assertThat(new File(cacheDir, "dir.zip.tmp"), not(exists()));
        new ZipUtil().unzip(new File(cacheDir, "dir.zip"), cacheDir);
        assertThat(new File(cacheDir, "dir/file1"), exists());
    }

    private void waitForCacheCreated() throws Exception {
        int timesTried = 10;
        while (timesTried > 0 && !zipArtifactCache.cacheCreated(artifactFolder)) {
            Thread.sleep(100);
            timesTried--;
        }
        if (timesTried <= 0) { fail("Timeout creating cache"); }
    }

    private void waitForCacheFileCreation(long threadId) throws Exception {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for(Thread thread : threads) {
            if(thread.getId()== threadId){
                thread.join();
            }
        }
    }

    private class FileCheckerThread extends Thread {
        String artifact;

        public boolean isDone() {
            return artifact != null;
        }

        public void run() {
            try {
                while (!zipArtifactCache.cacheCreated(artifactFolder)) {
                    Thread.sleep(1);
                }
                artifact = zipArtifactCache.cachedFile(artifactFolder).getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private TypeSafeMatcher<ZipArtifactCache> cacheCreated(final ArtifactFolder artifactFolder) {
        return new TypeSafeMatcher<ZipArtifactCache>() {
            public boolean matchesSafely(ZipArtifactCache item) {
                try {
                    return item.cacheCreated(artifactFolder);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public void describeTo(Description description) {
                description.appendText("cacheCreated from " + artifactFolder);
            }
        };
    }
}
