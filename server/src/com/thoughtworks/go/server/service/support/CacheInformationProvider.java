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

package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.server.cache.GoCache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.statistics.StatisticsGateway;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private void appendCacheConfigurationInformation(InformationStringBuilder infoCollector, GoCache cache) {
        CacheConfiguration configuration = cache.configuration();

        infoCollector.addSubSection("Cache configuration information");
        infoCollector.append(reflectionToString(configuration, ToStringStyle.MULTI_LINE_STYLE)).append("\n");
    }

    private void appendLiveCacheStatisticsInformation(InformationStringBuilder infoCollector, GoCache cache) {
        StatisticsGateway statistics = cache.statistics();

        infoCollector.addSubSection("Cache runtime information");
        infoCollector.append(String.format("Average get operation latency (in ms): %s\n", statistics.cacheGetOperation().latency()));
        infoCollector.append(String.format("Cache size: %s\n", statistics.getSize()));
        infoCollector.append(String.format("Cache counts: [Hits: %s, Miss: %s, Expired: %s, Eviction: %s, Put: %s, Remove: %s]\n\n",
                statistics.cacheHitCount(), statistics.cacheMissCount(), statistics.cacheExpiredCount(), statistics.cacheEvictedCount(),
                statistics.cachePutCount(), statistics.cacheRemoveCount()));

        infoCollector.append(String.format("Cache size (in-memory): %s\n", statistics.getLocalHeapSize()));
        infoCollector.append(String.format("Cache hit count (in-memory): %s\n", statistics.localHeapHitCount()));
        infoCollector.append(String.format("Cache size (disk): %s\n", statistics.getLocalDiskSize()));
        infoCollector.append(String.format("Cache hit count (disk): %s\n", statistics.localDiskHitCount()));
    }
}
