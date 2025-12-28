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
package com.thoughtworks.go.server.service.support;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.statistics.StatisticsGateway;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CacheInformationProvider implements ServerInfoProvider {

    @Override
    public double priority() {
        return 11.5;
    }

    @Override
    public Map<String, Object> asJsonCompatibleMap() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();

        for (CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS) {
            LinkedHashMap<String, Object> jsonForManager = new LinkedHashMap<>();
            json.put(cacheManager.getName(), jsonForManager);

            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                LinkedHashMap<String, Object> cacheJson = new LinkedHashMap<>();
                jsonForManager.put(cacheName, cacheJson);

                cacheJson.put("Cache configuration information", getCacheConfigurationInformationAsJson(cache));
                cacheJson.put("Cache runtime information", getCacheRuntimeInformationAsJson(cache));
            }
        }

        return json;
    }

    @Override
    public String name() {
        return "Cache Information";
    }

    public Map<String, Object> getCacheRuntimeInformationAsJson(Cache cache) {
        Map<String, Object> json = new LinkedHashMap<>();
        StatisticsGateway statistics = cache.getStatistics();

        json.put("Cache Size", statistics.getSize());

        Map<String, Object> cacheCount = new LinkedHashMap<>();
        cacheCount.put("Hits", statistics.cacheHitCount());
        cacheCount.put("Miss", Map.of(
            "Total", statistics.cacheMissCount(),
            "Expired", statistics.cacheMissExpiredCount(),
            "NotFound", statistics.cacheMissNotFoundCount())
        );
        cacheCount.put("MissExpired", statistics.cacheMissExpiredCount());
        cacheCount.put("MissNotFound", statistics.cacheMissNotFoundCount());
        cacheCount.put("Expired", statistics.cacheExpiredCount());
        cacheCount.put("Evicted", statistics.cacheEvictedCount());
        cacheCount.put("Put", Map.of(
            "Total", statistics.cachePutCount(),
            "Added", statistics.cachePutAddedCount(),
            "Updated", statistics.cachePutUpdatedCount())
        );
        cacheCount.put("Remove", statistics.cacheRemoveCount());
        json.put("Cache Counts", cacheCount);

        json.put("Cache Size (Disk)", statistics.getLocalDiskSize());
        json.put("Cache Count (Disk)", statistics.localDiskHitCount());

        return json;
    }

    public Map<String, Object> getCacheConfigurationInformationAsJson(Cache cache) {
        CacheConfiguration config = cache.getCacheConfiguration();
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();

        json.put("Name", config.getName());
        json.put("Maximum Elements in Memory", config.getMaxEntriesLocalHeap());
        json.put("Maximum Elements on Disk", config.getMaxBytesLocalDisk());
        json.put("Memory Store Eviction Policy", config.getMemoryStoreEvictionPolicy().toString());
        json.put("Clean or Flush", config.isClearOnFlush());
        json.put("Eternal", config.isEternal());
        json.put("Time To Idle Seconds", config.getTimeToIdleSeconds());
        json.put("time To Live Seconds", config.getTimeToLiveSeconds());
        if (config.getPersistenceConfiguration() != null) {
            json.put("Persistence Configuration Strategy", config.getPersistenceConfiguration().getStrategy());
            json.put("Persistence Configuration Synchronous writes", config.getPersistenceConfiguration().getSynchronousWrites());
        } else {
            json.put("Persistence Configuration Strategy", "NONE");
            json.put("Persistence Configuration Synchronous writes", false);
        }
        json.put("Disk Spool Buffer Size in MB", config.getDiskSpoolBufferSizeMB());
        json.put("Disk Access Stripes", config.getDiskAccessStripes());
        json.put("Disk Expiry Thread Interval Seconds", config.getDiskExpiryThreadIntervalSeconds());
        json.put("Logging Enabled", config.getLogging());
        json.put("Cache Writer Configuration", config.getCacheWriterConfiguration());
        json.put("Cache Loader Configurations", config.getCacheLoaderConfigurations());
        json.put("Transactional Mode", config.getTransactionalMode());
        json.put("Statistics Enabled", config.getStatistics());

        return json;
    }
}
