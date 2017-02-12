/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.server.cache.GoCache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CacheInformationProvider implements ServerInfoProvider {
    private GoCache goCache;

    @Autowired
    public CacheInformationProvider(GoCache goCache) {
        this.goCache = goCache;
    }

    @Override
    public double priority() {
        return 7.0;
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Cache configuration information", getCacheConfigurationInformationAsJson());
        json.put("Cache runtime information", getCacheRuntimeInformationAsJson());
        return json;
    }

    @Override
    public String name() {
        return "Cache Information";
    }

    public Map<String, Object> getCacheRuntimeInformationAsJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        StatisticsGateway statistics = goCache.statistics();
//        json.put("Statistics enabled", statistics.isStatisticsEnabled());

        json.put("Get Time in milliseconds", getStatisticsFrom(statistics.cacheGetOperation()));
        json.put("Put Time in milliseconds", getStatisticsFrom(statistics.cachePutOperation()));
        json.put("Remove Time in milliseconds", getStatisticsFrom(statistics.cacheRemoveOperation()));

        json.put("Cache Size", statistics.getSize());
//        json.put("Accuracy", statistics.getStatisticsAccuracyDescription());

        LinkedHashMap<String, Object> cacheCount = new LinkedHashMap<>();
        cacheCount.put("Hits", statistics.cacheHitCount());
        cacheCount.put("Miss", statistics.cacheMissCount());
        cacheCount.put("Expired", statistics.cacheExpiredCount());
        cacheCount.put("Eviction", statistics.cacheEvictedCount());
        cacheCount.put("Put", statistics.cachePutCount());
        cacheCount.put("Remove", statistics.cacheRemoveCount());
        json.put("Cache Counts", cacheCount);

        json.put("Cache Size (Disk)", statistics.getLocalDiskSize());
        json.put("Cache Count (Disk)", statistics.localDiskHitCount());

        return json;
    }

    private LinkedHashMap<String, Object> getStatisticsFrom(ExtendedStatistics.Result result) {
        LinkedHashMap<String, Object> time = new LinkedHashMap<>();
        time.put("Average", result.latency().average());
        time.put("Minimum", result.latency().minimum());
        time.put("Maximum", result.latency().maximum());
        return time;
    }

    public Map<String, Object> getCacheConfigurationInformationAsJson() {
        CacheConfiguration config = goCache.configuration();
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();

        json.put("name", config.getName());
        json.put("Maximum Elements in Memory", config.getMaxElementsInMemory());
        json.put("Maximum Elements on Disk", config.getMaxElementsOnDisk());
        json.put("Memory Store Eviction Policy", config.getMemoryStoreEvictionPolicy());
        json.put("Clean or Flush", config.isClearOnFlush());
        json.put("Eternal", config.isEternal());
        json.put("Time To Idle Seconds", config.getTimeToIdleSeconds());
        json.put("time To Live Seconds", config.getTimeToLiveSeconds());
        json.put("Overflow To Disk", config.isOverflowToDisk());
        json.put("Disk Persistent", config.isDiskPersistent());
//        json.put("Disk Store Path", config.getDiskStorePath());
        json.put("Disk Spool Buffer Size in MB", config.getDiskSpoolBufferSizeMB());
        json.put("Disk Access Stripes", config.getDiskAccessStripes());
        json.put("Disk Expiry Thread Interval Seconds", config.getDiskExpiryThreadIntervalSeconds());
        json.put("Logging Enabled", config.getLogging());
        json.put("Cache Event Listener Configurations", config.getCacheEventListenerConfigurations());
        json.put("Cache Extension Configurations", config.getCacheExtensionConfigurations());
        json.put("Cache Extension Configurations", config.getCacheExtensionConfigurations());
        json.put("Bootstrap Cache Loader Factory Configuration", config.getBootstrapCacheLoaderFactoryConfiguration());
        json.put("Cache Exception Handler Factory Configuration", config.getCacheExceptionHandlerFactoryConfiguration());
        json.put("Terracotta Configuration", config.getTerracottaConfiguration());
        json.put("Cache Writer Configuration", config.getCacheWriterConfiguration());
        json.put("Cache Loader Configurations", config.getCacheLoaderConfigurations());
        json.put("Frozen", config.isFrozen());
        json.put("Transactional Mode", config.getTransactionalMode());
        json.put("Statistics", config.getStatistics());

        return json;
    }
}
