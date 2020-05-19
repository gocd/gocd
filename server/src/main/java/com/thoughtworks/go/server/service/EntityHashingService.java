/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.service.lookups.CommandSnippet;
import com.thoughtworks.go.server.service.lookups.CommandSnippets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static java.lang.String.valueOf;
import static java.util.Objects.hash;

@Component
public class EntityHashingService implements ConfigChangedListener, Initializer {
    private static final String SEP_CHAR = "/";
    private static final String ETAG_CACHE_KEY = "GO_ETAG_CACHE";

    private final GoConfigService goConfigService;
    private final GoCache goCache;
    private final ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;

    private static String digestHex(String data) {
        return sha512_256Hex(data);
    }

    @Autowired
    public EntityHashingService(GoConfigService goConfigService, GoCache goCache, ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        this.goConfigService = goConfigService;
        this.goCache = goCache;
        this.configCache = configCache;
        this.registry = registry;
    }

    @Override
    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(new PipelineConfigChangedListener());
        goConfigService.register(new BasicPipelineConfigsChangedListener());
        goConfigService.register(new MergePipelineConfigsChangedListener());
        goConfigService.register(new SCMConfigChangedListener());
        goConfigService.register(new TemplateConfigChangedListner());
        goConfigService.register(new EnvironmentConfigListener());
        goConfigService.register(new PackageRepositoryChangeListener());
        goConfigService.register(new ElasticAgentProfileConfigListener());
        goConfigService.register(new SecretConfigListener());
        goConfigService.register(new PackageListener());
        goConfigService.register(new SecurityAuthConfigListener());
        goConfigService.register(new ConfigRepoListener());
        goConfigService.register(new RoleConfigListener());
        goConfigService.register(new ArtifactStoreListener());
        goConfigService.register(new AdminsConfigListener());
        goConfigService.register(new ClusterProfileListener());
        goConfigService.register(new SCMChangeListener());
        goConfigService.register(new ArtifactConfigChangeListener());
    }

    @Override
    public void startDaemon() {
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        goCache.remove(ETAG_CACHE_KEY);
    }

    public String hashForEntity(PipelineTemplateConfig config) {
        String cacheKey = cacheKey(config, config.name());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(NotificationFilter filter) {
        String cacheKey = cacheKey(filter, String.valueOf(filter.getId()));
        return getFromCache(cacheKey, () -> String.valueOf(filter.hashCode()));
    }

    public String hashForEntity(EnvironmentConfig config) {
        if (config instanceof MergeEnvironmentConfig) {
            return digestHex(valueOf(hash(config)));
        }

        String cacheKey = cacheKey(config, config.name());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(EnvironmentsConfig envConfigs) {
        final String environmentConfigSegment = envConfigs
                .stream()
                .map(this::hashForEntity)
                .collect(Collectors.joining(SEP_CHAR));

        return digestHex(environmentConfigSegment);
    }

    public String hashForEntity(PackageRepository config) {
        String cacheKey = cacheKey(config, config.getId());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(PackageRepositories packageRepositories) {
        List<String> parts = new ArrayList<>();
        for (PackageRepository packageRepository : packageRepositories) {
            parts.add(hashForEntity(packageRepository));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(SCM config) {
        String cacheKey = cacheKey(config, config.getName());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(PipelineConfig pipelineConfig, String groupName) {
        String cacheKey = cacheKey(pipelineConfig, pipelineConfig.name());
        String parts = getDomainEntityDigestFromCache(pipelineConfig, cacheKey);
        return digestHex(StringUtils.join(parts, SEP_CHAR, groupName));
    }

    public String hashForEntity(ConfigRepoConfig config) {
        String cacheKey = cacheKey(config, config.getId());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(ElasticProfile config) {
        String cacheKey = cacheKey(config, config.getId());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(SecretConfig config) {
        String cacheKey = cacheKey(config, config.getId());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(ClusterProfile profile) {
        String cacheKey = cacheKey(profile, profile.getId());
        return getDomainEntityDigestFromCache(profile, cacheKey);
    }

    public String hashForEntity(CommandSnippet commandSnippet) {
        String cacheKey = cacheKey(commandSnippet, commandSnippet.getName());
        return getDbEntityDigestFromCache(cacheKey, commandSnippet);
    }

    public String hashForEntity(CommandSnippets commandSnippets) {
        List<String> parts = new ArrayList<>();
        for (CommandSnippet commandSnippet : commandSnippets.getSnippets()) {
            parts.add(hashForEntity(commandSnippet));
        }

        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(SecurityAuthConfig config) {
        String cacheKey = cacheKey(config, config.getId());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(SecurityAuthConfigs authConfigs) {
        List<String> parts = new ArrayList<>();
        for (SecurityAuthConfig authConfig : authConfigs) {
            parts.add(hashForEntity(authConfig));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(Role config) {
        String cacheKey = cacheKey(config, config.getName());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(AdminsConfig config) {
        String cacheKey = cacheKey(config, "cacheKey");
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(PackageDefinition config) {
        String cacheKey = cacheKey(config, config.getId());
        return getDomainEntityDigestFromCache(config, cacheKey);
    }

    public String hashForEntity(Packages config) {
        List<String> parts = new ArrayList<>();
        for (PackageDefinition packageDefinition : config) {
            parts.add(hashForEntity(packageDefinition));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(PluginSettings pluginSettings) {
        String cacheKey = cacheKey(pluginSettings, pluginSettings.getPluginId());
        return getFromCache(cacheKey, () -> String.valueOf(pluginSettings.hashCode()));
    }

    public String hashForEntity(ArtifactStore artifactStore) {
        String cacheKey = cacheKey(artifactStore, artifactStore.getId());
        return getDbEntityDigestFromCache(cacheKey, artifactStore);
    }

    private String cacheKey(Object domainObject, CaseInsensitiveString name) {
        return cacheKey(domainObject, name.toLower());
    }

    private String cacheKey(Object domainObject, String name) {
        return getClass(domainObject) + "." + name;
    }

    private String getDbEntityDigestFromCache(String cacheKey, Object dbObject) {
        return getFromCache(cacheKey, () -> new GsonBuilder().create().toJson(dbObject));
    }

    private String getDomainEntityDigestFromCache(Object domainObject, String cacheKey) {
        return getFromCache(cacheKey, () -> getDomainObjectXmlPartial(domainObject));
    }

    private String getFromCache(String cacheKey, Supplier<String> fingerprintSupplier) {
        String cached = getFromCache(cacheKey);

        if (cached != null) {
            return cached;
        }

        String digest = digestHex(fingerprintSupplier.get());
        goCache.put(ETAG_CACHE_KEY, cacheKey, digest);

        return digest;
    }

    public void removeFromCache(Object domainObject, CaseInsensitiveString name) {
        removeFromCache(domainObject, name.toLower());
    }

    public void removeFromCache(Object domainObject, String name) {
        goCache.remove(ETAG_CACHE_KEY, cacheKey(domainObject, name));
    }

    private String getFromCache(String cacheKey) {
        return (String) goCache.get(ETAG_CACHE_KEY, cacheKey);
    }

    private String getClass(Object entity) {
        return entity.getClass().getName();
    }

    private String getDomainObjectXmlPartial(Object domainObject) {
        return new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(domainObject);
    }

    public String hashForEntity(RolesConfig roles) {
        List<String> parts = new ArrayList<>();
        for (Role role : roles) {
            parts.add(hashForEntity(role));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(ParamsConfig paramConfigs) {
        List<String> parts = new ArrayList<>();
        for (ParamConfig paramConfig : paramConfigs) {
            parts.add(hashForEntity(paramConfig));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    private String hashForEntity(ParamConfig paramConfig) {
        String cacheKey = cacheKey(paramConfig, paramConfig.getName());
        return getDomainEntityDigestFromCache(paramConfig, cacheKey);
    }

    public String hashForEntity(UsageStatisticsReporting usageStatisticsReporting) {
        String cacheKey = cacheKey(usageStatisticsReporting, usageStatisticsReporting.getServerId());
        return getDbEntityDigestFromCache(cacheKey, usageStatisticsReporting);
    }

    public String hashForEntity(DataSharingSettings dataSharingSettings) {
        String cacheKey = cacheKey(dataSharingSettings, "data_sharing_settings");
        return getDbEntityDigestFromCache(cacheKey, dataSharingSettings);
    }

    public String hashForEntity(PipelineGroups pipelineGroups) {
        List<String> parts = new ArrayList<>();
        for (PipelineConfigs pipelineConfigs : pipelineGroups) {
            parts.add(hashForEntity(pipelineConfigs));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(PipelineConfigs pipelineConfigs) {
        String cacheKey = cacheKey(pipelineConfigs, pipelineConfigs.getGroup());
        return getDomainEntityDigestFromCache(pipelineConfigs, cacheKey);
    }

    public String hashForEntity(CombinedPluginInfo pluginInfo) {
        String cacheKey = cacheKey(pluginInfo, String.format("plugin_info_%s", pluginInfo.getDescriptor().id()));
        return getFromCache(cacheKey, () -> String.valueOf(Objects.hash(pluginInfo)));
    }

    public String hashForEntity(Collection<CombinedPluginInfo> pluginInfos) {
        List<String> parts = new ArrayList<>();
        for (CombinedPluginInfo pluginInfo : pluginInfos) {
            parts.add(hashForEntity(pluginInfo));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(SecretConfigs secretConfigs) {
        List<String> parts = new ArrayList<>();
        for (SecretConfig secretConfig : secretConfigs) {
            parts.add(hashForEntity(secretConfig));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(SCMs scms) {
        List<String> parts = new ArrayList<>();
        for (SCM scm : scms) {
            parts.add(hashForEntity(scm));
        }
        return digestHex(StringUtils.join(parts, SEP_CHAR));
    }

    public String hashForEntity(ArtifactConfig artifactConfig) {
        String cacheKey = cacheKey(artifactConfig, "cacheKey");
        return getFromCache(cacheKey, () -> String.valueOf(Objects.hash(artifactConfig)));
    }

    /**
     * Computes an int hashCode based on a cryptographic digest of the contents. Unlike most of the methods of this
     * service, the result is intentionally <em>not</em> cached; caching is beyond the scope of this method and should
     * be implemented at a higher abstraction as needed.
     *
     * @param partial a {@link PartialConfig} to hash
     * @return an integer hash code that can be used for comparison.
     */
    public int computeHashForEntity(PartialConfig partial) {
        if (null == partial) return 0;

        return Objects.hash(
                digestMany(partial.getGroups()),
                digestMany(partial.getEnvironments()),
                digestMany(partial.getScms())
        );
    }

    /**
     * Computes a cryptographic digest of a collection's contents
     *
     * @param entities any {@link Collection} of config entities
     * @return a cryptographic hex digest ({@link String})
     */
    private String digestMany(Collection<?> entities) {
        return digestHex(entities.stream().
                map(entity -> digestHex(getDomainObjectXmlPartial(entity))).
                collect(Collectors.joining(SEP_CHAR)));
    }

    class PipelineConfigChangedListener extends EntityConfigChangedListener<PipelineConfig> {
        @Override
        public void onEntityConfigChange(PipelineConfig pipelineConfig) {
            removeFromCache(pipelineConfig, pipelineConfig.name());
        }
    }

    class SCMConfigChangedListener extends EntityConfigChangedListener<SCM> {
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

    private class PackageRepositoryChangeListener extends EntityConfigChangedListener<PackageRepository> {
        @Override
        public void onEntityConfigChange(PackageRepository repo) {
            removeFromCache(repo, repo.getId());
        }
    }

    class ElasticAgentProfileConfigListener extends EntityConfigChangedListener<ElasticProfile> {
        @Override
        public void onEntityConfigChange(ElasticProfile profile) {
            removeFromCache(profile, profile.getId());
        }
    }

    class SecretConfigListener extends EntityConfigChangedListener<SecretConfig> {
        @Override
        public void onEntityConfigChange(SecretConfig config) {
            removeFromCache(config, config.getId());
        }
    }

    class SCMChangeListener extends EntityConfigChangedListener<SCM> {
        @Override
        public void onEntityConfigChange(SCM scm) {
            removeFromCache(scm, scm.getId());
        }
    }

    class ClusterProfileListener extends EntityConfigChangedListener<ClusterProfile> {
        @Override
        public void onEntityConfigChange(ClusterProfile clusterProfile) {
            removeFromCache(clusterProfile, clusterProfile.getId());
        }
    }

    private class PackageListener extends EntityConfigChangedListener<PackageDefinition> {
        @Override
        public void onEntityConfigChange(PackageDefinition entity) {
            removeFromCache(entity, entity.getId());
        }
    }

    class SecurityAuthConfigListener extends EntityConfigChangedListener<SecurityAuthConfig> {
        @Override
        public void onEntityConfigChange(SecurityAuthConfig profile) {
            removeFromCache(profile, profile.getId());
        }
    }

    class ConfigRepoListener extends EntityConfigChangedListener<ConfigRepoConfig> {
        @Override
        public void onEntityConfigChange(ConfigRepoConfig entity) {
            removeFromCache(entity, entity.getId());
        }
    }

    private class RoleConfigListener extends EntityConfigChangedListener<Role> {
        @Override
        public void onEntityConfigChange(Role entity) {
            removeFromCache(entity, entity.getName());
        }
    }

    private class ArtifactStoreListener extends EntityConfigChangedListener<ArtifactStore> {
        @Override
        public void onEntityConfigChange(ArtifactStore entity) {
            removeFromCache(entity, entity.getId());
        }
    }

    private class AdminsConfigListener extends EntityConfigChangedListener<AdminsConfig> {
        @Override
        public void onEntityConfigChange(AdminsConfig entity) {
            removeFromCache(entity, "cacheKey");
        }
    }

    private class BasicPipelineConfigsChangedListener extends EntityConfigChangedListener<BasicPipelineConfigs> {
        @Override
        public void onEntityConfigChange(BasicPipelineConfigs pipelineConfigs) {
            removeFromCache(pipelineConfigs, pipelineConfigs.getGroup());
        }
    }

    private class MergePipelineConfigsChangedListener extends EntityConfigChangedListener<MergePipelineConfigs> {
        @Override
        public void onEntityConfigChange(MergePipelineConfigs pipelineConfigs) {
            removeFromCache(pipelineConfigs, pipelineConfigs.getGroup());
        }
    }

    class ArtifactConfigChangeListener extends EntityConfigChangedListener<ArtifactConfig> {
        @Override
        public void onEntityConfigChange(ArtifactConfig artifactConfig) {
            removeFromCache(artifactConfig, "cacheKey");
        }
    }
}
