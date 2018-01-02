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

package com.thoughtworks.go.util;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.thoughtworks.go.utils.Timeout;
import org.joda.time.DateTime;

public class TestingClock implements Clock {
    private Date currentTime;
    private List<Long> sleeps = new ArrayList<>();

    public TestingClock() {
        this(new Date());
    }

    public TestingClock(Date date) {
        this.currentTime = date;
    }

    public Date currentTime() {
        return currentTime;
    }

    public DateTime currentDateTime() {
        return new DateTime(currentTime);
    }

    public long currentTimeMillis() {
        return currentTime.getTime();
    }

    public void sleepForSeconds(long seconds) throws InterruptedException {
        sleepForMillis(seconds * 1000);
    }

    public void sleepForMillis(long millis) throws InterruptedException {
        sleeps.add(millis);
    }

    public DateTime timeoutTime(Timeout timeout) {
        return timeoutTime(timeout.inMillis());
    }

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
