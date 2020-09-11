/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SystemTimeClock;

import java.time.LocalDateTime;

import static java.lang.Math.round;
import static java.time.temporal.ChronoUnit.MILLIS;

public class ExponentialBackOff {
    private LocalDateTime lastFailureTime;
    private LocalDateTime failureStartTime;
    private long retryInterval;
    private final long DEFAULT_INITIAL_INTERVAL_IN_MILLIS = 5 * 60 * 1000;
    private final long MAX_RETRY_INTERVAL_IN_MILLIS = 60 * 60 * 1000;
    double multiplier;
    private SystemTimeClock clock;

    public ExponentialBackOff(double multiplier) {
        this(multiplier, new SystemTimeClock());
    }

    protected ExponentialBackOff(double multiplier, SystemTimeClock clock) {
        this.clock = clock;
        this.retryInterval = DEFAULT_INITIAL_INTERVAL_IN_MILLIS;
        this.lastFailureTime = this.failureStartTime = now();
        this.multiplier = multiplier;
    }

    public BackOffResult backOffResult() {
        boolean backOff = lastFailureTime
                .plus(this.retryInterval, MILLIS)
                .isAfter(now());

        return new BackOffResult(backOff, failureStartTime, lastFailureTime,
                lastFailureTime.plus(this.retryInterval, MILLIS));
    }

    public void failedAgain() {
        LocalDateTime now = now();
        this.retryInterval = retryInterval(now);
        this.lastFailureTime = now;
    }

    private long retryInterval(LocalDateTime now) {
        long timeBetweenFailures = lastFailureTime.until(now, MILLIS);
        long retryInterval = round(timeBetweenFailures * multiplier);

        return retryInterval > MAX_RETRY_INTERVAL_IN_MILLIS
                ? MAX_RETRY_INTERVAL_IN_MILLIS
                : retryInterval;
    }

    private LocalDateTime now() {
        return clock.currentLocalDateTime();
    }
}
