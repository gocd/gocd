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

import lombok.Getter;

import java.time.Instant;

@Getter
public class BackOffResult {
    public static BackOffResult PERMIT = new BackOffResult(false, null, null, null);
    public static BackOffResult DENY = new BackOffResult(true, null, null, null);

    private final Instant lastFailureTime;
    private final Instant failureStartTime;
    private final Instant nextRetryAttempt;
    private final boolean backOff;


    public BackOffResult(boolean backOff, Instant failureStartTime, Instant lastFailureTime, Instant nextRetryAttempt) {
        this.backOff = backOff;
        this.lastFailureTime = lastFailureTime;
        this.failureStartTime = failureStartTime;
        this.nextRetryAttempt = nextRetryAttempt;
    }

    public boolean shouldBackOff() {
        return backOff;
    }
}
