/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class DynamicReadWriteLockTest {
    private DynamicReadWriteLock readWriteLock;
    private volatile int numberOfLocks;

    @Before public void setUp() {
        readWriteLock = new DynamicReadWriteLock();
        numberOfLocks = 0;
    }

    @After public void tearDown() {

    }

    @Test public void shouldEnforceMutualExclutionOfWriteLockForGivenName() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(new Runnable() {
            @Override
            public void run() {
                readWriteLock.acquireWriteLock("foo");
                numberOfLocks++;
            }
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(0));
    }

    @Test public void shouldNotEnforceMutualExclutionOfReadLockForGivenName() throws InterruptedException {
        readWriteLock.acquireReadLock("foo");

        new Thread(new Runnable() {
            @Override
            public void run() {
                readWriteLock.acquireReadLock("foo");
                numberOfLocks++;
            }
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(1));
    }

    @Test public void shouldEnforceMutualExclutionOfReadAndWriteLockForGivenName() throws InterruptedException {
        readWriteLock.acquireReadLock("foo");

        new Thread(new Runnable() {
            @Override
            public void run() {
                readWriteLock.acquireWriteLock("foo");
                numberOfLocks++;
            }
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(0));
    }

    @Test public void shouldEnforceMutualExclutionOfWriteAndReadLockForGivenName() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(new Runnable() {
            @Override
            public void run() {
                readWriteLock.acquireReadLock("foo");
                numberOfLocks++;
            }
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(0));
    }


    @Test public void shouldNotEnforceMutualExclutionOfWriteLockForDifferentNames() throws InterruptedException {
        readWriteLock.acquireWriteLock("foo");

        new Thread(new Runnable() {
            @Override
            public void run() {
                readWriteLock.acquireWriteLock("bar");
                numberOfLocks++;
            }
        }).start();

        Thread.sleep(1000);

        assertThat(numberOfLocks, is(1));
    }
}
