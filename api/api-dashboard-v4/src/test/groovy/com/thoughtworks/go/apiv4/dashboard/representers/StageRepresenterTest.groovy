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
package com.thoughtworks.go.apiv4.dashboard.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.TrackingTool
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.permissions.NoOnePermission
import com.thoughtworks.go.config.security.users.NoOne
import com.thoughtworks.go.domain.*
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel
import com.thoughtworks.go.server.dashboard.Counter
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.helpers.PipelineModelMother.pipeline_model
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class StageRepresenterTest {

  @Test
  void 'renders stages and previous stage with hal representation'() {
    def previousStageInstance = new StageInstanceModel('stage1', '1', StageResult.Cancelled, new StageIdentifier('pipeline-name', 23, 'stage', '1'))
    def stageInstance = new StageInstanceModel('stage2', '2', StageResult.Cancelled, new StageIdentifier('pipeline-name', 23, 'stage', '2'))
    stageInstance.setPreviousStage(previousStageInstance)
    stageInstance.setApprovedBy('go-user')
    def jobState = JobState.Building
    def jobResult = JobResult.Passed
    def date = new Date(1367472329111)
    stageInstance.getBuildHistory().addJob("jobName", jobState, jobResult, date)

    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
    pipelineConfig.setOrigin(new FileConfigOrigin())
    pipelineConfig.setTrackingTool(new TrackingTool("http://example.com/\${ID}", "##\\d+"))
    pipelineConfig.setDisplayOrderWeight(0)
    def pipeline = new GoDashboardPipeline(pipeline_model('p1', 'pipeline_label'),
            permissions, "grp", counter, pipelineConfig)
    def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

    def json = toObject({ StageRepresenter.toJSON(it, pipeline, stageInstance, username, "pipeline-name", "2") })

    def expectedJson = [
      _links        : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/2/stage2/2']
      ],
      name          : 'stage2',
      counter       : '2',
      status        : StageState.Building,
      can_operate   : false,
      approved_by   : 'go-user',
      approval_type : 'manual',
      scheduled_at  : jsonDate(date),
      allow_only_on_success_of_previous_stage: false,
      previous_stage: [
        _links       : [
          self: [href: 'http://test.host/go/api/stages/pipeline-name/2/stage1/1']
        ],
        name        : 'stage1',
        counter     : '1',
        status      : StageState.Unknown,
        approval_type: 'manual',
        can_operate : false,
        approved_by : null,
        scheduled_at: null,
        allow_only_on_success_of_previous_stage: false
      ]
    ]
    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'renders stages without previous stage with hal representation'() {
    def stageInstance = new StageInstanceModel('stage2', '2', StageResult.Cancelled, new StageIdentifier('pipeline-name', 23, 'stage', '2'))

    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
    pipelineConfig.setOrigin(new FileConfigOrigin())
    pipelineConfig.setTrackingTool(new TrackingTool("http://example.com/\${ID}", "##\\d+"))
    pipelineConfig.setDisplayOrderWeight(0)
    def pipeline = new GoDashboardPipeline(pipeline_model('p1', 'pipeline_label'),
            permissions, "grp", counter, pipelineConfig)
    def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

    def json = toObject({ StageRepresenter.toJSON(it, pipeline, stageInstance, username, "pipeline-name", "23") })

    def expectedJson = [
      _links       : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/23/stage2/2']
      ],
      name         : 'stage2',
      counter      : '2',
      status       : StageState.Unknown,
      approval_type: 'manual',
      approved_by  : null,
      can_operate : false,
      approved_by : null,
      scheduled_at: null,
      allow_only_on_success_of_previous_stage: false
    ]
    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'should render cancelled by if stage is cancelled by user'() {
    def stageInstance = new StageInstanceModel('stage2', '2', new JobHistory().addJob("j1", JobState.Completed, JobResult.Cancelled, new Date(12345)))
    stageInstance.setCancelledBy("foo")

    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
    pipelineConfig.setOrigin(new FileConfigOrigin())
    pipelineConfig.setTrackingTool(new TrackingTool("http://example.com/\${ID}", "##\\d+"))
    pipelineConfig.setDisplayOrderWeight(0)
    def pipeline = new GoDashboardPipeline(pipeline_model('pipeline-name', 'pipeline_label'),
            permissions, "grp", counter, pipelineConfig)
    def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

    def json = toObject({ StageRepresenter.toJSON(it, pipeline, stageInstance, username, "pipeline-name", "23") })
    def expectedJson = [
      _links      : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/23/stage2/2']
      ],
      name         : 'stage2',
      counter      : '2',
      status       : StageState.Cancelled,
      approved_by  : null,
      can_operate : false,
      approval_type: 'manual',
      approved_by : null,
      scheduled_at: "1970-01-01T00:00:12Z",
      cancelled_by: "foo",
      allow_only_on_success_of_previous_stage: false
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'should render cancelled by if stage is cancelled by gocd'() {
    def stageInstance = new StageInstanceModel('stage2', '2', new JobHistory().addJob("j1", JobState.Completed, JobResult.Cancelled, new Date(12345)))
    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
    pipelineConfig.setOrigin(new FileConfigOrigin())
    pipelineConfig.setTrackingTool(new TrackingTool("http://example.com/\${ID}", "##\\d+"))
    pipelineConfig.setDisplayOrderWeight(0)
    def pipeline = new GoDashboardPipeline(pipeline_model('p1', 'pipeline_label'),
            permissions, "grp", counter, pipelineConfig)
    def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

    def json = toObject({ StageRepresenter.toJSON(it, pipeline, stageInstance, username, "pipeline-name", "23") })
    def expectedJson = [
      _links      : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/23/stage2/2']
      ],
      name        : 'stage2',
      counter     : '2',
      status      : StageState.Cancelled,
      can_operate : false,
      approved_by : null,
      approval_type: 'manual',
      scheduled_at: "1970-01-01T00:00:12Z",
      cancelled_by: "GoCD",
      allow_only_on_success_of_previous_stage: false
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }

}
