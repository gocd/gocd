/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dashboard.GoDashboardCache;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static com.thoughtworks.go.server.dashboard.GoDashboardPipelineMother.pipeline;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardCacheUpdateServiceTest {
    @Mock
    private GoDashboardCache cache;
    @Mock
    private GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    @Mock
    private GoConfigService goConfigService;

    private GoDashboardCacheUpdateService service;

    private GoConfigMother configMother;
    private CruiseConfig config;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        configMother = new GoConfigMother();
        config = configMother.defaultCruiseConfig();

        service = new GoDashboardCacheUpdateService(cache, dashboardCurrentStateLoader, goConfigService);
    }

    @Test
    public void shouldUpdateCacheForPipelineGivenItsName() throws Exception {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfigs groupConfig = config.findGroup("group1");
        GoDashboardPipeline pipeline = pipeline("pipeline1");

        when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn(groupConfig);
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, groupConfig)).thenReturn(pipeline);

        service.updateForPipeline(new CaseInsensitiveString("pipeline1"));

        verify(cache).put(pipeline);
    }

    @Test
    public void shouldUpdateCacheForPipelineGivenItsConfig() throws Exception {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfigs groupConfig = config.findGroup("group1");
        GoDashboardPipeline pipeline = pipeline("pipeline1");

        when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn(groupConfig);
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, groupConfig)).thenReturn(pipeline);

        service.updateForPipeline(pipelineConfig);

        verify(cache).put(pipeline);
    }

    @Test
    public void shouldUpdateCacheForAllPipelinesInAGivenConfig() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1", "job1");
        GoDashboardPipeline pipeline1 = pipeline("pipeline1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2");

        List<GoDashboardPipeline> pipelines = asList(pipeline1, pipeline2);
        when(dashboardCurrentStateLoader.allPipelines(config)).thenReturn(pipelines);

        service.updateForAllPipelinesIn(config);

        verify(cache).replaceAllEntriesInCacheWith(pipelines);
    }
}