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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.server.service.PipelineScheduleQueue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PipelineScheduleQueueMatcher {
    public static Matcher<Integer> numberOfScheduledPipelinesIsAtLeast(final PipelineScheduleQueue pipelineScheduleQueue) {
        return new TypeSafeMatcher<Integer>() {
            private int actualCount;
            public int expectedCount;

            @Override
            public boolean matchesSafely(Integer expectedCount) {
                this.expectedCount = expectedCount;
                this.actualCount = pipelineScheduleQueue.toBeScheduled().size();
                return actualCount >= expectedCount;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("Expect number of scheduled pipelines to be at least %s", expectedCount));
            }
        };
    }

}
