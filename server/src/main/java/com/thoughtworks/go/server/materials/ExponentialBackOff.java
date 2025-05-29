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

public class ExponentialBackOff {
    private static final long DEFAULT_INITIAL_INTERVAL_IN_MILLIS = 5 * 60 * 1000;
    private static final long MAX_RETRY_INTERVAL_IN_MILLIS = 60 * 60 * 1000;
    private final Clock clock;
    private long retryIntervalMillis;
    private final double multiplier;
    private final Instant failureStartTime;
    private Instant lastFailureTime;

    public ExponentialBackOff(double multiplier) {
        this(multiplier, new SystemTimeClock());
    }

    protected ExponentialBackOff(double multiplier, SystemTimeClock clock) {
        this.clock = clock;
        this.retryIntervalMillis = DEFAULT_INITIAL_INTERVAL_IN_MILLIS;
        this.lastFailureTime = this.failureStartTime = now();
        this.multiplier = multiplier;
    }

    public BackOffResult backOffResult() {
        boolean backOff = lastFailureTime
                .plus(this.retryIntervalMillis, MILLIS)
                .isAfter(now());

        return new BackOffResult(backOff, failureStartTime, lastFailureTime,
                lastFailureTime.plus(this.retryIntervalMillis, MILLIS));
    }

    public void failedAgain() {
        Instant now = now();
        this.retryIntervalMillis = retryIntervalMillis(now);
        this.lastFailureTime = now;
    }

    private long retryIntervalMillis(Instant now) {
        long timeBetweenFailures = lastFailureTime.until(now, MILLIS);
        long retryInterval = round(timeBetweenFailures * multiplier);

        return Math.min(retryInterval, MAX_RETRY_INTERVAL_IN_MILLIS);
    }

    private Instant now() {
        return clock.currentTime();
    }
}
