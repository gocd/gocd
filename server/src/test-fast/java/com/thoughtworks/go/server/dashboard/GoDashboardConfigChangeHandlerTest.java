/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigHolder;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.GoDashboardService;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardConfigChangeHandlerTest {
    @Mock
    private GoDashboardService cacheUpdateService;
    @Mock
    private GoConfigService goConfigService;

    private GoDashboardConfigChangeHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(goConfigService.combinedMD5()).thenReturn(new GoConfigHolder.Checksum("config-md5", "partials-md5"));
        handler = new GoDashboardConfigChangeHandler(cacheUpdateService, goConfigService);
    }

    @Test
    public void shouldReplaceAllEntriesInCacheWithNewEntriesWhenTheWholeConfigHasChanged() throws Exception {
        when(goConfigService.combinedMD5()).thenReturn(new GoConfigHolder.Checksum("config-md5-1", "partials-md5"));
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2");

        handler.call(config);

        verify(cacheUpdateService).updateCacheForAllPipelinesIn(config);
    }

    @Test
    public void shouldReplaceOnlyTheExistingPipelineEntryInCacheWithANewEntryWhenOnlyPipelineConfigHasChanged() throws Exception {
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        PipelineConfig pipelineConfig = new GoConfigMother().addPipeline(config, "pipeline1", "stage1", "job1");

        handler.call(pipelineConfig);

        verify(cacheUpdateService).updateCacheForPipeline(pipelineConfig);
    }

    @Test
    public void shouldUpdateTheFullConfigOnlyIfTheConfigHasChangedFromLastUpdate(){
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2");
        when(goConfigService.combinedMD5()).thenReturn(new GoConfigHolder.Checksum("config-md5-1", "partials-md5"));

        handler.call(config);
        verify(cacheUpdateService).updateCacheForAllPipelinesIn(config);

        handler.call(config);
        verifyNoMoreInteractions(cacheUpdateService);
    }
}