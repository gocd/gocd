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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheFactory {

    private final CacheConfiguration cacheConfiguration;
    private final TransactionSynchronizationManager transactionSynchronizationManager;

    public CacheFactory(TransactionSynchronizationManager transactionSynchronizationManager,
                        @Value("${cruise.cache.elements.limit}") int maxElementsInMemory,
                        @Value("${cruise.cache.is.eternal}") boolean eternal) {
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        cacheConfiguration = new CacheConfiguration("goCache", maxElementsInMemory)
                .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE))
                .eternal(eternal)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);
    }

    @Bean(name = "goCache")
    public GoCache domainObjectCache() {
        CacheManager cacheManager = CacheManager.newInstance(new net.sf.ehcache.config.Configuration().name(getClass().getName()));
        Cache cache = new Cache(cacheConfiguration);
        cacheManager.addCache(cache);
        return new GoCache(cache, transactionSynchronizationManager);
    }

    @Bean(name = "buildDurationCache")
    public com.github.benmanes.caffeine.cache.Cache<JobInstance.BuildDurationKey, Duration> buildDurationCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .build();
    }

}
