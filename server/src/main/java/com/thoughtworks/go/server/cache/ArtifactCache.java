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
package com.thoughtworks.go.server.cache;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.thoughtworks.go.server.service.ArtifactsDirHolder;

/**
 * @understands serving prepared artifacts and preparing artifact offline
 */
public abstract class ArtifactCache<T> {
    protected final ArtifactsDirHolder artifactsDirHolder;
    protected ConcurrentSkipListSet<T> pendingCacheFiles = new ConcurrentSkipListSet<>();
    protected ConcurrentMap<T, Exception> pendingExceptions = new ConcurrentHashMap<>();
    public static final String CACHE_ARTIFACTS_FOLDER = "cache/artifacts/";

    public ArtifactCache(ArtifactsDirHolder artifactsDirHolder) {
        this.artifactsDirHolder = artifactsDirHolder;
    }

    public boolean cacheCreated(T artifactLocation) throws Exception {
        if (currentlyCreatingCache(artifactLocation)) { return false; }
        if (exceptionCreatingCache(artifactLocation)) {
            Exception e = pendingExceptions.get(artifactLocation);
            if (e != null && pendingExceptions.remove(artifactLocation, e)) {
                throw e;
            } else {
                return false;
            }
        }
        if (cacheAlreadyCreated(artifactLocation)) { return true; }

        startCacheCreationThread(artifactLocation);
        return false;
    }

    private boolean exceptionCreatingCache(T artifactLocation) {
        return pendingExceptions.containsKey(artifactLocation);
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
            Thread cacheCreatorThread = new Thread("cache-creator-thread-" + UUID.randomUUID().toString()) {
                @Override
                public void run() {
                    try {
                        createCachedFile(artifactLocation);
                    } catch (Exception e) {
                        pendingExceptions.putIfAbsent(artifactLocation, e);
                    } finally {
                        pendingCacheFiles.remove(artifactLocation);
                    }
                }
            };
            cacheCreatorThread.start();
        }
    }

    public abstract File cachedFile(T artifactLocation);

    abstract void createCachedFile(T artifactLocation) throws IOException;
}
