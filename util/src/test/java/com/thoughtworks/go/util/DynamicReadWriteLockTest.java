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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicReadWriteLockTest {
    private DynamicReadWriteLock readWriteLock;
    private final AtomicInteger numberOfLocks = new AtomicInteger(0);

    @BeforeEach
    public void setUp() {
        readWriteLock = new DynamicReadWriteLock();
    }

    @Test
    public void shouldEnforceMutualExclusionOfWriteLockForGivenName() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(() -> {
            readWriteLock.acquireWriteLock("foo");
            numberOfLocks.incrementAndGet();
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks.get()).isZero();
    }

    @Test
    public void shouldNotEnforceMutualExclusionOfReadLockForGivenName() throws InterruptedException {
        readWriteLock.acquireReadLock("foo");

        new Thread(() -> {
            readWriteLock.acquireReadLock("foo");
            numberOfLocks.incrementAndGet();
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks.get()).isEqualTo(1);
    }

    @Test
    public void shouldEnforceMutualExclusionOfReadAndWriteLockForGivenName() throws InterruptedException {
        readWriteLock.acquireReadLock("foo");

        new Thread(() -> {
            readWriteLock.acquireWriteLock("foo");
            numberOfLocks.incrementAndGet();
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks.get()).isZero();
    }

    @Test
    public void shouldEnforceMutualExclusionOfWriteAndReadLockForGivenName() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(() -> {
            readWriteLock.acquireReadLock("foo");
            numberOfLocks.incrementAndGet();
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks.get()).isZero();
    }


    @Test
    public void shouldNotEnforceMutualExclusionOfWriteLockForDifferentNames() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(() -> {
            readWriteLock.acquireWriteLock("bar");
            numberOfLocks.incrementAndGet();
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks.get()).isEqualTo(1);
    }
}
