/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv3.dashboard

import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.dashboard.TimeStampBasedCounter
import com.thoughtworks.go.util.Clock

import static com.thoughtworks.go.helpers.PipelineModelMother.pipeline_model
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class GoDashboardPipelineMother {

  static GoDashboardPipeline dashboardPipeline(pipeline_name, group_name = "group1", permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE), timestamp = 1000L) {
    def clock = mock(Clock.class)
    when(clock.currentTimeMillis()).thenReturn(timestamp)
    new GoDashboardPipeline(pipeline_model(pipeline_name, 'pipeline-label'), permissions, group_name, null, new TimeStampBasedCounter(clock), new FileConfigOrigin(), 0)
  }
}
