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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.util.CachedDigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntityHashingService implements ConfigChangedListener, Initializer {
    private GoConfigService goConfigService;
    private GoCache goCache;
    private static final String ETAG_CACHE_KEY = "GO_ETAG_CACHE".intern();
    private ConfigCache configCache;
    private ConfigElementImplementationRegistry registry;

    @Autowired
    public EntityHashingService(GoConfigService goConfigService, GoCache goCache, ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        this.goConfigService = goConfigService;
        this.goCache = goCache;
        this.configCache = configCache;
        this.registry = registry;
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(new PipelineConfigChangedListener());
        goConfigService.register(new SCMConfigChangedListner());
        goConfigService.register(new EnvironmentConfigListener());
    }

    public String md5ForPipelineConfig(String pipelineName) {
        String cachedMD5 = cachedMD5(pipelineName);

        return cachedMD5 != null ? cachedMD5 : md5For(pipelineName);
    }

    private String md5ForEntity(Object domainObject) {
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(domainObject);
        return CachedDigestUtils.md5Hex(xml);
    }

    public String md5ForEntity(Object domainObject, String subkey) {
        String cacheKey = domainObject.getClass().getName() + subkey;
        String cachedMD5 = cachedMD5(cacheKey);

        return cachedMD5 != null ? cachedMD5 : cachingFor(domainObject, cacheKey);
    }

    private String cachingFor(Object domainObject, String subkey) {
        String md5 = md5ForEntity(domainObject);
        addToCache(subkey, md5);
        return md5;
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        goCache.remove(ETAG_CACHE_KEY);
    }

    private String md5For(String pipelineName) {
        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        String cacheKey = pipelineConfig.getClass().getName() + pipelineName;
        return cachingFor(pipelineConfig, cacheKey);
    }

    private String cachedMD5(String subkey) {
        return (String) goCache.get(ETAG_CACHE_KEY, subkey.toLowerCase());
    }

    private void addToCache(String subkey, String md5) {
        goCache.put(ETAG_CACHE_KEY, subkey.toLowerCase(), md5);
    }

    class PipelineConfigChangedListener extends EntityConfigChangedListener<PipelineConfig> {
        @Override
        public void onEntityConfigChange(PipelineConfig pipelineConfig) {
            String cacheKey = pipelineConfig.getClass().getName() + pipelineConfig.name().toLower();
            goCache.remove(ETAG_CACHE_KEY, cacheKey.toLowerCase());
        }
    }

    class SCMConfigChangedListner extends EntityConfigChangedListener<SCM> {
        @Override
        public void onEntityConfigChange(SCM scm) {
            String cacheKey =  scm.getClass().getName() + scm.getName();
            goCache.remove(ETAG_CACHE_KEY, cacheKey.toLowerCase());
        }
    }

    class EnvironmentConfigListener extends EntityConfigChangedListener<BasicEnvironmentConfig> {
        @Override
        public void onEntityConfigChange(BasicEnvironmentConfig config) {
            String cacheKey = config.getClass().getName() + config.name().toLower();
            goCache.remove(ETAG_CACHE_KEY, cacheKey.toLowerCase());
        }
    }
}