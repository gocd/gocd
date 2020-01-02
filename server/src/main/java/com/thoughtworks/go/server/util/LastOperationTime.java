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
package com.thoughtworks.go.server.util;

/**
 * @understands when an operation was last performed and how long it needs to wait till it retries
 */
public class LastOperationTime {

    private final long waitFor;
    private long recentOperationTime;

    public LastOperationTime(long waitInterval) {
        waitFor = waitInterval;
        recentOperationTime = 0;
    }

    public boolean pastWaitingPeriod() {
        return (System.currentTimeMillis() - recentOperationTime) > waitFor;
    }

    public void refresh() {
        recentOperationTime = System.currentTimeMillis();
    }
}
