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
package com.thoughtworks.go.server.caching;

import com.thoughtworks.go.server.service.ArtifactsDirHolder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadFactory;

import static com.thoughtworks.go.util.ExceptionUtils.uncaughtExceptionHandlerFor;

/**
 * Understands serving prepared artifacts and preparing artifact offline
 */
public abstract class ArtifactCache<T extends Comparable<T>> {
    public static final String CACHE_ARTIFACTS_FOLDER = "cache/artifacts/";
    private static final ThreadFactory THREAD_FACTORY = Thread.ofVirtual()
        .name("cache-creator-thread-", 1)
        .uncaughtExceptionHandler(uncaughtExceptionHandlerFor(ArtifactCache.class))
        .factory();

    protected final ArtifactsDirHolder artifactsDirHolder;
    protected ConcurrentSkipListSet<T> pendingCacheFiles = new ConcurrentSkipListSet<>();
    protected ConcurrentMap<T, Exception> pendingExceptions = new ConcurrentHashMap<>();

    public ArtifactCache(ArtifactsDirHolder artifactsDirHolder) {
        this.artifactsDirHolder = artifactsDirHolder;
    }

    public boolean cacheCreated(T artifactLocation) throws IOException {
        if (currentlyCreatingCache(artifactLocation)) {
            return false;
        }

        throwOnExceptionFor(artifactLocation);

        if (cacheAlreadyCreated(artifactLocation)) {
            return true;
        }

        startCacheCreationThread(artifactLocation);
        return false;
    }

    private void throwOnExceptionFor(T artifactLocation) throws IOException {
        Exception e = pendingExceptions.remove(artifactLocation);
        if (e != null) {
            switch (e) {
                case IOException ioe -> throw ioe;
                case RuntimeException re -> throw re;
                default -> throw new RuntimeException(e); // unexpected
            }
        }
    }

    private boolean cacheAlreadyCreated(T artifactLocation) {
        return cachedFile(artifactLocation).exists();
    }

    private boolean currentlyCreatingCache(T artifactLocation) {
        return pendingCacheFiles.contains(artifactLocation);
    }

    protected void startCacheCreationThread(final T artifactLocation) {
        boolean inserted = pendingCacheFiles.add(artifactLocation);
        if (inserted) {
            THREAD_FACTORY.newThread(() -> {
                    try {
                        createCachedFile(artifactLocation);
                    } catch (Exception e) {
                        pendingExceptions.putIfAbsent(artifactLocation, e);
                    } finally {
                        pendingCacheFiles.remove(artifactLocation);
                    }
                }).start();
        }
    }

    public abstract File cachedFile(T artifactLocation);

    abstract void createCachedFile(T artifactLocation) throws IOException;
}
