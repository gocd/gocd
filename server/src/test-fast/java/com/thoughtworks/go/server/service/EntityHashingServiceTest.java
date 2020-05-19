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
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.EnvironmentConfigMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EntityHashingServiceTest {
    private GoConfigService goConfigService;
    private GoCache goCache;
    private EntityHashingService entityHashingService;
    private ConfigCache configCache;
    private ConfigElementImplementationRegistry registry;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        this.goConfigService = mock(GoConfigService.class);
        this.goCache = mock(GoCache.class);
        this.configCache = new ConfigCache();
        this.registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        this.entityHashingService = new EntityHashingService(this.goConfigService, this.goCache, configCache, registry);
    }

    @Test
    public void shouldThrowAnExceptionWhenObjectIsNull() {
        thrown.expect(NullPointerException.class);
        entityHashingService.hashForEntity((EnvironmentConfig) null);
    }

    @Test
    public void shouldComputeTheDigestOfAGivenXmlPartialGeneratedFromAnObject() {
        BasicEnvironmentConfig environment = EnvironmentConfigMother.environment("P1");
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(environment);

        assertThat(entityHashingService.hashForEntity(environment), is(sha512_256Hex(xml)));
    }

    @Test
    public void shouldRegisterToListenForConfigChange() {
        entityHashingService.initialize();

        verify(goConfigService).register(entityHashingService);
    }

    @Test
    public void shouldUseObjectHashCodeForPluginSettings() {
        PluginSettings pluginSettings = new PluginSettings("com.foo.plugin");
        String expected = "be09fc88146dad0d8d80dad98cfbf038689775685ae7c43003745f054dede879";

        String actual = entityHashingService.hashForEntity(pluginSettings);

        assertThat(actual, is(expected));
        verify(goCache).put("GO_ETAG_CACHE", "com.thoughtworks.go.server.domain.PluginSettings.com.foo.plugin", expected);
    }

    @Test
    public void shouldInvalidatePipelineConfigEtagsFromCacheOnConfigChange() {
        entityHashingService.onConfigChange(null);

        verify(goCache).remove("GO_ETAG_CACHE");
    }

    @Test
    public void shouldInvalidatePipelineConfigEtagsFromCacheOnPipelineChange() {
        EntityHashingService.PipelineConfigChangedListener listener = entityHashingService.new PipelineConfigChangedListener();

        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("P1");
        listener.onEntityConfigChange(pipelineConfig);

        verify(goCache).remove("GO_ETAG_CACHE", (pipelineConfig.getClass().getName() + "." + "p1"));
    }

    @Test
    public void entityChecksumIsIdenticalForObjectsWithCaseInsensitiveName() throws Exception {
        BasicEnvironmentConfig environment = EnvironmentConfigMother.environment("UPPER_CASE_NAME");
        when(goCache.get("GO_ETAG_CACHE", "com.thoughtworks.go.config.BasicEnvironmentConfig.upper_case_name")).thenReturn("foo");
        String checksum = entityHashingService.hashForEntity(environment);
        assertThat(checksum, is("foo"));
        verify(goCache).get("GO_ETAG_CACHE", "com.thoughtworks.go.config.BasicEnvironmentConfig.upper_case_name");
        verifyNoMoreInteractions(goCache);
    }

    @Test
    public void shouldNotAccessCacheIfTheEnvironmentConfigIsAnInstanceOfMergeEnvConfig() {
        BasicEnvironmentConfig basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        MergeEnvironmentConfig mergeEnvConfig = new MergeEnvironmentConfig(basicEnvConfig);

        entityHashingService.hashForEntity(mergeEnvConfig);

        verifyZeroInteractions(goCache);
    }

    @Test
    public void shouldAccessCacheIfTheEnvironmentConfigIsAnInstanceOfBasicEnvConfig() {
        BasicEnvironmentConfig basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        when(goCache.get("GO_ETAG_CACHE", "com.thoughtworks.go.config.BasicEnvironmentConfig.env")).thenReturn("foo");

        String digest = entityHashingService.hashForEntity(basicEnvConfig);

        assertThat(digest, is("foo"));
        verify(goCache).get("GO_ETAG_CACHE", "com.thoughtworks.go.config.BasicEnvironmentConfig.env");
        verifyNoMoreInteractions(goCache);
    }

    @Test
    public void shouldInvalidateArtifactConfigEtagsFromCacheOnConfigChange() {
        EntityHashingService.ArtifactConfigChangeListener artifactConfigChangeListener = entityHashingService.new ArtifactConfigChangeListener();
        ArtifactConfig artifactConfig = new ArtifactConfig();

        artifactConfigChangeListener.onEntityConfigChange(artifactConfig);

        verify(goCache).remove("GO_ETAG_CACHE", (artifactConfig.getClass().getName() + ".cacheKey"));
    }

    @Test
    public void computeHashForEntity() {
        assertEquals(
                "Given the same structure and origin, computeHashForEntity() should output the same hash code",
                entityHashingService.computeHashForEntity(PartialConfigMother.withPipeline("foo")),
                entityHashingService.computeHashForEntity(PartialConfigMother.withPipeline("foo"))
        );

        assertNotEquals(
                "Given structurally different partials, computeHashForEntity() outputs different hash codes",
                entityHashingService.computeHashForEntity(PartialConfigMother.withPipeline("foo")),
                entityHashingService.computeHashForEntity(PartialConfigMother.withPipeline("bar"))
        );

        PartialConfig a = PartialConfigMother.withPipeline("foo");
        PartialConfig b = PartialConfigMother.withPipeline("bar");
        b.setOrigin(new RepoConfigOrigin(((RepoConfigOrigin) b.getOrigin()).getConfigRepo(), "something-else"));

        assertNotEquals(
                "Given structurally equal partials, but different origins, computeHashForEntity() outputs different hash codes",
                entityHashingService.computeHashForEntity(a),
                entityHashingService.computeHashForEntity(b)
        );
    }
}
