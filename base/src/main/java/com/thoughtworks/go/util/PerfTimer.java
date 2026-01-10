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
package com.thoughtworks.go.util;

import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;

public class PerfTimer {
    public static PerfTimer start(Logger logger, String message) {
        return start(logger, message, new SystemTimeClock());
    }

    @VisibleForTesting
    static PerfTimer start(Logger logger, String message, Clock clock) {
        PerfTimer timer = new PerfTimer(logger, message, clock);
        timer.start();
        return timer;
    }

    private final Logger logger;
    private final String message;
    private final Clock clock;

    private long startTime;
    private long elapsed;

    private PerfTimer(Logger logger, String message, Clock clock) {
        this.logger = logger;
        this.message = message;
        this.clock = clock;
    }

    private void start() {
        startTime = clock.currentTimeMillis();
    }

    public void stop() {
        if (startTime == 0) {
            return;
        }
        elapsed = elapsed + clock.currentTimeMillis() - startTime;
        startTime = 0;
        logger.info("{} took {} ms", message, elapsed());
    }

    public long elapsed() {
        if (startTime > 0) {
            stop();
        }
        return elapsed;
    }

}
