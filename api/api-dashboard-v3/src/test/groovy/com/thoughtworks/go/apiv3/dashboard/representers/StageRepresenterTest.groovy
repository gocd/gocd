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
package com.thoughtworks.go.apiv3.dashboard.representers

import com.thoughtworks.go.domain.*
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

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

    def json = toObject({ StageRepresenter.toJSON(it, stageInstance, "pipeline-name", "2") })

    def expectedJson = [
      _links        : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/2/stage2/2']
      ],
      name          : 'stage2',
      counter       : '2',
      status        : StageState.Building,
      approved_by   : 'go-user',
      scheduled_at  : jsonDate(date),
      previous_stage: [
        _links      : [
          self: [href: 'http://test.host/go/api/stages/pipeline-name/2/stage1/1']
        ],
        name        : 'stage1',
        counter     : '1',
        status      : StageState.Unknown,
        approved_by : null,
        scheduled_at: null,
      ]
    ]
    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'renders stages without previous stage with hal representation'() {
    def stageInstance = new StageInstanceModel('stage2', '2', StageResult.Cancelled, new StageIdentifier('pipeline-name', 23, 'stage', '2'))

    def json = toObject({ StageRepresenter.toJSON(it, stageInstance, "pipeline-name", "23") })

    def expectedJson = [
      _links      : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/23/stage2/2']
      ],
      name        : 'stage2',
      counter     : '2',
      status      : StageState.Unknown,
      approved_by : null,
      scheduled_at: null
    ]
    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'should render cancelled by if stage is cancelled by user'() {
    def stageInstance = new StageInstanceModel('stage2', '2', new JobHistory().addJob("j1", JobState.Completed, JobResult.Cancelled, new Date(12345)))
    stageInstance.setCancelledBy("foo");

    def json = toObject({ StageRepresenter.toJSON(it, stageInstance, "pipeline-name", "23") })
    def expectedJson = [
      _links      : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/23/stage2/2']
      ],
      name        : 'stage2',
      counter     : '2',
      status      : StageState.Cancelled,
      approved_by : null,
      scheduled_at: "1970-01-01T00:00:12Z",
      cancelled_by: "foo"
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }

  @Test
  void 'should render cancelled by if stage is cancelled by gocd'() {
    def stageInstance = new StageInstanceModel('stage2', '2', new JobHistory().addJob("j1", JobState.Completed, JobResult.Cancelled, new Date(12345)))

    def json = toObject({ StageRepresenter.toJSON(it, stageInstance, "pipeline-name", "23") })
    def expectedJson = [
      _links      : [
        self: [href: 'http://test.host/go/api/stages/pipeline-name/23/stage2/2']
      ],
      name        : 'stage2',
      counter     : '2',
      status      : StageState.Cancelled,
      approved_by : null,
      scheduled_at: "1970-01-01T00:00:12Z",
      cancelled_by: "GoCD"
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }

}
