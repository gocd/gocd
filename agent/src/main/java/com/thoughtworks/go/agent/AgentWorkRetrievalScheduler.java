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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
public class AgentWorkRetrievalScheduler implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AgentWorkRetrievalScheduler.class);

    private final AgentController controller;
    private final BackOff backoffStrategy;
    private final TaskScheduler scheduler;

    @Autowired
    public AgentWorkRetrievalScheduler(AgentController controller, BackOff backoffStrategy, TaskScheduler scheduler) {
        this.controller = controller;
        this.backoffStrategy = backoffStrategy;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void schedule() {
        // Schedule constantly, to ensure any unexpected exceptions in the loop don't cause a zombie agent
        scheduler.schedule(this, new PeriodicTrigger(1, TimeUnit.MILLISECONDS));
    }

    @Override
    public void run() {
        BackOffExecution backOffExecution = backoffStrategy.start();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long nextBackOffMillis = backOffExecution.nextBackOff();
                LOG.debug("[Agent Loop] Waiting {} ms before retrieving next work.", nextBackOffMillis);
                waitFor(nextBackOffMillis);
                WorkAttempt result = controller.performWork();
                LOG.debug("[Agent Loop] Work attempted was {}", result);
                if (result.shouldResetDelay()) {
                    backOffExecution = backoffStrategy.start();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void waitFor(long waitMillis) throws InterruptedException {
        Thread.sleep(waitMillis);
    }

}
