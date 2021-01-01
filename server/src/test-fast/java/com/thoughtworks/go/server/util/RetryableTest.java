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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class RetryableTest {
    @Test
    public void retryThrowsExceptionWhen() throws Exception {
        final List<Integer> attempts = new ArrayList<Integer>();
        boolean raised = false;

        try {
            Retryable.retry(new Predicate<Integer>() {
                @Override
                public boolean test(Integer integer) {
                    attempts.add(integer);
                    return false;
                }
            }, "testing retries", 3, 1L);
        } catch (Retryable.TooManyRetriesException e) {
            raised = true;
        }

        Integer[] expected = {0, 1, 2};
        Integer[] actual = new Integer[3];

        assertEquals(3, attempts.size());
        assertArrayEquals("Should have logged all attempts", expected, attempts.toArray(actual));
        assertTrue("Did not throw Retryable.TooManyRetriesException", raised);
    }

    @Test
    public void retryStopsWhenSuccessful() throws Exception {
        final List<Integer> attempts = new ArrayList<Integer>();
        boolean raised = false;

        try {
            Retryable.retry(new Predicate<Integer>() {
                @Override
                public boolean test(Integer integer) {
                    attempts.add(integer);
                    return 1 == integer;
                }
            }, "testing retries", 3, 1L);
        } catch (Retryable.TooManyRetriesException e) {
            raised = true;
        }


        Integer[] expected = {0, 1};
        Integer[] actual = new Integer[2];

        assertEquals(2, attempts.size());
        assertArrayEquals("Should have logged all attempts", expected, attempts.toArray(actual));
        assertFalse("Should not have thrown Retryable.TooManyRetriesException", raised);
    }
}