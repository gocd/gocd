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

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

public class DynamicReadWriteLockTest {
    private final DynamicReadWriteLock readWriteLock = spy(new DynamicReadWriteLock());

    @Test
    public void shouldEnforceMutualExclusionOfWriteLockForGivenName() {
        readWriteLock.acquireWriteLock("foo");

        Thread thread = new Thread(() -> readWriteLock.acquireWriteLock("foo"));
        thread.start();

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(readWriteLock, times(2)).acquireWriteLock("foo");
                assertThat(thread.isAlive()).isTrue();
            });
        thread.interrupt();
    }

    @Test
    public void shouldNotEnforceMutualExclusionOfReadLockForGivenName() {
        readWriteLock.acquireReadLock("foo");

        Thread thread = new Thread(() -> readWriteLock.acquireReadLock("foo"));
        thread.start();

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(readWriteLock, times(2)).acquireReadLock("foo");
                assertThat(thread.isAlive()).isFalse();
            });
    }

    @Test
    public void shouldEnforceMutualExclusionOfReadAndWriteLockForGivenName() {
        readWriteLock.acquireReadLock("foo");

        Thread thread = new Thread(() -> readWriteLock.acquireWriteLock("foo"));
        thread.start();

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(readWriteLock).acquireWriteLock("foo");
                assertThat(thread.isAlive()).isTrue();
            });
        thread.interrupt();
    }

    @Test
    public void shouldEnforceMutualExclusionOfWriteAndReadLockForGivenName() {
        readWriteLock.acquireWriteLock("foo");

        Thread thread = new Thread(() -> readWriteLock.acquireReadLock("foo"));
        thread.start();

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(readWriteLock).acquireReadLock("foo");
                assertThat(thread.isAlive()).isTrue();
            });
        thread.interrupt();
    }

    @Test
    public void shouldNotEnforceMutualExclusionOfWriteLockForDifferentNames() {
        readWriteLock.acquireWriteLock("foo");

        Thread thread = new Thread(() -> readWriteLock.acquireWriteLock("bar"));
        thread.start();

        await()
            .pollDelay(10, TimeUnit.MILLISECONDS)
            .timeout(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(readWriteLock).acquireWriteLock("bar");
                assertThat(thread.isAlive()).isFalse();
            });
    }
}
