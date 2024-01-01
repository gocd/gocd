/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.remote.work.*;

import java.util.Map;

enum WorkAttempt {
    OK, NOTHING_TO_DO, FAILED;

    private static final Map<Class<? extends Work>, WorkAttempt> WORK_TO_RESULT = Map.of(
        BuildWork.class, OK,
        NoWork.class, NOTHING_TO_DO,
        DeniedAgentWork.class, NOTHING_TO_DO,
        UnregisteredAgentWork.class, FAILED
    );

    public static WorkAttempt fromWork(Work work) {
        return WORK_TO_RESULT.getOrDefault(work.getClass(), OK);
    }

    boolean shouldResetDelay() {
        // Reset backoff delays once we have executed real work successfully
        return OK.equals(this);
    }
}
