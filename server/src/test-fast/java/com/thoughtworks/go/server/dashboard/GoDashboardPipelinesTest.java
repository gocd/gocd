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

import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoDashboardPipelinesTest {
    @Test
    public void shouldSetLastUpdatedTime() {
        TimeStampBasedCounter provider = mock(TimeStampBasedCounter.class);
        when(provider.getNext()).thenReturn(100L);
        GoDashboardPipelines goDashboardPipelines = new GoDashboardPipelines(new HashMap<>(), provider);
        assertThat(goDashboardPipelines.lastUpdatedTimeStamp(), is(100L));
    }
}
