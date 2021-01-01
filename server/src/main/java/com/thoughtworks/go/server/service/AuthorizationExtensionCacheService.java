/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class AuthorizationExtensionCacheService {
    private final int CACHE_EXPIRY_IN_MINUTES = SystemEnvironment.getGoServerAuthorizationExtensionCallsCacheTimeoutInSeconds() / 60;

    private final Cache<String, Boolean> isValidUserCache;
    private final Cache<String, List<String>> getUserRolesCache;
    private final AuthorizationExtension authorizationExtension;
    private final CacheKeyGenerator cacheKeyGenerator;

    public AuthorizationExtensionCacheService(AuthorizationExtension authorizationExtension, Ticker ticker) {
        this.authorizationExtension = authorizationExtension;
        isValidUserCache = CacheBuilder.newBuilder()
                .ticker(ticker).expireAfterWrite(CACHE_EXPIRY_IN_MINUTES, TimeUnit.MINUTES).build();
        getUserRolesCache = CacheBuilder.newBuilder()
                .ticker(ticker).expireAfterWrite(CACHE_EXPIRY_IN_MINUTES, TimeUnit.MINUTES).build();
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
    }

    @Autowired
    public AuthorizationExtensionCacheService(AuthorizationExtension authorizationExtension) {
        this(authorizationExtension, Ticker.systemTicker());
    }

    public boolean isValidUser(String pluginId, String username, SecurityAuthConfig authConfig) {
        String cacheKey = cacheKeyGenerator.generate("AuthorizationExtension_isValidUser", pluginId, username, authConfig.getId());
        Boolean fromCache = isValidUserCache.getIfPresent(cacheKey);

        if (fromCache == null) {
            fromCache = authorizationExtension.isValidUser(pluginId, username, authConfig);
            isValidUserCache.put(cacheKey, fromCache);
        }

        return fromCache;
    }

    public List<String> getUserRoles(String pluginId, String username, SecurityAuthConfig authConfig, List<PluginRoleConfig> pluginRoleConfigs) {
        String roleConfigNames = pluginRoleConfigs.stream().map(role -> role.getName().toString()).sorted().collect(Collectors.joining("&&"));
        String cacheKey = cacheKeyGenerator.generate("AuthorizationExtension_GetUserRoles", pluginId, username, authConfig.getId(), roleConfigNames);

        List<String> rolesFromCache = getUserRolesCache.getIfPresent(cacheKey);

        if (rolesFromCache == null) {
            rolesFromCache = authorizationExtension.getUserRoles(pluginId, username, authConfig, pluginRoleConfigs);
            getUserRolesCache.put(cacheKey, rolesFromCache);
        }

        return rolesFromCache;
    }

    public void invalidateCache() {
        isValidUserCache.invalidateAll();
        getUserRolesCache.invalidateAll();
    }

}
