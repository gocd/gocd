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

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.time.ZoneId.systemDefault;

public class TestingClock implements Clock, AutoCloseable {
    private final List<Long> sleeps = new ArrayList<>();
    private final MockedStatic<SystemTimeClock> mockedClock;

    private Instant currentTime;

    public TestingClock() {
        this(Instant.now());
    }

    public TestingClock(Instant instant) {
        this(instant, null);
    }

    private TestingClock(Instant instant, MockedStatic<SystemTimeClock> mockedStatic) {
        this.currentTime = instant;
        this.mockedClock = mockedStatic;
    }

    public static TestingClock switchForSystem() {
        return switchForSystem(Instant.now());
    }

    public static TestingClock switchForSystem(Instant instant) {
        MockedStatic<SystemTimeClock> mockedClock = Mockito.mockStatic(SystemTimeClock.class);
        TestingClock testingClock = new TestingClock(instant, mockedClock);
        mockedClock.when(SystemTimeClock::get).thenReturn(testingClock);
        return testingClock;
    }

    @Override
    public void close() {
        sleeps.clear();
        if (mockedClock != null) {
            mockedClock.close();
        }
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
    public Instant timeoutTime(Duration timeout) {
        return currentTime.plus(timeout);
    }

    public void addSeconds(long amount) {
        add(Duration.ofSeconds(amount));
    }

    public void addMillis(long amount) {
        add(Duration.ofMillis(amount));
    }

    public void add(Duration duration) {
        currentTime = currentTime.plus(duration);
    }

    public void setTime(Instant instant) {
        currentTime = instant;
    }

    public List<Long> getSleeps() {
        return sleeps;
    }

}
