/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
        entityHashingService.md5ForEntity((EnvironmentConfig) null);
    }

    @Test
    public void shouldComputeTheMD5OfAGivenXmlPartialGeneratedFromAnObject() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("P1");
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);

        assertThat(entityHashingService.md5ForEntity(pipelineConfig), is(CachedDigestUtils.md5Hex(xml)));
    }

    @Test
    public void shouldRegisterToListenForConfigChange() {
        entityHashingService.initialize();

        verify(goConfigService).register(entityHashingService);
    }

    @Test
    public void shouldUseObjectHashCodeForPluginSettings() {
        PluginSettings pluginSettings = new PluginSettings("com.foo.plugin");
        String expectedMd5 = "8f54eed0331c2bd93ca4cf8f470f4406";

        String actualMd5 = entityHashingService.md5ForEntity(pluginSettings);

        assertThat(actualMd5, is(expectedMd5));
        verify(goCache).put("GO_ETAG_CACHE", "com.thoughtworks.go.server.domain.PluginSettings.com.foo.plugin", expectedMd5);
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
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("UPPER_CASE_NAME");
        when(goCache.get("GO_ETAG_CACHE", "com.thoughtworks.go.config.PipelineConfig.upper_case_name")).thenReturn("foo");
        String checksum = entityHashingService.md5ForEntity(pipelineConfig);
        assertThat(checksum, is("foo"));
        verify(goCache).get("GO_ETAG_CACHE", "com.thoughtworks.go.config.PipelineConfig.upper_case_name");
        verifyNoMoreInteractions(goCache);
    }

    @Test
    public void shouldNotAccessCacheIfTheEnvironmentConfigIsAnInstanceOfMergeEnvConfig() {
        BasicEnvironmentConfig basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        MergeEnvironmentConfig mergeEnvConfig = new MergeEnvironmentConfig(basicEnvConfig);

        entityHashingService.md5ForEntity(mergeEnvConfig);

        verifyZeroInteractions(goCache);
    }

    @Test
    public void shouldAccessCacheIfTheEnvironmentConfigIsAnInstanceOfBasicEnvConfig() {
        BasicEnvironmentConfig basicEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env"));
        when(goCache.get("GO_ETAG_CACHE", "com.thoughtworks.go.config.BasicEnvironmentConfig.env")).thenReturn("foo");

        String md5 = entityHashingService.md5ForEntity(basicEnvConfig);

        assertThat(md5, is("foo"));
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
}
