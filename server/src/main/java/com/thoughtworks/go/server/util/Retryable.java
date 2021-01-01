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
package com.thoughtworks.go.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * Provides generic retryable logic.
 */
public class Retryable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Retryable.class);
    private static long DEFAULT_PERIOD = 500L;

    private Retryable() {
    }

    public static void retry(Predicate<Integer> action, String message, int times) throws TooManyRetriesException {
        retry(action, message, times, DEFAULT_PERIOD);
    }

    /**
     * Simple retry logic with constant delay period, which retries until {@link Predicate} returns true. If you require
     * a backoff instead of static delay, it should be very easy to implement.
     *
     * @param action {@link Predicate<Integer>} defining the action to perform, taking the attempt number as input
     * @param label  a user-defined {@link String} describing the action
     * @param times  the number of times to try
     * @param delay  the period, in milliseconds, to wait between tries
     * @throws TooManyRetriesException when all tries have been performed
     */
    public static void retry(Predicate<Integer> action, String label, int times, long delay) throws TooManyRetriesException {
        if (times < 1) {
            throw new IllegalArgumentException("Retry block must try at least 1 time");
        }

        if (delay < 1) {
            throw new IllegalArgumentException("Must have at least 1 ms delay");
        }

        label = label == null ? "retry block" : label;

        int tries = 0;

        for (; times > tries; ++tries) {
            try {
                if (action.test(tries)) return;
            } catch (Exception e) {
                LOGGER.warn("Attempted to perform {}, but got exception", label, e);
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                LOGGER.warn("Couldn't sleep while testing {}", label, e);
            }
        }

        throw new TooManyRetriesException(String.format("Retried too many times (%d of %d): %s", tries, times, label));
    }


    /**
     * Thrown when number of allowed retries was exceeded
     */
    public static class TooManyRetriesException extends Exception {
        TooManyRetriesException(String message) {
            super(message);
        }
    }
}
