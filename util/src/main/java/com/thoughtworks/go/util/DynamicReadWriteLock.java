/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @understands handling of multiple dynamically-created readWriteLocks
 */
public class DynamicReadWriteLock {
    private Map<String, ReadWriteLock> locks = new HashMap<>();

    public void acquireReadLock(String key) {
        getLock(key).readLock().lock();
    }

    public void releaseReadLock(String key) {
        getLock(key).readLock().unlock();
    }

    public void acquireWriteLock(String key) {
        getLock(key).writeLock().lock();
    }

    public void releaseWriteLock(String key) {
        getLock(key).writeLock().unlock();
    }

    private ReadWriteLock getLock(String key) {
        synchronized (key.intern()) {
            if (locks.containsKey(key)) {
                return locks.get(key);
            }
            ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
            locks.put(key, lock);
            return lock;
        }
    }

    public void withWriteLock(String mutex, Procedure procedure) {
        acquireWriteLock(mutex);
        try {
            procedure.call();
        } finally {
            releaseWriteLock(mutex);
        }

    }

    public void withReadLock(String mutex, Procedure procedure) {
        acquireReadLock(mutex);
        try {
            procedure.call();
        } finally {
            releaseReadLock(mutex);
        }
    }
}
