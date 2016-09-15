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
        goConfigService.register(new TemplateConfigChangedListner());
        goConfigService.register(new EnvironmentConfigListener());
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        goCache.remove(ETAG_CACHE_KEY);
    }

    public String md5ForEntity(PipelineTemplateConfig config) {
        String cacheKey = cacheKey(config, config.name());
        return getFromCache(config, cacheKey);
    }

    public String md5ForEntity(EnvironmentConfig config) {
        String cacheKey = cacheKey(config, config.name());
        return getFromCache(config, cacheKey);
    }

    public String md5ForEntity(SCM config) {
        String cacheKey = cacheKey(config, config.getName());
        return getFromCache(config, cacheKey);
    }

    public String md5ForEntity(PipelineConfig config) {
        String cacheKey = cacheKey(config, config.name());
        return getFromCache(config, cacheKey);
    }

    private String cacheKey(Object domainObject, CaseInsensitiveString name) {
        return cacheKey(domainObject, name.toLower());
    }

    private String cacheKey(Object domainObject, String name) {
        return getClass(domainObject) + "." + name;
    }

    private String getFromCache(Object domainObject, String cacheKey) {
        String cachedMD5 = getFromCache(cacheKey);

        if (cachedMD5 != null) {
            return cachedMD5;
        }

        String md5 = computeMd5For(domainObject);
        goCache.put(ETAG_CACHE_KEY, cacheKey, md5);

        return md5;
    }

    private void removeFromCache(Object domainObject, CaseInsensitiveString name) {
        removeFromCache(domainObject, name.toLower());
    }

    private void removeFromCache(Object domainObject, String name) {
        goCache.remove(ETAG_CACHE_KEY, cacheKey(domainObject, name));
    }

    private String getFromCache(String cacheKey) {
        return (String) goCache.get(ETAG_CACHE_KEY, cacheKey);
    }

    private String getClass(Object entity) {
        return entity.getClass().getName();
    }

    private String computeMd5For(Object domainObject) {
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(domainObject);
        return CachedDigestUtils.md5Hex(xml);
    }

    class PipelineConfigChangedListener extends EntityConfigChangedListener<PipelineConfig> {
        @Override
        public void onEntityConfigChange(PipelineConfig pipelineConfig) {
            removeFromCache(pipelineConfig, pipelineConfig.name());
        }
    }

    class SCMConfigChangedListner extends EntityConfigChangedListener<SCM> {
        @Override
        public void onEntityConfigChange(SCM scm) {
            removeFromCache(scm, scm.getName());
        }
    }

    class TemplateConfigChangedListner extends EntityConfigChangedListener<PipelineTemplateConfig> {
        @Override
        public void onEntityConfigChange(PipelineTemplateConfig pipelineTemplateConfig) {
            removeFromCache(pipelineTemplateConfig, pipelineTemplateConfig.name());
        }
    }

    class EnvironmentConfigListener extends EntityConfigChangedListener<BasicEnvironmentConfig> {
        @Override
        public void onEntityConfigChange(BasicEnvironmentConfig config) {
            removeFromCache(config, config.name());
        }
    }
}
