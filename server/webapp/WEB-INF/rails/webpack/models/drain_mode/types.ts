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

import * as _ from "lodash";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {MaterialJSON, Materials} from "models/drain_mode/material";

const TimeFormatter = require("helpers/time_formatter");

export interface JobJSON {
  name: string;
  pipeline_counter: number;
  pipeline_name: string;
  scheduled_date: string;
  stage_counter: string;
  stage_name: string;
  state: string;
  agent_uuid?: string | null;
}

export interface RunningSystemJSON {
  mdu?: MaterialJSON[];
  jobs?: JobJSON[];
}

export interface DrainModeMetadataJSON {
  updated_by: string;
  updated_on: string;
}

export interface EmbeddedJSON {
  is_drain_mode: boolean;
  is_completely_drained: boolean;
  metadata: DrainModeMetadataJSON;
  running_systems: RunningSystemJSON;
}

export interface DrainModeInfoJSON {
  _embedded: EmbeddedJSON;
}

export class StageLocator {
  pipelineName: string;
  pipelineCounter: string;
  stageCounter: string;
  stageName: string;

  constructor(pipelineName: string, pipelineCounter: string,
              stageName: string, stageCounter: string) {
    this.pipelineName    = pipelineName;
    this.pipelineCounter = pipelineCounter;
    this.stageName       = stageName;
    this.stageCounter    = stageCounter;
  }

  static fromStageLocatorString(locator: string) {
    const parts = locator.split("/");
    return new StageLocator(parts[0], parts[1], parts[2], parts[3]);
  }

  asMap() {
    return new Map([
                     ["Pipeline Name", this.pipelineName],
                     ["Pipeline Counter", this.pipelineCounter],
                     ["Stage Name", this.stageName],
                     ["Stage Counter", this.stageCounter]
                   ]);
  }
}

export class Job {
  jobName: string;
  pipelineCounter: number;
  pipelineName: string;
  stageCounter: string;
  stageName: string;
  scheduledDate: Date;
  state?: string;
  agentUUID?: string | null;

  constructor(name: string, pipelineCounter: number, pipelineName: string,
              stageCounter: string, stageName: string,
              scheduledDate: Date, state?: string,
              agentUUID?: string | null) {
    this.jobName         = name;
    this.pipelineCounter = pipelineCounter;
    this.pipelineName    = pipelineName;
    this.scheduledDate   = scheduledDate;
    this.stageCounter    = stageCounter;
    this.stageName       = stageName;
    this.state           = state;
    this.agentUUID       = agentUUID;
  }

  static fromJSON(job: JobJSON) {
    return new Job(job.name,
                   job.pipeline_counter,
                   job.pipeline_name,
                   job.stage_counter,
                   job.stage_name,
                   TimeFormatter.format(job.scheduled_date),
                   job.state,
                   job.agent_uuid);
  }

  stageLocatorString() {
    return _.join([this.pipelineName, this.pipelineCounter, this.stageName, this.stageCounter], "/");
  }

  locator() {
    return _.join([this.stageLocatorString(), this.jobName], "/");
  }
}

export class RunningSystem {
  jobs: Job[];
  mdu: Materials;

  constructor(jobs: Job[], mdu: Materials) {
    this.jobs = jobs;
    this.mdu  = mdu;
  }

  static fromJSON(runningSystemJSON: RunningSystemJSON) {
    const jobs      = runningSystemJSON.jobs ? runningSystemJSON.jobs.map(Job.fromJSON) : [];
    const materials = runningSystemJSON.mdu ? Materials.fromJSON(runningSystemJSON.mdu) : new Materials([]);
    return new RunningSystem(jobs, materials);
  }

  groupJobsByStage() {
    const groupByStage = new Map();
    this.jobs.forEach((job) => {
      if (!groupByStage.has(job.stageLocatorString())) {
        groupByStage.set(job.stageLocatorString(), []);
      }
      groupByStage.get(job.stageLocatorString()).push(job);
    });
    return groupByStage;
  }
}

export class DrainModeMetadata {
  updatedBy: string;
  updatedOn: Date;

  constructor(updatedBy: string, updatedOn: string) {
    this.updatedBy = updatedBy;
    this.updatedOn = TimeFormatter.formatInDate(updatedOn);
  }

  static fromJSON(json: DrainModeMetadataJSON) {
    return new DrainModeMetadata(json.updated_by, json.updated_on);
  }
}

export class DrainModeInfo {
  public readonly drainModeState: Stream<boolean>;
  public isCompletelyDrained: boolean;
  public metdata: DrainModeMetadata;
  public runningSystem: RunningSystem;

  constructor(isDrainMode: boolean, isCompletelyDrained: boolean,
              drainModeMetadata: DrainModeMetadata, runningSystem: RunningSystem) {
    this.drainModeState      = stream(isDrainMode);
    this.isCompletelyDrained = isCompletelyDrained;
    this.metdata             = drainModeMetadata;
    this.runningSystem       = runningSystem;
  }

  static fromJSON(json: DrainModeInfoJSON) {
    return new DrainModeInfo(json._embedded.is_drain_mode,
                             json._embedded.is_completely_drained,
                             DrainModeMetadata.fromJSON(json._embedded.metadata),
                             RunningSystem.fromJSON(json._embedded.running_systems));
  }
}
