/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.cache;

import java.io.IOException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class UserCacheFactory {
    private EhCacheFactoryBean factoryBean;

    public UserCacheFactory() {
        factoryBean = new EhCacheFactoryBean();
    }

    public Cache createCache() throws IOException {
        factoryBean.setCacheManager(createCacheManager());
        factoryBean.setCacheName("userCache");
        factoryBean.setDiskPersistent(false);
        factoryBean.setOverflowToDisk(false);
        factoryBean.setMaxElementsInMemory(1000);
        factoryBean.setEternal(true);
        factoryBean.setMemoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);
        factoryBean.afterPropertiesSet();
        return (Cache) factoryBean.getObject();
    }

    private CacheManager createCacheManager() {
        Configuration configuration = new Configuration();
        configuration.setDefaultCacheConfiguration(new CacheConfiguration("cache", 10000));
        return new CacheManager(configuration);
    }
}
