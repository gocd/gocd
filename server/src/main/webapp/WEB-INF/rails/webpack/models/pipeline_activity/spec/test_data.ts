/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
      htt://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {HistoryJSON, PipelineActivityJSON, StageJSON} from "../pipeline_activity_json";

export function passed(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Passed");
}

export function failed(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Failed");
}

export function building(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Building");
}

export function cancelled(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Cancelled");
}

export function unknown(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Unknown");
}

export class PipelineActivityData {
  static pipelineRunInfo(...stages: StageJSON[]): HistoryJSON {
    return {
      pipelineId: 42,
      label: "1",
      counterOrLabel: "1",
      scheduled_date: "22 Nov, 2019 at 1:5:59 [+0530]",
      scheduled_timestamp: 1574404139615,
      buildCauseBy: "Triggered by changes",
      modification_date: "about 22 hours ago",
      revision: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
      comment: "Initial commit",
      materialRevisions: [{
        revision: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
        revision_href: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
        user: "Bob <bob@go.cd>",
        date: "2019-11-21T1:3:20+0:30",
        changed: true,
        folder: "",
        scmType: "Git",
        location: "http://github.com/bdpiparva/sample_repo",
        action: "Modified",
        modifications: [{
          user: "Bob <bob@go.cd>",
          revision: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
          date: "2019-11-21T1:3:20+0:30",
          comment: "Adding test file",
          modifiedFiles: []
        }]
      }],
      stages
    } as HistoryJSON;
  }

  static stage(stageId: number, name: string, status: string) {
    return {
      stageName: name,
      stageId,
      stageStatus: status,
      stageLocator: "Foo/0/foo/1",
      getCanRun: true,
      getCanCancel: false,
      scheduled: false,
      stageCounter: 1
    } as StageJSON;
  }

  static underConstruction() {
    return {
      pipelineName: "up42",
      paused: true,
      pauseCause: "Under construction",
      pauseBy: "admin",
      canForce: false,
      nextLabel: "",
      groups: [
        {
          config: {
            stages: [
              {
                name: "foo",
                isAutoApproved: true
              }
            ]
          },
          history: [
            {
              pipelineId: -1,
              label: "unknown",
              counterOrLabel: "0",
              scheduled_date: "N/A",
              scheduled_timestamp: null,
              buildCauseBy: "Triggered by null",
              modification_date: "N/A",
              materialRevisions: [],
              stages: [
                {
                  stageName: "foo",
                  stageId: 0,
                  stageStatus: "Unknown",
                  stageLocator: "Foo/0/foo/1",
                  getCanRun: false,
                  getCanCancel: false,
                  scheduled: false,
                  stageCounter: 1
                }
              ],
              revision: "",
              comment: null
            }
          ]
        }
      ],
      forcedBuild: false,
      showForceBuildButton: false,
      canPause: true,
      count: 0,
      start: 0,
      perPage: 10
    } as PipelineActivityJSON;
  }

  static oneStage() {
    return {
      pipelineName: "up43",
      paused: false,
      pauseCause: "",
      pauseBy: "",
      canForce: true,
      nextLabel: "",
      groups: [{
        config: {
          stages: [{
            name: "up42_stage",
            isAutoApproved: true
          }]
        },
        history: [{
          pipelineId: 42,
          label: "1",
          counterOrLabel: "1",
          scheduled_date: "22 Nov, 2019 at 1:5:59 [+0530]",
          scheduled_timestamp: 1574404139615,
          buildCauseBy: "Triggered by changes",
          modification_date: "about 22 hours ago",
          revision: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
          comment: "Initial commit",
          materialRevisions: [{
            revision: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
            revision_href: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
            user: "Bob <bob@go.cd>",
            date: "2019-11-21T1:3:20+0:30",
            changed: true,
            folder: "",
            scmType: "Git",
            location: "http://github.com/bdpiparva/sample_repo",
            action: "Modified",
            modifications: [{
              user: "Bob <bob@go.cd>",
              revision: "b0982fa2ff92d126ad003c9e007959b4b8dd96a9",
              date: "2019-11-21T1:3:20+0:30",
              comment: "Adding test file",
              modifiedFiles: []
            }]
          }],
          stages: [{
            stageName: "up42_stage",
            stageId: 96,
            stageStatus: "Passed",
            stageLocator: "up43/1/up42_stage/1",
            getCanRun: true,
            getCanCancel: false,
            scheduled: true,
            stageCounter: 1,
            approvedBy: "changes"
          }]
        }]
      }],
      forcedBuild: false,
      showForceBuildButton: false,
      canPause: true,
      count: 1,
      start: 0,
      perPage: 10
    } as PipelineActivityJSON;
  }
}
