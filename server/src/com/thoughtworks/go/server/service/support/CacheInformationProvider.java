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

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.server.cache.GoCache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;

@Component
public class CacheInformationProvider implements ServerInfoProvider {
    private GoCache goCache;

    @Autowired
    public CacheInformationProvider(GoCache goCache) {
        this.goCache = goCache;
    }

    @Override
    public double priority() {
        return 5.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder infoCollector) {
        infoCollector.addSection("Cache information");

        appendCacheConfigurationInformation(infoCollector, goCache);
        appendLiveCacheStatisticsInformation(infoCollector, goCache);
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

    private void appendCacheConfigurationInformation(InformationStringBuilder infoCollector, GoCache cache) {
        CacheConfiguration configuration = cache.configuration();

        infoCollector.addSubSection("Cache configuration information");
        infoCollector.append(reflectionToString(configuration, ToStringStyle.MULTI_LINE_STYLE)).append("\n");
    }

    private void appendLiveCacheStatisticsInformation(InformationStringBuilder infoCollector, GoCache cache) {
        LiveCacheStatistics statistics = cache.statistics();

        infoCollector.addSubSection("Cache runtime information");
        infoCollector.append(String.format("Statistics enabled? %s\n", statistics.isStatisticsEnabled()));
        infoCollector.append(String.format("Average get time in milliseconds: %s [Min: %s, Max: %s]\n", statistics.getAverageGetTimeMillis(),
                statistics.getMinGetTimeMillis(), statistics.getMaxGetTimeMillis()));
        infoCollector.append(String.format("Cache size: %s (Accuracy: %s)\n", statistics.getSize(), statistics.getStatisticsAccuracyDescription()));
        infoCollector.append(String.format("Cache counts: [Hits: %s, Miss: %s, Expired: %s, Eviction: %s, Put: %s, Remove: %s]\n\n",
                statistics.getCacheHitCount(), statistics.getCacheMissCount(), statistics.getExpiredCount(), statistics.getEvictedCount(),
                statistics.getPutCount(), statistics.getRemovedCount()));

        infoCollector.append(String.format("Cache size (in-memory): %s\n", statistics.getInMemorySize()));
        infoCollector.append(String.format("Cache hit count (in-memory): %s\n", statistics.getInMemoryHitCount()));
        infoCollector.append(String.format("Cache size (disk): %s\n", statistics.getOnDiskSize()));
        infoCollector.append(String.format("Cache hit count (disk): %s\n", statistics.getOnDiskHitCount()));
    }

    public Map<String, Object> getCacheRuntimeInformationAsJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        LiveCacheStatistics statistics = goCache.statistics();

        json.put("Statistics enabled", statistics.isStatisticsEnabled());

        LinkedHashMap<String, Object> time = new LinkedHashMap<>();
        time.put("Average", statistics.getAverageGetTimeMillis());
        time.put("Minimum", statistics.getMinGetTimeMillis());
        time.put("Maximum", statistics.getMinGetTimeMillis());
        json.put("Get Time in milliseconds", time);

        json.put("Cache Size", statistics.getSize());
        json.put("Accuracy", statistics.getStatisticsAccuracyDescription());

        LinkedHashMap<String, Object> cacheCount = new LinkedHashMap<>();
        cacheCount.put("Hits", statistics.getCacheHitCount());
        cacheCount.put("Miss", statistics.getCacheMissCount());
        cacheCount.put("Expired", statistics.getExpiredCount());
        cacheCount.put("Eviction", statistics.getEvictedCount());
        cacheCount.put("Put", statistics.getPutCount());
        cacheCount.put("Remove", statistics.getRemovedCount());
        json.put("Cache Counts", cacheCount);

        json.put("Cache Size (Disk)", statistics.getOnDiskSize());
        json.put("Cache Count (Disk)", statistics.getOnDiskHitCount());

        return json;
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
        json.put("Disk Store Path", config.getDiskStorePath());
        json.put("Disk Spool Buffer Size in MB", config.getDiskSpoolBufferSizeMB());
        json.put("Disk Access Stripes", config.getDiskAccessStripes());
        json.put("Disk Expiry Thread Interval Seconds", config.getDiskExpiryThreadIntervalSeconds());
        json.put("Logging Enabled", config.isLoggingEnabled());
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
