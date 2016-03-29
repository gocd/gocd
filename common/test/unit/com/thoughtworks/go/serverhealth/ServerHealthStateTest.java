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

package com.thoughtworks.go.serverhealth;

import java.util.Date;

import com.thoughtworks.go.util.SystemTimeClock;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.utils.Timeout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ServerHealthStateTest {

    private static final HealthStateType HEALTH_STATE_TYPE_IDENTIFIER = HealthStateType.invalidConfig();

    static final ServerHealthState ERROR_SERVER_HEALTH_STATE = ServerHealthState.error("error", "royally screwed",
            HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState WARNING_SERVER_HEALTH_STATE = ServerHealthState.warning("warning", "warning",
            HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState ANOTHER_ERROR_SERVER_HEALTH_STATE = ServerHealthState.error("Second", "Hi World",
            HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState SUCCESS_SERVER_HEALTH_STATE = ServerHealthState.success(HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState ANOTHER_SUCCESS_SERVER_HEALTH_STATE = ServerHealthState.success(
            HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState ANOTHER_WARNING_SERVER_HEALTH_STATE = ServerHealthState.warning("different warning", "my friend warning",
            HEALTH_STATE_TYPE_IDENTIFIER);
    private TestingClock testingClock;

    @Before
    public void setUp() {
        testingClock = new TestingClock();
        ServerHealthState.clock = testingClock;
    }

    @After
    public void tearDown() {
        ServerHealthState.clock = new SystemTimeClock();
    }

    @Test
    public void shouldTrumpSuccessIfCurrentIsWarning() {
        assertThat(SUCCESS_SERVER_HEALTH_STATE.trump(WARNING_SERVER_HEALTH_STATE), is(WARNING_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldTrumpSuccessIfCurrentIsSuccess() {
        assertThat(SUCCESS_SERVER_HEALTH_STATE.trump(ANOTHER_SUCCESS_SERVER_HEALTH_STATE), is(
                ANOTHER_SUCCESS_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldTrumpWarningIfCurrentIsWarning() {
        assertThat(ANOTHER_WARNING_SERVER_HEALTH_STATE.trump(WARNING_SERVER_HEALTH_STATE), is(
                WARNING_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldNotTrumpWarningIfCurrentIsSuccess() {
        assertThat(WARNING_SERVER_HEALTH_STATE.trump(SUCCESS_SERVER_HEALTH_STATE), is(WARNING_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldNotTrumpErrorIfCurrentIsSuccess() {
        assertThat(ERROR_SERVER_HEALTH_STATE.trump(SUCCESS_SERVER_HEALTH_STATE), is(ERROR_SERVER_HEALTH_STATE));
    }
    
    @Test
    public void shouldtNotTrumpErrorIfCurrentIsWarning() {
        assertThat(ERROR_SERVER_HEALTH_STATE.trump(WARNING_SERVER_HEALTH_STATE), is(ERROR_SERVER_HEALTH_STATE));
    }

    @Test
    public void shouldExpireAfterTheExpiryTime() throws Exception {
        testingClock.setTime(new Date());
        ServerHealthState expireInFiveMins = ServerHealthState.warning("message", "desc", HealthStateType.databaseDiskFull(), Timeout.FIVE_MINUTES);
        ServerHealthState expireNever = ServerHealthState.warning("message", "desc", HealthStateType.databaseDiskFull());
        assertThat(expireInFiveMins.hasExpired(),is(false));
        testingClock.addMillis((int) Timeout.TWO_MINUTES.inMillis());
        assertThat(expireInFiveMins.hasExpired(),is(false));
        testingClock.addMillis((int) Timeout.THREE_MINUTES.inMillis());
        assertThat(expireInFiveMins.hasExpired(),is(false));
        testingClock.addMillis(10);
        assertThat(expireInFiveMins.hasExpired(),is(true));
        testingClock.addMillis(999999999);
        assertThat(expireNever.hasExpired(),is(false));
    }

    @Test
    public void shouldUnderstandEquality() {
        ServerHealthState fooError = ServerHealthState.error("my message", "my description", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        ServerHealthState fooErrorCopy = ServerHealthState.error("my message", "my description", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        assertThat(fooError, is(fooErrorCopy));
    }
    
    @Test
    public void shouldNotAllowNullMessage() {
        ServerHealthState nullError = null;
        try {
            nullError = ServerHealthState.error(null, "some desc", HealthStateType.general(HealthStateScope.forPipeline("foo")));
            fail("should have bombed as message given is null");
        } catch(Exception e) {
            assertThat(nullError, is(nullValue()));
            assertThat(e.getMessage(), is("message cannot be null"));
        }
    }

   @Test
    public void shouldGetMessageWithTimestamp() {
        ServerHealthState errorState = ServerHealthState.error("my message", "my description", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        assertThat(errorState.getMessageWithTimestamp(), is("my message" + " [" + ServerHealthState.TIMESTAMP_FORMAT.format(errorState.getTimestamp()) + "]"));
    }

    @Test
    public void shouldEscapeErrorMessageAndDescriptionByDefault() {
        ServerHealthState errorState = ServerHealthState.error("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")));

        assertThat(errorState.getMessage(), is("&quot;&lt;message1 &amp; message2&gt;&quot;"));
        assertThat(errorState.getDescription(), is("&quot;&lt;message1 &amp; message2&gt;&quot;"));
    }

    @Test
    public void shouldEscapeWarningMessageAndDescriptionByDefault() {
        ServerHealthState warningStateWithoutTimeout = ServerHealthState.warning("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        ServerHealthState warningStateWithTimeout = ServerHealthState.warning("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")), Timeout.TEN_SECONDS);
        ServerHealthState warningState = ServerHealthState.warning("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")), 15L);

        assertThat(warningStateWithoutTimeout.getMessage(), is("&quot;&lt;message1 &amp; message2&gt;&quot;"));
        assertThat(warningStateWithoutTimeout.getDescription(), is("&quot;&lt;message1 &amp; message2&gt;&quot;"));

        assertThat(warningStateWithTimeout.getMessage(), is("\"<message1 & message2>\""));
        assertThat(warningStateWithTimeout.getDescription(), is("\"<message1 & message2>\""));

        assertThat(warningState.getMessage(), is("&quot;&lt;message1 &amp; message2&gt;&quot;"));
        assertThat(warningState.getDescription(), is("&quot;&lt;message1 &amp; message2&gt;&quot;"));
    }

    @Test
    public void shouldPreserverHtmlInWarningMessageAndDescription() {
        ServerHealthState warningState = ServerHealthState.warningWithHtml("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        ServerHealthState warningStateWithTime = ServerHealthState.warningWithHtml("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")), 15L);

        assertThat(warningState.getMessage(), is("\"<message1 & message2>\""));
        assertThat(warningState.getDescription(), is("\"<message1 & message2>\""));
        assertThat(warningStateWithTime.getMessage(), is("\"<message1 & message2>\""));
        assertThat(warningStateWithTime.getDescription(), is("\"<message1 & message2>\""));
    }
}
