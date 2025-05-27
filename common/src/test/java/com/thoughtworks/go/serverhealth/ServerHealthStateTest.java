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
package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.util.SystemTimeClock;
import com.thoughtworks.go.util.TestingClock;
import com.thoughtworks.go.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ServerHealthStateTest {

    private static final HealthStateType HEALTH_STATE_TYPE_IDENTIFIER = HealthStateType.invalidConfig();

    static final ServerHealthState ERROR_SERVER_HEALTH_STATE = ServerHealthState.error("error", "royally screwed",
            HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState WARNING_SERVER_HEALTH_STATE = ServerHealthState.warning("warning", "warning",
            HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState SUCCESS_SERVER_HEALTH_STATE = ServerHealthState.success(HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState ANOTHER_SUCCESS_SERVER_HEALTH_STATE = ServerHealthState.success(
            HEALTH_STATE_TYPE_IDENTIFIER);
    static final ServerHealthState ANOTHER_WARNING_SERVER_HEALTH_STATE = ServerHealthState.warning("different warning", "my friend warning",
            HEALTH_STATE_TYPE_IDENTIFIER);
    private TestingClock testingClock;

    @BeforeEach
    public void setUp() {
        testingClock = new TestingClock();
        ServerHealthState.clock = testingClock;
    }

    @AfterEach
    public void tearDown() {
        ServerHealthState.clock = new SystemTimeClock();
    }

    @Test
    public void shouldTrumpSuccessIfCurrentIsWarning() {
        assertThat(SUCCESS_SERVER_HEALTH_STATE.trump(WARNING_SERVER_HEALTH_STATE)).isEqualTo(WARNING_SERVER_HEALTH_STATE);
    }

    @Test
    public void shouldTrumpSuccessIfCurrentIsSuccess() {
        assertThat(SUCCESS_SERVER_HEALTH_STATE.trump(ANOTHER_SUCCESS_SERVER_HEALTH_STATE)).isEqualTo(
                ANOTHER_SUCCESS_SERVER_HEALTH_STATE);
    }

    @Test
    public void shouldTrumpWarningIfCurrentIsWarning() {
        assertThat(ANOTHER_WARNING_SERVER_HEALTH_STATE.trump(WARNING_SERVER_HEALTH_STATE)).isEqualTo(
                WARNING_SERVER_HEALTH_STATE);
    }

    @Test
    public void shouldNotTrumpWarningIfCurrentIsSuccess() {
        assertThat(WARNING_SERVER_HEALTH_STATE.trump(SUCCESS_SERVER_HEALTH_STATE)).isEqualTo(WARNING_SERVER_HEALTH_STATE);
    }

    @Test
    public void shouldNotTrumpErrorIfCurrentIsSuccess() {
        assertThat(ERROR_SERVER_HEALTH_STATE.trump(SUCCESS_SERVER_HEALTH_STATE)).isEqualTo(ERROR_SERVER_HEALTH_STATE);
    }

    @Test
    public void shouldNotTrumpErrorIfCurrentIsWarning() {
        assertThat(ERROR_SERVER_HEALTH_STATE.trump(WARNING_SERVER_HEALTH_STATE)).isEqualTo(ERROR_SERVER_HEALTH_STATE);
    }

    @Test
    public void shouldExpireAfterTheExpiryTime() {
        testingClock.setTime(Instant.now());
        ServerHealthState expireInFiveMins = ServerHealthState.warning("message", "desc", HealthStateType.databaseDiskFull(), Timeout.FIVE_MINUTES);
        ServerHealthState expireNever = ServerHealthState.warning("message", "desc", HealthStateType.databaseDiskFull());
        assertThat(expireInFiveMins.hasExpired()).isFalse();
        testingClock.addMillis((int) Timeout.TWO_MINUTES.inMillis());
        assertThat(expireInFiveMins.hasExpired()).isFalse();
        testingClock.addMillis((int) Timeout.THREE_MINUTES.inMillis());
        assertThat(expireInFiveMins.hasExpired()).isFalse();
        testingClock.addMillis(10);
        assertThat(expireInFiveMins.hasExpired()).isTrue();
        testingClock.addMillis(999999999);
        assertThat(expireNever.hasExpired()).isFalse();
    }

    @Test
    public void shouldUnderstandEquality() {
        ServerHealthState fooError = ServerHealthState.error("my message", "my description", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        ServerHealthState fooErrorCopy = ServerHealthState.error("my message", "my description", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        assertThat(fooError).isEqualTo(fooErrorCopy);
    }

    @Test
    public void shouldNotAllowNullMessage() {
        ServerHealthState nullError = null;
        try {
            nullError = ServerHealthState.error(null, "some desc", HealthStateType.general(HealthStateScope.forPipeline("foo")));
            fail("should have bombed as message given is null");
        } catch(Exception e) {
            assertThat(nullError).isNull();
            assertThat(e.getMessage()).isEqualTo("message cannot be null");
        }
    }

   @Test
    public void shouldGetMessageWithTimestamp() {
        ServerHealthState errorState = ServerHealthState.error("my message", "my description", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        assertThat(errorState.getMessageWithTimestamp()).isEqualTo("my message" + " [" + ServerHealthState.TIMESTAMP_FORMAT.format(errorState.getTimestamp()) + "]");
    }

    @Test
    public void shouldEscapeErrorMessageAndDescriptionByDefault() {
        ServerHealthState errorState = ServerHealthState.error("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")));

        assertThat(errorState.getMessage()).isEqualTo("&quot;&lt;message1 &amp; message2&gt;&quot;");
        assertThat(errorState.getDescription()).isEqualTo("&quot;&lt;message1 &amp; message2&gt;&quot;");
    }

    @Test
    public void shouldEscapeWarningMessageAndDescriptionByDefault() {
        ServerHealthState warningStateWithoutTimeout = ServerHealthState.warning("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        ServerHealthState warningStateWithTimeout = ServerHealthState.warning("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")), Timeout.TEN_SECONDS);
        ServerHealthState warningState = ServerHealthState.warning("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")), 15L);

        assertThat(warningStateWithoutTimeout.getMessage()).isEqualTo("&quot;&lt;message1 &amp; message2&gt;&quot;");
        assertThat(warningStateWithoutTimeout.getDescription()).isEqualTo("&quot;&lt;message1 &amp; message2&gt;&quot;");

        assertThat(warningStateWithTimeout.getMessage()).isEqualTo("\"<message1 & message2>\"");
        assertThat(warningStateWithTimeout.getDescription()).isEqualTo("\"<message1 & message2>\"");

        assertThat(warningState.getMessage()).isEqualTo("&quot;&lt;message1 &amp; message2&gt;&quot;");
        assertThat(warningState.getDescription()).isEqualTo("&quot;&lt;message1 &amp; message2&gt;&quot;");
    }

    @Test
    public void shouldPreserverHtmlInWarningMessageAndDescription() {
        ServerHealthState warningState = ServerHealthState.warningWithHtml("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")));
        ServerHealthState warningStateWithTime = ServerHealthState.warningWithHtml("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")), 15L);

        assertThat(warningState.getMessage()).isEqualTo("\"<message1 & message2>\"");
        assertThat(warningState.getDescription()).isEqualTo("\"<message1 & message2>\"");
        assertThat(warningStateWithTime.getMessage()).isEqualTo("\"<message1 & message2>\"");
        assertThat(warningStateWithTime.getDescription()).isEqualTo("\"<message1 & message2>\"");
    }

    @Test
    public void shouldPreserveHtmlInErrorMessageAndDescription() {
        ServerHealthState error = ServerHealthState.errorWithHtml("\"<message1 & message2>\"", "\"<message1 & message2>\"", HealthStateType.general(HealthStateScope.forPipeline("foo")));

        assertThat(error.getMessage()).isEqualTo("\"<message1 & message2>\"");
        assertThat(error.getDescription()).isEqualTo("\"<message1 & message2>\"");
    }
}
