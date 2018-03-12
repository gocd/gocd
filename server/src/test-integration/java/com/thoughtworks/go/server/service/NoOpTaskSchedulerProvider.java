/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.util.scheduling.TaskSchedulerProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@Component
@Profile("test")
public class NoOpTaskSchedulerProvider implements TaskSchedulerProvider {

    @Override
    public TaskScheduler poolScheduler() {
        return new NoOpTaskScheduler();
    }

    private class NoOpTaskScheduler implements TaskScheduler {
        @Override
        public ScheduledFuture schedule(Runnable task, Trigger trigger) {
            return null;
        }

        @Override
        public ScheduledFuture schedule(Runnable task, Date startTime) {
            return null;
        }

        @Override
        public ScheduledFuture scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            return null;
        }

        @Override
        public ScheduledFuture scheduleAtFixedRate(Runnable task, long period) {
            return null;
        }

        @Override
        public ScheduledFuture scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            return null;
        }

        @Override
        public ScheduledFuture scheduleWithFixedDelay(Runnable task, long delay) {
            return null;
        }
    }


}
