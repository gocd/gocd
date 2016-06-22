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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.util.EntityDigest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntityHashingService implements ConfigChangedListener, Initializer {
    private GoConfigService goConfigService;
    private GoCache goCache;
    private EntityDigest entityDigest;
    private static final String PIPELINE_CONFIG_CACHE_KEY = "GO_PIPELINE_CONFIGS_ETAGS_CACHE".intern();

    @Autowired
    public EntityHashingService(GoConfigService goConfigService, GoCache goCache) {
        this.goConfigService = goConfigService;
        this.goCache = goCache;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(new PipelineConfigChangedListener());
    }

    public EntityHashingService initializeWith(EntityDigest entityDigest) {
        this.entityDigest = entityDigest;
        return this;
    }

    public String md5ForPipelineConfig(String pipelineName) {
        String cachedMD5 = cachedMD5(pipelineName);

        return cachedMD5 != null ? cachedMD5 : md5For(pipelineName);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        goCache.remove(PIPELINE_CONFIG_CACHE_KEY);
    }

    private String md5For(String pipelineName) {
        String md5 = entityDigest.md5ForPipeline(goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName)));

        addToCache(pipelineName, md5);

        return md5;
    }

    private String cachedMD5(String pipelineName) {
        return (String) goCache.get(PIPELINE_CONFIG_CACHE_KEY, pipelineName.toLowerCase());
    }

    private void addToCache(String pipelineName, String md5) {
        goCache.put(PIPELINE_CONFIG_CACHE_KEY, pipelineName.toLowerCase(), md5);
    }

    class PipelineConfigChangedListener extends EntityConfigChangedListener<PipelineConfig> {
        @Override
        public void onEntityConfigChange(PipelineConfig pipelineConfig) {
            goCache.remove(PIPELINE_CONFIG_CACHE_KEY, pipelineConfig.name().toLower());
        }
    }
}