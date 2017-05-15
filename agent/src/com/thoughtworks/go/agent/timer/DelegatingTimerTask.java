/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.TimerTask;

public class DelegatingTimerTask extends TimerTask {
    private static final Log logger = LogFactory.getLog(DelegatingTimerTask.class);
    private final Runnable delegate;

    public DelegatingTimerTask(Runnable delegate) {
        Assert.notNull(delegate, "Delegate must not be null");
        this.delegate = delegate;
    }

    public final Runnable getDelegate() {
        return this.delegate;
    }

    public void run() {
        try {
            this.delegate.run();
        } catch (Throwable var2) {
            logger.error("Unexpected exception thrown from Runnable: " + this.delegate, var2);
        }

    }
}
