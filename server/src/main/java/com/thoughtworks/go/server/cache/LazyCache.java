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
package com.thoughtworks.go.server.cache;

import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.function.Supplier;

public class LazyCache {
    private final Ehcache ehCache;
    private final TransactionSynchronizationManager transactionSynchronizationManager;

    public LazyCache(Ehcache ehCache, TransactionSynchronizationManager transactionSynchronizationManager) {
        this.ehCache = ehCache;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> compute) {
        Element element = ehCache.get(key);

        if (element != null) {
            return (T) element.getObjectValue();
        }

        synchronized (key.intern()) {
            element = ehCache.get(key);
            if (element != null) {
                return (T) element.getObjectValue();
            }

            T object = compute.get();
            ehCache.put(new Element(key, object));
            return object;
        }
    }

    public void flushOnCommit() {
        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                ehCache.flush();
            }
        });
    }

    @PreDestroy
    public void destroy() {
        ehCache.removeAll();
        Optional.ofNullable(ehCache.getCacheManager())
            .ifPresent(cm -> cm.removeCache(ehCache.getName()));
    }
}
