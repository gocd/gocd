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
package com.thoughtworks.go.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.time.ZoneId.systemDefault;

public class TestingClock implements Clock {
    private Instant currentTime;
    private final List<Long> sleeps = new ArrayList<>();

    public TestingClock() {
        this(Instant.now());
    }

    public TestingClock(Instant instant) {
        this.currentTime = instant;
    }

    @Override
    public Instant currentTime() {
        return currentTime;
    }

    @Override
    public Date currentUtilDate() {
        return Date.from(currentTime);
    }

    @Override
    public Timestamp currentSqlTimestamp() {
        return new Timestamp(currentTimeMillis());
    }

    @Override
    public LocalDateTime currentLocalDateTime() {
        return LocalDateTime.ofInstant(currentTime(), systemDefault());
    }

    @Override
    public long currentTimeMillis() {
        return currentTime.toEpochMilli();
    }

    @Override
    public void sleepForSeconds(long seconds) {
        sleepForMillis(seconds * 1000);
    }

    @Override
    public void sleepForMillis(long millis) {
        sleeps.add(millis);
    }

    @Override
    public Instant timeoutTime(Timeout timeout) {
        return timeoutTime(timeout.inMillis());
    }

    @Override
    public Instant timeoutTime(long milliSeconds) {
        return addDuration(milliSeconds, ChronoUnit.MILLIS);
    }

    public void addSeconds(int numberOfSeconds) {
        add(numberOfSeconds, ChronoUnit.SECONDS);
    }


    public void addMillis(int millis) {
        add(millis, ChronoUnit.MILLIS);
    }

    private void add(int duration, ChronoUnit unit) {
        currentTime = addDuration(duration, unit);
    }

    private Instant addDuration(long duration, ChronoUnit unit) {
        return currentTime.plus(duration, unit);
    }

    public void setTime(Instant instant) {
        currentTime = instant;
    }

    public List<Long> getSleeps() {
        return sleeps;
    }

}
