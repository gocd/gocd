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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.service.GoDashboardCurrentStateLoader;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.thoughtworks.go.server.dashboard.GoDashboardPipelineMother.pipeline;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardStageStatusChangeHandlerTest {
    @Mock
    private GoDashboardCache cache;
    @Mock
    private GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    @Mock
    private GoConfigService goConfigService;

    private BasicCruiseConfig config;
    private GoConfigMother configMother;
    private GoDashboardStageStatusChangeHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        configMother = new GoConfigMother();
        config = configMother.defaultCruiseConfig();

        handler = new GoDashboardStageStatusChangeHandler(cache, dashboardCurrentStateLoader, goConfigService);
    }

    @Test
    public void shouldRefreshPipelineInCacheWhenStageStatusChanges() throws Exception {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfigs groupConfig = config.findGroup("group1");
        GoDashboardPipeline pipeline = pipeline("pipeline1", "group1");

        when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("pipeline1"))).thenReturn(groupConfig);
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, groupConfig)).thenReturn(pipeline);

        handler.call(StageMother.scheduledStage("pipeline1", 1, "stage1", 2, "job1"));

        verify(cache).put(pipeline);
    }
}