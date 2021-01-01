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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

class ModeAwareRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModeAwareRunnable.class);
    private final String finalRunnableName;
    private final SystemEnvironment systemEnvironment;
    private final Runnable runnable;

    ModeAwareRunnable(Runnable runnable, SystemEnvironment systemEnvironment) {
        this.runnable = runnable;
        this.finalRunnableName = runnableName(runnable);
        this.systemEnvironment = systemEnvironment;
    }

    private static String runnableName(Runnable runnable) {
        String runnableName = runnable.toString();

        if (runnable instanceof ScheduledMethodRunnable) {
            ScheduledMethodRunnable name = (ScheduledMethodRunnable) runnable;
            runnableName = name.getTarget().getClass().getSimpleName() + "#" + name.getMethod().getName();
        }
        return runnableName;
    }

    @Override
    public void run() {
        if (systemEnvironment.isServerActive()) {
            LOGGER.debug("Server is in active state, running: {}", finalRunnableName);
            try {
                runnable.run();
            } catch (Exception e) {
                LOGGER.error("There was an error running {}", finalRunnableName, e);
            }
        } else {
            LOGGER.debug("Server is not in active state, skipping: {}", finalRunnableName);
        }
    }
}
