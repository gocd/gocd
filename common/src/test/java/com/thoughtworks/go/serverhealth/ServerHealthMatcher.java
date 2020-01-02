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
package com.thoughtworks.go.serverhealth;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ServerHealthMatcher {

    public static Matcher<ServerHealthService> containsState(final HealthStateType type) {
        return containsState(type, null, null);
    }

    public static Matcher<ServerHealthService> containsState(final HealthStateType healthStateType,
                                                        final HealthStateLevel healthStateLevel,
                                                        final String message) {
        return new TypeSafeMatcher<ServerHealthService>() {
            private List<ServerHealthState> allLogs;
            private ServerHealthState entry;
            private boolean levelMatches;
            private boolean messageMatches;

            @Override
            public boolean matchesSafely(ServerHealthService item) {
                allLogs = item.logs();
                for (ServerHealthState serverHealthState : allLogs) {
                    if (serverHealthState.getType().equals(healthStateType)) {
                        entry = serverHealthState;
                    }
                }
                if (!(entry != null)) {
                    return false;
                } else {
                    levelMatches = healthStateLevel == null || healthStateLevel.equals(entry.getLogLevel());
                    messageMatches = message == null || message.equals(entry.getMessage());
                    return levelMatches && messageMatches;
                }
            }

            @Override
            public void describeTo(Description description) {
                if (entry == null) {
                    description.appendText("Can not find result with " + healthStateType + " in all logs: " + allLogs);
                } else {
                    if (!levelMatches) {
                        description.appendText("Level was " + entry.getLogLevel() + " instead of " + healthStateLevel);
                    }
                    if (!messageMatches) {
                        description.appendText("Message was: \n" + entry.getMessage() + "\n instead of:\n" + message);
                    }
                }
            }
        };
    }

    public static Matcher<ServerHealthService> doesNotContainState(final HealthStateType healthStateType) {
        return new TypeSafeMatcher<ServerHealthService>() {
            private ServerHealthState entry;

            @Override
            public boolean matchesSafely(ServerHealthService item) {
                for (ServerHealthState serverHealthState : item.logs()) {
                    if (serverHealthState.getType().equals(healthStateType)) {
                        entry = serverHealthState;
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("HealthStateType " + healthStateType + " contains: " + entry + "\n" + "With level: " + entry.getLogLevel());
            }
        };
    }

}
