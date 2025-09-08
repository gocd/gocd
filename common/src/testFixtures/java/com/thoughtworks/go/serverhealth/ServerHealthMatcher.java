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
package com.thoughtworks.go.serverhealth;

import org.assertj.core.api.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerHealthMatcher {

    public static ThrowingConsumer<? super ServerHealthService> containsState(final HealthStateType type) {
        return containsState(type, null, null);
    }

    public static ThrowingConsumer<? super ServerHealthService> containsState(final HealthStateType healthStateType,
                                                                              final HealthStateLevel healthStateLevel,
                                                                              final String message) {
        return serverHealthService -> {
            assertThat(serverHealthService.logsSorted())
                .describedAs("ServerHealthService expected to contain state with type %s, level %s and message %s", healthStateType, healthStateLevel, message)
                .anyMatch(state ->
                    healthStateType.equals(state.getType()) &&
                        (healthStateLevel == null || healthStateLevel.equals(state.getLogLevel())) &&
                        (message == null || message.equals(state.getMessage())));
        };
    }

    public static ThrowingConsumer<? super ServerHealthService> doesNotContainState(final HealthStateType healthStateType) {
        return serverHealthService ->
            assertThat(serverHealthService.logsSorted())
                .describedAs("ServerHealthService contains entry with type %s", healthStateType)
                .noneMatch(state -> healthStateType.equals(state.getType()));
    }

}
