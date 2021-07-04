/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.PipelineLockStatusChangeListener;
import com.thoughtworks.go.server.service.GoDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GoDashboardPipelineLockStatusChangeHandlerTest {
    @Mock
    private GoDashboardService cacheUpdateService;

    private GoDashboardPipelineLockStatusChangeHandler handler;

    @BeforeEach
    public void setUp() throws Exception {

        handler = new GoDashboardPipelineLockStatusChangeHandler(cacheUpdateService);
    }

    @Test
    public void shouldHandlePipelineLockStatusChangeByRefreshingPipelineInCache() throws Exception {
        handler.call(PipelineLockStatusChangeListener.Event.lock("pipeline1"));

        verify(cacheUpdateService).updateCacheForPipeline(new CaseInsensitiveString("pipeline1"));
    }
}