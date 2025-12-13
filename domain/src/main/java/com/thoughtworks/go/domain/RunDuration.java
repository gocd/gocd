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
package com.thoughtworks.go.domain;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * Understands the time taken for a stage to complete
 */
public abstract class RunDuration  {
    protected final Duration duration;

    protected RunDuration(Duration duration) {
        this.duration = duration;
    }

    public abstract String duration(Function<Duration, String> formatter);

    public static final RunDuration IN_PROGRESS_DURATION = new RunDuration(null) {

        @Override
        public String duration(Function<Duration, String> formatter) {
            return "In Progress";
        }
    };

    public static class ActualDuration extends RunDuration {

        public ActualDuration(Duration duration) {
            super(duration);
        }

        @Override
        public String duration(Function<Duration, String> formatter) {
            return formatter.apply(duration);
        }

        public long getTotalSeconds() {
            return duration.getSeconds();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RunDuration that)) {
            return false;
        }
        return Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return duration != null ? duration.hashCode() : 0;
    }
}
