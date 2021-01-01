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
package com.thoughtworks.go.domain;

import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * @understands the time taken for a stage to complete
 */
public abstract class RunDuration  {
    public static final PeriodFormatter PERIOD_FORMATTER =
            new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(
                    2).appendHours().appendSeparator(
                    ":").appendMinutes().appendSeparator(":").appendSeconds().toFormatter();
    protected Duration duration;

    public abstract String duration(PeriodFormatter formatter);

    public static final RunDuration IN_PROGRESS_DURATION = new RunDuration() {

        @Override
        public String duration(PeriodFormatter formatter) {
            return "In Progress";
        }
    };

    public static class ActualDuration extends RunDuration {

        public ActualDuration(Duration duration) {
            this.duration = duration;
        }

        @Override
        public String duration(PeriodFormatter formatter) {
            return formatter.print(duration.toPeriod());
        }

        public long getTotalSeconds() {
            Period period = duration.toPeriod();
            return period.getHours()*3600 + period.getMinutes()*60 + period.getSeconds();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RunDuration)) {
            return false;
        }
        RunDuration that = (RunDuration) o;
        return !(duration != null ? !duration.equals(that.duration) : that.duration != null);
    }

    @Override
    public int hashCode() {
        return duration != null ? duration.hashCode() : 0;
    }

    @Override public String toString() {
        return duration(PERIOD_FORMATTER);
    }
}
