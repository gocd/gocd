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

package com.thoughtworks.go.domain;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;

import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.TestingClock;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DownloadActionTest {

    private TestingClock clock;
    private FetchHandler fetchHandler;
    private StubGoPublisher publisher;

    @Before
    public void setUp() throws Exception {
        clock = new TestingClock();
        fetchHandler = mock(FetchHandler.class);
        publisher = new StubGoPublisher();
    }

    @Test public void shouldRetryWhenCreatingFolderZipCache() throws Exception {
        when(fetchHandler.handleResult(200, publisher)).thenReturn(true);
        MockCachingFetchZipHttpService httpService = new MockCachingFetchZipHttpService(3);
        DownloadAction downloadAction = new DownloadAction(httpService, publisher, clock);

        downloadAction.perform("foo", fetchHandler);

        assertThat(httpService.timesCalled, is(3));
        assertThat(clock.getSleeps(), hasItems(5000L));
    }

    @Test
    public void shouldRetryThreeTimesWhenDownloadFails() throws Exception {
        when(fetchHandler.handleResult(200, publisher)).thenReturn(true);
        LogFixture logging = LogFixture.startListening();

        FailSometimesHttpService httpService = new FailSometimesHttpService(3);
        DownloadAction downloadAction = new DownloadAction(httpService, publisher, clock);
        downloadAction.perform("foo", fetchHandler);

        assertThat(httpService.timesCalled, is(4));

        shouldHaveLogged(logging, Level.WARN, "Could not fetch artifact foo.");
        shouldHaveLogged(logging, Level.WARN, "Error was : Caught an exception 'Connection Reset'");

        assertBetween(clock.getSleeps().get(0), 10000L, 20000L);
        assertBetween(clock.getSleeps().get(1), 20000L, 30000L);
        assertBetween(clock.getSleeps().get(2), 30000L, 40000L);
        assertThat(clock.getSleeps().size(), is(3));

        logging.stopListening();
    }

    @Test
    public void shouldFailAfterFourthTryWhenDownloadFails() throws Exception {
        LogFixture logging = LogFixture.startListening();

        FailSometimesHttpService httpService = new FailSometimesHttpService(99);
        try {
            new DownloadAction(httpService, new StubGoPublisher(), clock).perform("foo", fetchHandler);
            fail("Expected to throw exception after four tries");
        } catch (Exception e) {
            assertThat(httpService.timesCalled, is(4));
            shouldHaveLogged(logging, Level.ERROR, "Giving up fetching resource 'foo'. Tried 4 times and failed.");
        }
        logging.stopListening();
    }

    @Test
    public void shouldReturnWithoutRetryingArtifactIsNotModified() throws Exception {
        fetchHandler = new FileHandler(new File(""), getSrc());
        HttpService httpService = mock(HttpService.class);
        StubGoPublisher goPublisher = new StubGoPublisher();

        when(httpService.download("foo", fetchHandler)).thenReturn(SC_NOT_MODIFIED);

        new DownloadAction(httpService, goPublisher, clock).perform("foo", fetchHandler);

        verify(httpService).download("foo", this.fetchHandler);
        verifyNoMoreInteractions(httpService);
        assertThat(goPublisher.getMessage(), containsString("Artifact is not modified, skipped fetching it"));
    }

    private String getSrc() {
        return "";
    }

    private class MockCachingFetchZipHttpService extends HttpService {
        private final int count;
        private int timesCalled = 0;

        MockCachingFetchZipHttpService(int count) {
            this.count = count;
        }

        public int download(String url, FetchHandler handler) throws IOException {
            timesCalled += 1;
            if (timesCalled < count) {
                return SC_ACCEPTED;
            } else {
                return SC_OK;
            }
        }
    }

    private void shouldHaveLogged(LogFixture logging, Level level, String message) {
        Assert.assertTrue(
                "Expected log to contain " + message + " but got:\n" + logging.allLogs(),
                logging.contains(level, message));
    }

    private void assertBetween(Long actual, long min, long max) {
        assertThat(actual, greaterThanOrEqualTo(min));
        assertThat(actual, lessThanOrEqualTo(max));
    }

    private class FailSometimesHttpService extends HttpService {
        private int count;
        private int timesCalled = 0;

        public FailSometimesHttpService(int count) {
            this.count = count;
        }

        public int download(String url, FetchHandler handler) throws IOException {
            timesCalled += 1;
            if (timesCalled <= count) {
                throw new SocketException("Connection Reset");
            } else {
                return SC_OK;
            }
        }
    }
}
