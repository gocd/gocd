package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.EnvironmentConfig;
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
}
