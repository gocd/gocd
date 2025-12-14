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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemTimeClock;

import java.time.Instant;

import static java.lang.Math.round;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class ExponentialBackOff {
    private static final long DEFAULT_INITIAL_INTERVAL_IN_MILLIS = MINUTES.toMillis(5);
    static final long MAX_RETRY_INTERVAL_IN_MILLIS = MINUTES.toMillis(60);

    private final Clock clock;
    private final float multiplier;
    private final Instant failureStartTime;

    private long retryIntervalMillis;
    private Instant lastFailureTime;

    public ExponentialBackOff(float multiplier) {
        this(multiplier, new SystemTimeClock());
    }

    protected ExponentialBackOff(float multiplier, SystemTimeClock clock) {
        this.clock = clock;
        this.retryIntervalMillis = DEFAULT_INITIAL_INTERVAL_IN_MILLIS;
        Instant now = now();
        this.lastFailureTime = now;
        this.failureStartTime = now;
        this.multiplier = multiplier;
    }

    public BackOffResult backOffResult() {
        Instant nextAttempt = lastFailureTime.plus(this.retryIntervalMillis, MILLIS);
        boolean backOff = nextAttempt.isAfter(now());

        return new BackOffResult(backOff, failureStartTime, lastFailureTime, nextAttempt);
    }

    public void failedAgain() {
        Instant now = now();
        this.retryIntervalMillis = nextRetryIntervalMillis(lastFailureTime, now);
        this.lastFailureTime = now;
    }

    private long nextRetryIntervalMillis(Instant lastFailureTime, Instant now) {
        long millisSinceLastFailure = lastFailureTime.until(now, MILLIS);
        long retryIntervalMillis = round(millisSinceLastFailure * (double) multiplier);

        return Math.min(retryIntervalMillis, MAX_RETRY_INTERVAL_IN_MILLIS);
    }

    private Instant now() {
        return clock.currentTime();
    }
}
