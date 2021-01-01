/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.utils.Timeout;
import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.systemDefault;

public class TestingClock implements Clock {
    private Date currentTime;
    private List<Long> sleeps = new ArrayList<>();

    public TestingClock() {
        this(new Date());
    }

    public TestingClock(Date date) {
        this.currentTime = date;
    }

    @Override
    public Date currentTime() {
        return currentTime;
    }

    @Override
    public DateTime currentDateTime() {
        return new DateTime(currentTime);
    }

    @Override
    public Timestamp currentTimestamp() {
        return new Timestamp(currentTimeMillis());
    }

    @Override
    public LocalDateTime currentLocalDateTime() {
        return LocalDateTime.ofInstant(ofEpochMilli(currentTimeMillis()), systemDefault());
    }

    @Override
    public long currentTimeMillis() {
        return currentTime.getTime();
    }

    @Override
    public void sleepForSeconds(long seconds) throws InterruptedException {
        sleepForMillis(seconds * 1000);
    }

    @Override
    public void sleepForMillis(long millis) {
        sleeps.add(millis);
    }

    @Override
    public DateTime timeoutTime(Timeout timeout) {
        return timeoutTime(timeout.inMillis());
    }

    @Override
    public DateTime timeoutTime(long milliSeconds) {
        return new DateTime(addDuration(Calendar.MILLISECOND, (int) milliSeconds));
    }

    public void addYears(int numberOfYears) {
        add(Calendar.YEAR, numberOfYears);
    }

    public void addSeconds(int numberOfSeconds) {
        add(Calendar.SECOND, numberOfSeconds);
    }

    private void add(int axis, int duration) {
        currentTime = addDuration(axis, duration);
    }

    private Date addDuration(int axis, int duration) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTime);
        calendar.add(axis, duration);
        Date date = calendar.getTime();
        return date;
    }

    public void setTime(Date date) {
        currentTime = date;
    }

    public void setTime(DateTime dateTime) {
        setTime(dateTime.toDate());
    }

    public List<Long> getSleeps() {
        return sleeps;
    }

    public void addMillis(int millis) {
        add(Calendar.MILLISECOND, millis);
    }
}
