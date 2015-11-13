/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service;

import java.util.Map;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.presentation.CanDeleteResult;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PipelineConfigServiceTest {

    private PipelineConfigService pipelineConfigService;
    private CruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private GoCache goCache;

    @Before
    public void setUp() throws Exception {
        PipelineConfigs configs = createGroup("group", "pipeline", "in_env");
        downstream(configs);
        cruiseConfig = new BasicCruiseConfig(configs);
        cruiseConfig.addEnvironment(environment("foo", "in_env"));

        goConfigService = mock(GoConfigService.class);
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        when(goConfigService.getConfigForEditing()).thenReturn(cruiseConfig);
        goCache = mock(GoCache.class);
        PipelineConfigurationCache.getInstance().onConfigChange(cruiseConfig);
        pipelineConfigService = new PipelineConfigService(goConfigService, goCache);
    }

    @Test
    public void shouldBeAbleToGetTheCanDeleteStatusOfAllPipelines() {

        Map<CaseInsensitiveString, CanDeleteResult> pipelineToCanDeleteIt = pipelineConfigService.canDeletePipelines();

        assertThat(pipelineToCanDeleteIt.size(), is(3));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("down")), is(new CanDeleteResult(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("in_env")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT", new CaseInsensitiveString("in_env"), new CaseInsensitiveString("foo")))));
        assertThat(pipelineToCanDeleteIt.get(new CaseInsensitiveString("pipeline")), is(new CanDeleteResult(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("down")))));
    }

    @Test
    public void shouldGetPipelineConfigBasedOnName() {
        String pipelineName = "pipeline";
        PipelineConfigurationCache.getInstance().onConfigChange(cruiseConfig);
        PipelineConfig pipeline = pipelineConfigService.getPipelineConfig(pipelineName);
        assertThat(pipeline, is(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName))));
    }

    @Test
    public void shouldRemovePipelineConfigFromCacheWhenPresentOnPipelineConfigChange() {
        when(goCache.get("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p")).thenReturn(new Object());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipelineConfigService.onPipelineConfigChange(pipelineConfig, "grp");
        verify(goCache).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p");
    }

    @Test
    public void shouldNotRemovePipelineConfigFromCacheWhenNotPresentOnPipelineConfigChange(){
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipelineConfigService.onPipelineConfigChange(pipelineConfig, "grp");
        verify(goCache, never()).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE", "p");
    }
    @Test
    public void shouldRemovePipelineConfigsFromCacheWhenPresentOnGoConfigChange() {
        when(goCache.get("GO_PIPELINE_CONFIGS_ETAGS_CACHE")).thenReturn(new Object());
        pipelineConfigService.onConfigChange(cruiseConfig);
        verify(goCache).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE");
    }

    @Test
    public void shouldNotRemovePipelineConfigsFromCacheWhenNotPresentOnGoConfigChange(){
        pipelineConfigService.onConfigChange(cruiseConfig);
        verify(goCache, never()).remove("GO_PIPELINE_CONFIGS_ETAGS_CACHE");
    }

    private void downstream(PipelineConfigs configs) {
        PipelineConfig down = PipelineConfigMother.pipelineConfig("down");
        down.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("mingle")));
        configs.add(down);
    }
}
