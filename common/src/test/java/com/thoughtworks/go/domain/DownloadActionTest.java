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

import com.thoughtworks.go.remote.work.artifact.WorkDownloader;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class DownloadActionTest {

    private TestingClock clock;
    private FetchHandler fetchHandler;
    private StubGoPublisher publisher;

    @BeforeEach
    public void setUp() {
        clock = new TestingClock();
        fetchHandler = mock(FetchHandler.class);
        publisher = new StubGoPublisher();
    }

    @Test
    public void shouldRetryWhenCreatingFolderZipCache() throws Exception {
        when(fetchHandler.handleResult(200, publisher)).thenReturn(true);
        MockCachingWorkDownloader httpService = new MockCachingWorkDownloader(3);
        DownloadAction downloadAction = new DownloadAction(httpService, publisher, clock);

        downloadAction.perform("foo", fetchHandler);

        assertThat(httpService.timesCalled).isEqualTo(3);
        assertThat(clock.getSleeps()).contains(5000L);
    }

    @Test
    public void shouldRetryThreeTimesWhenDownloadFails() throws Exception {
        when(fetchHandler.handleResult(200, publisher)).thenReturn(true);
        try (LogFixture logging = logFixtureFor(DownloadAction.class, Level.DEBUG)) {
            FailSometimesWorkDownloader httpService = new FailSometimesWorkDownloader(3);
            DownloadAction downloadAction = new DownloadAction(httpService, publisher, clock);
            downloadAction.perform("foo", fetchHandler);

            assertThat(httpService.timesCalled).isEqualTo(4);

            shouldHaveLogged(logging, Level.WARN, "Could not fetch artifact foo.");
            shouldHaveLogged(logging, Level.WARN, "Error was : Caught an exception 'Connection Reset'");

            assertBetween(clock.getSleeps().get(0), 10000L, 20000L);
            assertBetween(clock.getSleeps().get(1), 20000L, 30000L);
            assertBetween(clock.getSleeps().get(2), 30000L, 40000L);
            assertThat(clock.getSleeps().size()).isEqualTo(3);
        }
    }

    @Test
    public void shouldFailAfterFourthTryWhenDownloadFails() {
        try (LogFixture logging = logFixtureFor(DownloadAction.class, Level.DEBUG)) {
            FailSometimesWorkDownloader httpService = new FailSometimesWorkDownloader(99);
            try {
                new DownloadAction(httpService, new StubGoPublisher(), clock).perform("foo", fetchHandler);
                fail("Expected to throw exception after four tries");
            } catch (Exception e) {
                assertThat(httpService.timesCalled).isEqualTo(4);
                shouldHaveLogged(logging, Level.ERROR, "Giving up fetching resource 'foo'. Tried 4 times and failed.");
            }
        }
    }

    @Test
    public void shouldReturnWithoutRetryingArtifactIsNotModified() throws Exception {
        fetchHandler = new FileHandler(new File(""), getSrc());
        WorkDownloader httpService = mock(WorkDownloader.class);
        StubGoPublisher goPublisher = new StubGoPublisher();

        when(httpService.download("foo", fetchHandler)).thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);

        new DownloadAction(httpService, goPublisher, clock).perform("foo", fetchHandler);

        verify(httpService).download("foo", this.fetchHandler);
        verifyNoMoreInteractions(httpService);
        assertThat(goPublisher.getMessage()).contains("Artifact is not modified, skipped fetching it");
    }

    private String getSrc() {
        return "";
    }

    private static class MockCachingWorkDownloader implements WorkDownloader {
        private final int count;
        private int timesCalled = 0;

        MockCachingWorkDownloader(int count) {
            this.count = count;
        }

        @Override
        public int download(String url, FetchHandler handler) {
            timesCalled += 1;
            if (timesCalled < count) {
                return HttpURLConnection.HTTP_ACCEPTED;
            } else {
                return HttpURLConnection.HTTP_OK;
            }
        }
    }

    private void shouldHaveLogged(LogFixture logging, Level level, String message) {
        String result;
        synchronized (logging) {
            result = logging.getLog();
        }
        assertTrue(logging.contains(level, message), "Expected log to contain " + message + " but got:\n" + result);
    }

    private void assertBetween(Long actual, long min, long max) {
        assertThat(actual).isGreaterThanOrEqualTo(min);
        assertThat(actual).isLessThanOrEqualTo(max);
    }

    private static class FailSometimesWorkDownloader implements WorkDownloader {
        private final int count;
        private int timesCalled = 0;

        public FailSometimesWorkDownloader(int count) {
            this.count = count;
        }

        @Override
        public int download(String url, FetchHandler handler) throws IOException {
            timesCalled += 1;
            if (timesCalled <= count) {
                throw new SocketException("Connection Reset");
            } else {
                return HttpURLConnection.HTTP_OK;
            }
        }
    }
}
