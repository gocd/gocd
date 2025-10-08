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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Understands handling of multiple dynamically-created readWriteLocks
 */
public class DynamicReadWriteLock {
    private final ConcurrentMap<String, ReadWriteLock> locks = new ConcurrentHashMap<>();

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
        return locks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }
}
