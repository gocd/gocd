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
package com.thoughtworks.go.utils;

public enum Timeout {
    ZERO_SECOND(0),
    ONE_SECOND(1000),
    FIVE_SECONDS(5 * ONE_SECOND.timeout),
    TEN_SECONDS(2 * FIVE_SECONDS.timeout),
    NINETY_SECONDS(90 * ONE_SECOND.timeout),
    TWENTY_SECONDS(20 * ONE_SECOND.timeout),
    THIRTY_SECONDS(30 * ONE_SECOND.timeout),
    ONE_MINUTE(60 * ONE_SECOND.timeout),
    TWO_MINUTES(2 * ONE_MINUTE.timeout),
    THREE_MINUTES(3 * ONE_MINUTE.timeout),
    FIVE_MINUTES(5 * ONE_MINUTE.timeout),
    TEN_MINUTES(2 * FIVE_MINUTES.timeout),
    ONE_HOUR(60 * ONE_MINUTE.timeout),
    ONE_DAY(24 * ONE_HOUR.timeout),
    NEVER(Integer.MAX_VALUE);

    private long timeout;

    private Timeout(long timeout) {
        this.timeout = timeout;
    }

    public long inMillis() {
        return timeout;
    }
}
