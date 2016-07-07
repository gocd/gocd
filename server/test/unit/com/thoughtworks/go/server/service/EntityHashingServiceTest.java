package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
        entityHashingService.md5ForEntity(null, null);
    }

    @Test
    public void shouldComputeTheMD5OfAGivenXmlPartialGeneratedFromAnObject() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("P1");
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);

        assertThat(entityHashingService.md5ForEntity(pipelineConfig, "P1"), is(CachedDigestUtils.md5Hex(xml)));
    }

    @Test
    public void shouldReturnCachedMD5IfPresent() {
        when(goCache.get("GO_ETAG_CACHE", "p1")).thenReturn("pipeline_config_md5");

        assertThat(entityHashingService.md5ForPipelineConfig("P1"), is("pipeline_config_md5"));

        verifyZeroInteractions(goConfigService);
    }

    @Test
    public void shouldMakeCaseInsensitiveComparisonsOnCacheKeyWhileRetrievingMD5() {
        when(goCache.get("GO_ETAG_CACHE", "foo")).thenReturn("something");

        assertThat(entityHashingService.md5ForPipelineConfig("FOO"), is("something"));
    }

    @Test
    public void shouldRegisterToListenForConfigChange() {
        entityHashingService.initialize();

        verify(goConfigService).register(entityHashingService);
//        verify(goConfigService, times(1)).register(any(entityHashingService.new PipelineConfigChangedListener));
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

        verify(goCache).remove("GO_ETAG_CACHE", (pipelineConfig.getClass().getName() + "p1").toLowerCase());
    }
}
