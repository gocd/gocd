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
package com.thoughtworks.go.apiv2.stageinstance.representers

import com.thoughtworks.go.domain.JobResult
import com.thoughtworks.go.domain.JobState
import com.thoughtworks.go.domain.PipelineRunIdInfo
import com.thoughtworks.go.domain.StageIdentifier
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory
import com.thoughtworks.go.presentation.pipelinehistory.JobHistoryItem
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class StageInstancesRepresenterTest {
  @Test
  void 'should represent stage history with hal'() {
    def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(12345))
    jobHistoryItem.setId(34)
    def jobHistory = new JobHistory()
    jobHistory.add(jobHistoryItem)
    def identifier = new StageIdentifier("pipeline", 2, "stage", "4")
    def stageInstanceModel = new StageInstanceModel("stage", "3", jobHistory, identifier)
    stageInstanceModel.setId(21)
    def stageInstances = new StageInstanceModels()
    stageInstances.add(stageInstanceModel)
    def runIdInfo = new PipelineRunIdInfo(50, 1)

    def actualJson = toObjectString({ StageInstancesRepresenter.toJSON(it, stageInstances, runIdInfo) })

    def expectedJson = [
      "_links": [
        "previous": [
          "href": "http://test.host/go/api/stages/pipeline/stage/history?before=21"
        ],
        "next"    : [
          "href": "http://test.host/go/api/stages/pipeline/stage/history?after=21"
        ]
      ],
      "stages": [
        [
          "name"            : "stage",
          "counter"         : "3",
          "approval_type"   : null,
          "approved_by"     : null,
          "rerun_of_counter": null,
          "pipeline_name"   : "pipeline",
          "pipeline_counter": 2,
          "jobs"            : [
            [
              "name"          : "job",
              "state"         : "Completed",
              "result"        : "Passed",
              "scheduled_date": 12345
            ]
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add previous link if there are no newer records'() {
    def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(12345))
    jobHistoryItem.setId(34)
    def jobHistory = new JobHistory()
    jobHistory.add(jobHistoryItem)
    def identifier = new StageIdentifier("pipeline", 2, "stage", "4")
    def stageInstanceModel = new StageInstanceModel("stage", "3", jobHistory, identifier)
    stageInstanceModel.setId(21)
    def stageInstances = new StageInstanceModels()
    stageInstances.add(stageInstanceModel)
    def runIdInfo = new PipelineRunIdInfo(21, 1)

    def actualJson = toObjectString({ StageInstancesRepresenter.toJSON(it, stageInstances, runIdInfo) })

    def expectedJson = [
      "_links": [
        "next": [
          "href": "http://test.host/go/api/stages/pipeline/stage/history?after=21"
        ]
      ],
      "stages": [
        [
          "name"            : "stage",
          "counter"         : "3",
          "approval_type"   : null,
          "approved_by"     : null,
          "rerun_of_counter": null,
          "pipeline_name"   : "pipeline",
          "pipeline_counter": 2,
          "jobs"            : [
            [
              "name"          : "job",
              "state"         : "Completed",
              "result"        : "Passed",
              "scheduled_date": 12345
            ]
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add next link if there are no older records'() {
    def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(12345))
    jobHistoryItem.setId(34)
    def jobHistory = new JobHistory()
    jobHistory.add(jobHistoryItem)
    def identifier = new StageIdentifier("pipeline", 2, "stage", "4")
    def stageInstanceModel = new StageInstanceModel("stage", "3", jobHistory, identifier)
    stageInstanceModel.setId(21)
    def stageInstances = new StageInstanceModels()
    stageInstances.add(stageInstanceModel)
    def runIdInfo = new PipelineRunIdInfo(44, 21)

    def actualJson = toObjectString({ StageInstancesRepresenter.toJSON(it, stageInstances, runIdInfo) })

    def expectedJson = [
      "_links": [
        "previous": [
          "href": "http://test.host/go/api/stages/pipeline/stage/history?before=21"
        ]
      ],
      "stages": [
        [
          "name"            : "stage",
          "counter"         : "3",
          "approval_type"   : null,
          "approved_by"     : null,
          "rerun_of_counter": null,
          "pipeline_name"   : "pipeline",
          "pipeline_counter": 2,
          "jobs"            : [
            [
              "name"          : "job",
              "state"         : "Completed",
              "result"        : "Passed",
              "scheduled_date": 12345
            ]
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add links if only one page is present'() {
    def jobHistoryItem = new JobHistoryItem("job", JobState.Completed, JobResult.Passed, new Date(12345))
    jobHistoryItem.setId(34)
    def jobHistory = new JobHistory()
    jobHistory.add(jobHistoryItem)
    def identifier = new StageIdentifier("pipeline", 2, "stage", "4")
    def stageInstanceModel = new StageInstanceModel("stage", "3", jobHistory, identifier)
    stageInstanceModel.setId(21)
    def stageInstances = new StageInstanceModels()
    stageInstances.add(stageInstanceModel)
    def runIdInfo = new PipelineRunIdInfo(21, 21)

    def actualJson = toObjectString({ StageInstancesRepresenter.toJSON(it, stageInstances, runIdInfo) })

    def expectedJson = [
      "stages": [
        [
          "name"            : "stage",
          "counter"         : "3",
          "approval_type"   : null,
          "approved_by"     : null,
          "rerun_of_counter": null,
          "pipeline_name"   : "pipeline",
          "pipeline_counter": 2,
          "jobs"            : [
            [
              "name"          : "job",
              "state"         : "Completed",
              "result"        : "Passed",
              "scheduled_date": 12345
            ]
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
