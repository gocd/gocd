/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {
  HistoryJSON,
  MaterialRevisionJSON,
  ModificationJSON,
  PipelineActivityJSON,
  StageJSON
} from "../pipeline_activity_json";

export function passed(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Passed");
}

export function failed(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Failed");
}

export function building(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Building");
}

export function failing(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Failing");
}

export function cancelled(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Cancelled");
}

export function unknown(name: string, stageId: number = Math.random()) {
  return PipelineActivityData.stage(stageId, name, "Unknown");
}

export class PipelineActivityData {
  static modification(revision: string = this.randomRevision()) {
    return {
      user: "Bob <bob@go.cd>",
      revision,
      date: "2019-11-21T1:3:20+0:30",
      comment: "Adding test file",
      modifiedFiles: []
    } as ModificationJSON;
  }

  static materialRevision(modifications: ModificationJSON[] = [this.modification(), this.modification()]) {
    return {
      revision: modifications[0].revision,
      revision_href: modifications[0].revision,
      user: modifications[0].user,
      date: modifications[0].date,
      changed: true,
      folder: "",
      scmType: "Git",
      location: "http://github.com/bdpiparva/sample_repo",
      action: "Modified",
      modifications
    } as MaterialRevisionJSON;
  }

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
      materialRevisions: [this.materialRevision(), this.materialRevision()],
      stages
    } as HistoryJSON;
  }

  static stage(stageId: number,
               name: string,
               status: string,
               stageCounter    = 1,
               pipelineName    = "Foo",
               pipelineCounter = "0") {
    return {
      stageName: name,
      stageId,
      stageStatus: status,
      stageLocator: `${pipelineName}/${pipelineCounter}/${name}/${stageCounter}`,
      getCanRun: true,
      getCanCancel: false,
      scheduled: false,
      stageCounter
    } as StageJSON;
  }

  static withStages(pipelineName: string, ...stages: StageJSON[]) {
    return {
      pipelineName,
      paused: false,
      pauseCause: "",
      pauseBy: "",
      canForce: true,
      nextLabel: "",
      groups: [{
        config: {
          stages: stages.map((stage) => {
            return {name: stage.stageName, isAutoApproved: true};
          }),
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
          comment: "",
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

  private static randomRevision() {
    return [
      "3b5c3810db30d1c98bdb18fb2fe91aabf593a",
      "501d66d5e442fd5e324e7ecf3e6627048abf1",
      "71f39e9b97063728be0f392ff5ba0e9df69e0",
      "445f86e9d0c080d72c7c8c5138408fe8f5acb",
      "a999249c08041ce2eedbb8bedae54e94684b1",
      "9d9f4dcd9a50c036cbcfb68b7c872feecb253",
      "5fc07bdda3a2aa151d76d75d7df9a32e440b1",
      "f53e09dc678d3a97c2b23e4b19a57bc67b489"]
      [this.getRandomIntegerInRange(0, 7)] + this.getRandomIntegerInRange(100, 999);
  }

  private static getRandomIntegerInRange(min: number, max: number) {
    return Math.ceil(Math.random() * (max - min) + min);
  }
}
