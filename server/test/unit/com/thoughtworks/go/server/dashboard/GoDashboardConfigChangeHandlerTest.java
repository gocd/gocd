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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoDashboardCurrentStateLoader;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static com.thoughtworks.go.server.dashboard.GoDashboardPipelineMother.pipeline;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardConfigChangeHandlerTest {
    @Mock
    private GoDashboardCache cache;
    @Mock
    private GoDashboardCurrentStateLoader dashboardCurrentStateLoader;
    @Mock
    private GoConfigService goConfigService;

    private GoDashboardConfigChangeHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        handler = new GoDashboardConfigChangeHandler(cache, dashboardCurrentStateLoader, goConfigService);
    }

    @Test
    public void shouldReplaceAllEntriesInCacheWithNewEntriesWhenTheWholeConfigHasChanged() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2");
        List<GoDashboardPipeline> newPipelines = asList(pipeline("pipeline1"), pipeline("pipeline2"));
        when(dashboardCurrentStateLoader.allPipelines(config)).thenReturn(newPipelines);

        handler.call(config);

        verify(cache).replaceAllEntriesInCacheWith(newPipelines);
    }

    @Test
    public void shouldReplaceOnlyTheExistingPipelineEntryInCacheWithANewEntryWhenOnlyPipelineConfigHasChanged() throws Exception {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2");
        PipelineConfig pipelineConfig = config.getPipelineConfigByName(new CaseInsensitiveString("pipeline1"));

        GoDashboardPipeline newPipeline = pipeline("pipeline1");
        when(goConfigService.findGroupByPipeline(pipelineConfig.name())).thenReturn(config.getGroups().first());
        when(dashboardCurrentStateLoader.pipelineFor(pipelineConfig, config.getGroups().first())).thenReturn(newPipeline);

        handler.call(pipelineConfig);

        verify(cache).put(newPipeline);
    }
}