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
package com.thoughtworks.go.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DynamicReadWriteLockTest {
    private DynamicReadWriteLock readWriteLock;
    private volatile int numberOfLocks;

    @BeforeEach
    public void setUp() {
        readWriteLock = new DynamicReadWriteLock();
        numberOfLocks = 0;
    }

    @Test
    public void shouldEnforceMutualExclusionOfWriteLockForGivenName() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(() -> {
            readWriteLock.acquireWriteLock("foo");
            numberOfLocks++;
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(0));
    }

    @Test
    public void shouldNotEnforceMutualExclusionOfReadLockForGivenName() throws InterruptedException {
        readWriteLock.acquireReadLock("foo");

        new Thread(() -> {
            readWriteLock.acquireReadLock("foo");
            numberOfLocks++;
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(1));
    }

    @Test
    public void shouldEnforceMutualExclusionOfReadAndWriteLockForGivenName() throws InterruptedException {
        readWriteLock.acquireReadLock("foo");

        new Thread(() -> {
            readWriteLock.acquireWriteLock("foo");
            numberOfLocks++;
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(0));
    }

    @Test
    public void shouldEnforceMutualExclusionOfWriteAndReadLockForGivenName() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(() -> {
            readWriteLock.acquireReadLock("foo");
            numberOfLocks++;
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(0));
    }


    @Test
    public void shouldNotEnforceMutualExclusionOfWriteLockForDifferentNames() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(() -> {
            readWriteLock.acquireWriteLock("bar");
            numberOfLocks++;
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(1));
    }
}
