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
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.EnvironmentConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.security.ProductionIVProvider;
import com.thoughtworks.go.security.TestIVProvider;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.server.service.EntityHashingService.ETAG_CACHE_KEY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EntityHashingServiceTest {
    private GoConfigService goConfigService;
    private GoCache goCache;
    private EntityHashingService service;
    private EntityHashes digests;

    @BeforeEach
    void setUp() {
        this.goConfigService = mock(GoConfigService.class);
        this.goCache = mock(GoCache.class);
        digests = new EntityHashes(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins());
        this.service = new EntityHashingService(this.goConfigService, this.goCache, digests);
    }

    @Test
    void throwAnExceptionWhenObjectIsNull() {
        assertThrows(NullPointerException.class, () -> service.hashForEntity((EnvironmentConfig) null));
    }

    @Test
    void registersListenersForConfigChange() {
        service.initialize();

        verify(goConfigService).register(service);
    }

    @Test
    void digestsCombinedPluginInfoAndCollection() {
        final CombinedPluginInfo info1 = new CombinedPluginInfo(new NotificationPluginInfo(
                GoPluginDescriptor.builder().id("foo").build(),
                new PluggableInstanceSettings(List.of(new PluginConfiguration("user", null)))
        ));
        final CombinedPluginInfo info2 = new CombinedPluginInfo(new NotificationPluginInfo(
                GoPluginDescriptor.builder().id("bar").build(),
                new PluggableInstanceSettings(List.of(new PluginConfiguration("user", null)))
        ));
        final Collection<CombinedPluginInfo> many = List.of(info1, info2);

        final String actual = service.hashForEntity(many);
        assertTrue(actual.matches("[a-f0-9]{64}"));

        assertEquals(digests.digestMany(
                service.hashForEntity(info1),
                service.hashForEntity(info2)
        ), actual);
    }

    @Test
    @DisplayName("when plugin settings contain secret properties, the digest used for" +
            "ETags should not change as long as the decrypted values remain the same, " +
            "even if the crypto salt changes between requests")
    void digestIsConsistentForPluginSettingsWithSecretPropertiesEvenWhenCryptoSaltChanges() {
        TestIVProvider.with(new ProductionIVProvider(), () -> {
            final String id = "com.foo.plugin";
            final String key = "can you keep a secret?";
            final String secret = "nope!";
            final String pluginSettingsCacheKey = "com.thoughtworks.go.server.domain.PluginSettings.com.foo.plugin";

            final PluginSettings p1 = pluginSettings(id, key, secret);
            final PluginSettings p2 = pluginSettings(id, key, secret);

            assertNotEquals(
                    p1.getPluginSettingsProperties().get(0).getEncryptedValue(),
                    p2.getPluginSettingsProperties().get(0).getEncryptedValue(),
                    "both entities should have different cipherTexts even though the input values are equal"
            );

            final String expected = service.hashForEntity(p1);

            // don't care about the actual value, just that we get what looks like a digest
            assertTrue(expected.matches("[a-f0-9]{64}"));
            verify(goCache).put(ETAG_CACHE_KEY, pluginSettingsCacheKey, expected);

            // Even though this is a mock and the values are never really cached, I'm
            // putting this here for logical correctness to demonstrate intent: we must
            // be sure that our assertion below does not test cached copies.
            //
            // Also, if we ever throw in a real cache instead of a mock (unlikely, but
            // you never know), this test should remain correct.
            service.removeFromCache(p1, p1.getPluginId());
            verify(goCache).remove(ETAG_CACHE_KEY, pluginSettingsCacheKey);

            final String second = service.hashForEntity(p2);
            assertEquals(expected, second, "given the same plaintext, two PluginSettings should have equal digests");
            verify(goCache, times(2)).put(ETAG_CACHE_KEY, pluginSettingsCacheKey, expected);
        });
    }

    @Test
    void invalidatesPipelineConfigETagsFromCacheOnConfigChange() {
        service.onConfigChange(null);

        verify(goCache).remove(ETAG_CACHE_KEY);
    }

    @Test
    void invalidatesPipelineConfigETagsFromCacheOnPipelineChange() {
        EntityHashingService.PipelineConfigChangedListener listener = service.new PipelineConfigChangedListener();

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("P1");
        listener.onEntityConfigChange(pipelineConfig);

        verify(goCache).remove(ETAG_CACHE_KEY, (pipelineConfig.getClass().getName() + "." + "p1"));
    }

    @Test
    void entityChecksumIsIdenticalForObjectsWithCaseInsensitiveName() {
        BasicEnvironmentConfig environment = EnvironmentConfigMother.environment("UPPER_CASE_NAME");
        when(goCache.get(ETAG_CACHE_KEY, "com.thoughtworks.go.config.BasicEnvironmentConfig.upper_case_name")).thenReturn("foo");

        assertEquals("foo", service.hashForEntity(environment));

        verify(goCache).get(ETAG_CACHE_KEY, "com.thoughtworks.go.config.BasicEnvironmentConfig.upper_case_name");
        verifyNoMoreInteractions(goCache);
    }

    @Test
    @DisplayName("hashForEntity() can determine the proper overloaded method for implementations of " +
            "EnvironmentConfig and List<EnvironmentConfig> without ambiguity")
    void hashesEnvironmentConfigsWithoutClassAmbiguityIssues() {
        // important to test these when typed as the non-specific parent interface
        final EnvironmentConfig basic = new BasicEnvironmentConfig(new CaseInsensitiveString("hello"));
        final EnvironmentConfig merged = new MergeEnvironmentConfig(basic);
        final EnvironmentsConfig mult = new EnvironmentsConfig();
        mult.add(basic);
        final EnvironmentsConfig nested = new EnvironmentsConfig();
        nested.add(merged);

        // resolving the wrong overload might result in an exception indicating that the object "does not
        // have a ConfigTag"
        assertDoesNotThrow(() -> {
                    assertTrue(isNotBlank(service.hashForEntity(basic)));
                    assertTrue(isNotBlank(service.hashForEntity(merged)));
                    assertTrue(isNotBlank(service.hashForEntity(mult)));
                    assertTrue(isNotBlank(service.hashForEntity(nested)));
                }
        );
    }

    @Test
    void treatsMergeEnvironmentConfigsAsCollectionsOfEnvironmentConfigs() {
        BasicEnvironmentConfig env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        BasicEnvironmentConfig env2 = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        MergeEnvironmentConfig merged = new MergeEnvironmentConfig(env1, env2);

        when(goCache.get(ETAG_CACHE_KEY, "com.thoughtworks.go.config.BasicEnvironmentConfig.env")).
                thenReturn("foo").
                thenReturn("bar");

        final String type = MergeEnvironmentConfig.class.getSimpleName();
        assertEquals(digests.digestMany(type, "foo", "bar"), service.hashForEntity(merged));

        verify(goCache, times(2)).get(ETAG_CACHE_KEY, "com.thoughtworks.go.config.BasicEnvironmentConfig.env");
        verifyNoMoreInteractions(goCache);
    }

    @Test
    void invalidatesArtifactConfigETagsFromCacheOnConfigChange() {
        EntityHashingService.ArtifactConfigChangeListener artifactConfigChangeListener = service.new ArtifactConfigChangeListener();
        ArtifactConfig artifactConfig = new ArtifactConfig();

        artifactConfigChangeListener.onEntityConfigChange(artifactConfig);

        verify(goCache).remove(ETAG_CACHE_KEY, (artifactConfig.getClass().getName() + ".cacheKey"));
    }

    @Test
    void hashesForAPipelineConfigDiffersForDifferentPlugins() {
        PipelineConfig test = PipelineConfigMother.pipelineConfig("test");

        String hashForPlugin1 = service.hashForEntity(test, "group1", "plugin_1");
        String hashForPlugin2 = service.hashForEntity(test, "group1", "plugin_2");

        assertNotEquals(hashForPlugin1, hashForPlugin2);
    }

    private PluginSettings pluginSettings(String id, String key, String secret) {
        final PluginSettings p = new PluginSettings(id);
        final ConfigurationProperty cp = new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(secret));
        cp.handleSecureValueConfiguration(true);
        p.getPluginSettingsProperties().add(cp);
        return p;
    }
}
