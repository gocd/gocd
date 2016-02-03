/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.ConfigCacheStore;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class ConfigEhCacheStore implements ConfigCacheStore {

    private final Ehcache ehcache;

    @Autowired
    public ConfigEhCacheStore(ConfigCache configCache) {
        int maxElementsInMemory = 100;
        EhCacheFactoryBean factoryBean = new EhCacheFactoryBean();

        Configuration configuration = new Configuration();
        configuration.setDefaultCacheConfiguration(new CacheConfiguration("cache", maxElementsInMemory));

        factoryBean.setCacheManager(new CacheManager(configuration));
        factoryBean.setCacheName("configCacheStore");
        factoryBean.setDiskPersistent(false);
        factoryBean.setOverflowToDisk(false);
        factoryBean.setMaxElementsInMemory(maxElementsInMemory);
        factoryBean.setEternal(true);
        factoryBean.setMemoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);
        try {
            factoryBean.afterPropertiesSet();
        } catch (IOException e) {
            bomb(e);
        }
        ehcache = factoryBean.getObject();
        configCache.setConfigCacheStore(this);
    }

    @Override
    public Object get(String key) {
        Element element = ehcache.get(key);
        if (element == null) {
            return null;
        }
        return element.getValue();
    }

    @Override
    public void put(String key, Object obj) {
        ehcache.put(new Element(key, obj));
    }
}
