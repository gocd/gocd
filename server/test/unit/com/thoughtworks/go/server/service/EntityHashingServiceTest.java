package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.util.EntityDigest;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EntityHashingServiceTest {
    private EntityDigest entityDigest;
    private GoConfigService goConfigService;
    private GoCache goCache;
    private EntityHashingService entityHashingService;

    @Before
    public void setUp() {
        this.entityDigest = mock(EntityDigest.class);
        this.goConfigService = mock(GoConfigService.class);
        this.goCache = mock(GoCache.class);
        this.entityHashingService = new EntityHashingService(this.goConfigService, this.goCache);

        this.entityHashingService.initializeWith(entityDigest);
    }

    @Test
    public void shouldGenerateMD5ForAPipelineConfig() {
        PipelineConfig pipelineConfig = mock(PipelineConfig.class);

        when(goConfigService.pipelineConfigNamed(any(CaseInsensitiveString.class))).thenReturn(pipelineConfig);
        when(entityDigest.md5ForPipeline(pipelineConfig)).thenReturn("pipeline_config_md5");

        String md5ForPipeline = entityHashingService.md5ForPipelineConfig("P1");

        assertThat(md5ForPipeline, is("pipeline_config_md5"));
        verify(goCache).put("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p1", "pipeline_config_md5");
    }

    @Test
    public void shouldReturnCachedMD5IfPresent() {
        when(goCache.get("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p1")).thenReturn("pipeline_config_md5");

        assertThat(entityHashingService.md5ForPipelineConfig("P1"), is("pipeline_config_md5"));

        verifyZeroInteractions(goConfigService);
        verifyZeroInteractions(entityDigest);
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

        verify(goCache).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE");
    }

    @Test
    public void shouldInvalidatePipelineConfigEtagsFromCacheOnPipelineChange() {
        EntityHashingService.PipelineConfigChangedListener listener = entityHashingService.new PipelineConfigChangedListener();

        listener.onEntityConfigChange(PipelineConfigMother.pipelineConfig("P1"));

        verify(goCache).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p1");
    }
}
