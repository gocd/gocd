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

package com.thoughtworks.go.server.cache;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.cache.ehcache.EhCacheFactoryBean;

public class GoCacheFactory {
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private String diskStorePath;
    private EhCacheFactoryBean factoryBean;
    private Boolean clearOnFlush = true;

    static {
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    }

    public GoCacheFactory(TransactionSynchronizationManager transactionSynchronizationManager) {
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        factoryBean = new EhCacheFactoryBean();
    }

    public GoCache createCache() throws IOException {
        factoryBean.setCacheManager(createCacheManager());
        factoryBean.afterPropertiesSet();
        factoryBean.getObject().getCacheConfiguration().setClearOnFlush(clearOnFlush);
        return new GoCache(factoryBean, transactionSynchronizationManager);
    }

    public void setCacheManager(CacheManager cacheManager) {
        factoryBean.setCacheManager(cacheManager);
    }

    public void setCacheName(String cacheName) {
        factoryBean.setCacheName(cacheName);
    }

    public void setMaxElementsInMemory(int maxElementsInMemory) {
        factoryBean.setMaxElementsInMemory(maxElementsInMemory);
    }

    public void setMaxElementsOnDisk(int maxElementsOnDisk) {
        factoryBean.setMaxElementsOnDisk(maxElementsOnDisk);
    }

    public void setMemoryStoreEvictionPolicy(MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {
        factoryBean.setMemoryStoreEvictionPolicy(memoryStoreEvictionPolicy);
    }

    public void setOverflowToDisk(boolean overflowToDisk) {
        factoryBean.setOverflowToDisk(overflowToDisk);
    }

    public void setEternal(boolean eternal) {
        factoryBean.setEternal(eternal);
    }

    public void setTimeToLive(int timeToLive) {
        factoryBean.setTimeToLive(timeToLive);
    }

    public void setTimeToIdle(int timeToIdle) {
        factoryBean.setTimeToIdle(timeToIdle);
    }

    public void setDiskPersistent(boolean diskPersistent) {
        factoryBean.setDiskPersistent(diskPersistent);
    }

    public void setDiskExpiryThreadIntervalSeconds(int diskExpiryThreadIntervalSeconds) {
        factoryBean.setDiskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
    }

    public void setBlocking(boolean blocking) {
        factoryBean.setBlocking(blocking);
    }

    public void setDiskStorePath(String diskStorePath) {
        this.diskStorePath = diskStorePath;
    }

    private CacheManager createCacheManager() throws UnsupportedEncodingException {
        Configuration configuration = new Configuration();
        configuration.setUpdateCheck(false);
        configuration.addDiskStore(diskStore());
        configuration.setDefaultCacheConfiguration(new CacheConfiguration("cache", 10000));
        return new CacheManager(configuration);
    }

    private DiskStoreConfiguration diskStore() {
        DiskStoreConfiguration diskStore = new DiskStoreConfiguration();
        diskStore.setPath(diskStorePath);
        return diskStore;
    }

    public void setClearOnFlush(Boolean clearOnFlush) {
        this.clearOnFlush = clearOnFlush;
    }
}
