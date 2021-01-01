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
package com.thoughtworks.go.server.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.text.MessageFormat.format;

/*
 * Multiplexes added actions asynchronously and processes them in a single thread.
 *
 * Since actions can be added from different threads, line up all of them on to one thread,
 * for processing, and to make sure that the upstream processes are not blocked.
 */
public class MultiplexingQueueProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiplexingQueueProcessor.class);
    private Thread processorThread;
    protected final BlockingQueue<Action> queue;
    private String queueName;

    public MultiplexingQueueProcessor(String processorNameForLogging) {
        this.queueName = processorNameForLogging;
        queue = new LinkedBlockingQueue<>();
    }

    public void add(Action action) {
        LOGGER.debug("Adding action into {} queue for {}", queueName, action.description());
        queue.add(action);
    }

    public void start() {
        if (processorThread != null) {
            throw new RuntimeException(format("Cannot start queue processor for {0} multiple times.", queueName));
        }

        processorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Action action = queue.take();
                    LOGGER.debug("Acting on item in {} queue for {}", queueName, action.description());

                    long startTime = System.currentTimeMillis();
                    action.call();
                    long endTime = System.currentTimeMillis();

                    LOGGER.debug("Finished acting on item in {} queue for {}. Time taken: {} ms", queueName, action.description(), (endTime - startTime));
                } catch (Exception e) {
                    LOGGER.warn(format("Failed to handle action in {0} queue", queueName), e);
                }
            }
        });
        processorThread.setName(format("{0}-Queue-Processor", queueName));
        processorThread.setDaemon(true);
        processorThread.start();
    }

    public interface Action {
        void call();

        String description();
    }
}
