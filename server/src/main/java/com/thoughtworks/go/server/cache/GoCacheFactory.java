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

import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class GoCacheFactory {

    private final CacheConfiguration cacheConfiguration;
    private TransactionSynchronizationManager transactionSynchronizationManager;

    static {
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    }

    public GoCacheFactory(TransactionSynchronizationManager transactionSynchronizationManager,
                          @Value("${cruise.cache.elements.limit}") int maxElementsInMemory,
                          @Value("${cruise.cache.is.eternal}") boolean eternal) {
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        cacheConfiguration = new CacheConfiguration("goCache", maxElementsInMemory)
                .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE))
                .eternal(eternal)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);
    }

    @Bean(name = "goCache")
    public GoCache createCache() {
        CacheManager cacheManager = CacheManager.newInstance(new Configuration().name(getClass().getName()));
        Cache cache = new Cache(cacheConfiguration);
        cacheManager.addCache(cache);
        return new GoCache(cache, transactionSynchronizationManager);
    }

}
