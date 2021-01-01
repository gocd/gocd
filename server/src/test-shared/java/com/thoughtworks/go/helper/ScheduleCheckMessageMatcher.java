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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.server.scheduling.ScheduleCheckMessage;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.argThat;

public class ScheduleCheckMessageMatcher {
    public static ScheduleCheckMessage matchScheduleCheckMessage(final String expectedPipelineName) {
        return argThat(new ArgumentMatcher<ScheduleCheckMessage>() {

            @Override
            public boolean matches(ScheduleCheckMessage o) {
                return expectedPipelineName.equals(o.getPipelineName());
            }

            @Override
            public String toString() {
                return "matchScheduleCheckMessage(" + expectedPipelineName + ")";
            }
        });
    }
}
