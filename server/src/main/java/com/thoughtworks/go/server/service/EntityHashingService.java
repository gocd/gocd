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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.PipelineGroups;
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
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.service.lookups.CommandSnippet;
import com.thoughtworks.go.server.service.lookups.CommandSnippets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Component
public class EntityHashingService implements ConfigChangedListener, Initializer {
    static final String ETAG_CACHE_KEY = "GO_ETAG_CACHE";

    private final GoConfigService goConfigService;
    private final GoCache goCache;
    private final EntityHashes hashes;

    @Autowired
    public EntityHashingService(GoConfigService goConfigService, GoCache goCache, EntityHashes hashes) {
        this.goConfigService = goConfigService;
        this.goCache = goCache;
        this.hashes = hashes;
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
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(NotificationFilter filter) {
        String cacheKey = cacheKey(filter, String.valueOf(filter.getId()));
        return getNonConfigEntityDigestFromCache(cacheKey, filter);
    }

    public String hashForEntity(EnvironmentConfig config) {
        if (config instanceof MergeEnvironmentConfig) {
            return hashForEntity((MergeEnvironmentConfig) config);
        }

        String cacheKey = cacheKey(config, config.name());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(MergeEnvironmentConfig config) {
        return hashForEntity((List<EnvironmentConfig>) config);
    }

    public String hashForEntity(EnvironmentsConfig config) {
        return hashForEntity((List<EnvironmentConfig>) config);
    }

    public String hashForEntity(List<EnvironmentConfig> envs) {
        return hashes.digestMany(Stream.concat(
                Stream.of(envs.getClass().getSimpleName()),
                envs.stream().map(this::hashForEntity)
        ).toArray(String[]::new));
    }

    public String hashForEntity(PackageRepository config) {
        String cacheKey = cacheKey(config, config.getId());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(PackageRepositories packageRepositories) {
        return compound(packageRepositories, this::hashForEntity);
    }

    public String hashForEntity(SCM config) {
        String cacheKey = cacheKey(config, config.getName());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(SCMs scms) {
        return compound(scms, this::hashForEntity);
    }

    public String hashForEntity(ConfigRepoConfig config) {
        String cacheKey = cacheKey(config, config.getId());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(ElasticProfile config) {
        String cacheKey = cacheKey(config, config.getId());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(ClusterProfile profile) {
        String cacheKey = cacheKey(profile, profile.getId());
        return getConfigEntityDigestFromCache(cacheKey, profile);
    }

    public String hashForEntity(SecretConfig config) {
        String cacheKey = cacheKey(config, config.getId());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(SecretConfigs secretConfigs) {
        return compound(secretConfigs, this::hashForEntity);
    }

    public String hashForEntity(CommandSnippet commandSnippet) {
        String cacheKey = cacheKey(commandSnippet, commandSnippet.getName());
        return getNonConfigEntityDigestFromCache(cacheKey, commandSnippet);
    }

    public String hashForEntity(CommandSnippets commandSnippets) {
        return compound(commandSnippets.getSnippets(), this::hashForEntity);
    }

    public String hashForEntity(SecurityAuthConfig config) {
        String cacheKey = cacheKey(config, config.getId());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(SecurityAuthConfigs authConfigs) {
        return compound(authConfigs, this::hashForEntity);
    }

    public String hashForEntity(Role config) {
        String cacheKey = cacheKey(config, config.getName());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(RolesConfig roles) {
        return compound(roles, this::hashForEntity);
    }

    public String hashForEntity(AdminsConfig config) {
        String cacheKey = cacheKey(config, "cacheKey");
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(PackageDefinition config) {
        String cacheKey = cacheKey(config, config.getId());
        return getConfigEntityDigestFromCache(cacheKey, config);
    }

    public String hashForEntity(Packages config) {
        return compound(config, this::hashForEntity);
    }

    public String hashForEntity(PluginSettings pluginSettings) {
        String cacheKey = cacheKey(pluginSettings, pluginSettings.getPluginId());
        return getNonConfigEntityDigestFromCache(cacheKey, pluginSettings);
    }

    public String hashForEntity(ArtifactStore artifactStore) {
        String cacheKey = cacheKey(artifactStore, artifactStore.getId());
        return getNonConfigEntityDigestFromCache(cacheKey, artifactStore);
    }

    public String hashForEntity(ParamConfig paramConfig) {
        String cacheKey = cacheKey(paramConfig, paramConfig.getName());
        return getConfigEntityDigestFromCache(cacheKey, paramConfig);
    }

    public String hashForEntity(ParamsConfig paramConfigs) {
        return compound(paramConfigs, this::hashForEntity);
    }

    public String hashForEntity(PipelineConfig pipelineConfig, String groupName) {
        return getFromCache(
                cacheKey(pipelineConfig, pipelineConfig.name()),
                () -> hashes.digestMany(
                        hashes.digestDomainConfigEntity(pipelineConfig),
                        groupName
                )
        );
    }

    public String hashForEntity(PipelineConfig pipelineConfig, String groupName, String pluginId) {
        return hashes.digestMany(
                hashes.digestDomainConfigEntity(pipelineConfig),
                groupName,
                pluginId
        );
    }

    public String hashForEntity(PipelineConfigs pipelineConfigs) {
        String cacheKey = cacheKey(pipelineConfigs, pipelineConfigs.getGroup());
        return getConfigEntityDigestFromCache(cacheKey, pipelineConfigs);
    }

    public String hashForEntity(PipelineGroups pipelineGroups) {
        return compound(pipelineGroups, this::hashForEntity);
    }

    public String hashForEntity(CombinedPluginInfo pluginInfo) {
        String cacheKey = cacheKey(pluginInfo, String.format("plugin_info_%s", pluginInfo.getDescriptor().id()));
        return getNonConfigEntityDigestFromCache(cacheKey, pluginInfo);
    }

    public String hashForEntity(Collection<CombinedPluginInfo> pluginInfos) {
        return compound(pluginInfos, this::hashForEntity);
    }

    public String hashForEntity(ArtifactConfig artifactConfig) {
        String cacheKey = cacheKey(artifactConfig, "cacheKey");
        return getFromCache(cacheKey, () -> String.valueOf(Objects.hash(artifactConfig)));
    }

    public void removeFromCache(Object domainObject, CaseInsensitiveString name) {
        removeFromCache(domainObject, name.toLower());
    }

    public void removeFromCache(Object domainObject, String name) {
        goCache.remove(ETAG_CACHE_KEY, cacheKey(domainObject, name));
    }

    /**
     * Creates a compound digest over a set of entities. While similar to {@link EntityHashes#digestMany(Collection)},
     * the advantage of abstracting over {@link EntityHashes#digestMany(String...)} is that we can specify a digest
     * function that takes advantage of fragment caching, such as the various {@code hashForEntity(T entity)} methods
     * in this class. Simply use the {@code this::hashForEntity} method reference as the {@code hashFn} parameter.
     *
     * @param entities the collection of entities for which to generate a compound digest
     * @param hashFn   the underlying digest function for members of the collection
     * @param <T>      a domain entity type
     * @return a single digest representing the contents of the collection
     */
    private <T> String compound(Collection<T> entities, Function<T, String> hashFn) {
        return hashes.digestMany(entities.stream().
                map(hashFn).
                toArray(String[]::new));
    }

    private String cacheKey(Object domainObject, CaseInsensitiveString name) {
        return cacheKey(domainObject, name.toLower());
    }

    private String cacheKey(Object domainObject, String name) {
        return classnameOf(domainObject) + "." + name;
    }

    /**
     * Digests and caches an entity that is not represented in configuration, but is either hydrated from the
     * database or is otherwise some other ad-hoc object. This delegates to {@link EntityHashes#digestDomainNonConfigEntity(Object)}
     * which will do a full content digest.
     * <p>
     * It's important to note that previous implementations of non-config entity digesting sometimes relied on the
     * output of {@link Object#hashCode()}; this will not produce unique digests because it is fairly easy to produce
     * collisions in {@link Object#hashCode()}, particularly for string data.
     * <p>
     * Don't do that.
     * <p>
     * {@link Object#hashCode()} was never intended to be a unique representation; its intent was to optimally
     * distribute objects in memory slots when used with hashed collections such as {@link java.util.HashMap}.
     *
     * @param cacheKey a {@link String} that describes a cache location unique to this entity
     * @param entity   the non-config entity to be digested
     * @return the content digest of the entity (possibly retrieved from a cache hit)
     */
    private String getNonConfigEntityDigestFromCache(String cacheKey, Object entity) {
        return getFromCache(cacheKey, () -> hashes.digestDomainNonConfigEntity(entity));
    }

    /**
     * Digests and caches a configuration entity This delegates to {@link EntityHashes#digestDomainConfigEntity(Object)}
     * which will do a full content digest.
     *
     * @param cacheKey a {@link String} that describes a cache location unique to this entity
     * @param entity   the config entity to be digested
     * @return the content digest of the entity (possibly retrieved from a cache hit)
     */
    private String getConfigEntityDigestFromCache(String cacheKey, Object entity) {
        return getFromCache(cacheKey, () -> hashes.digestDomainConfigEntity(entity));
    }

    private String getFromCache(String cacheKey, Supplier<String> digestSupplier) {
        final String cached = (String) goCache.get(ETAG_CACHE_KEY, cacheKey);

        if (cached != null) {
            return cached;
        }

        final String digest = digestSupplier.get();
        goCache.put(ETAG_CACHE_KEY, cacheKey, digest);

        return digest;
    }

    private String classnameOf(Object entity) {
        return entity.getClass().getName();
    }
}
