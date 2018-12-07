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
import {MaterialJSON, Materials} from "models/drain_mode/material";

const TimeFormatter = require("helpers/time_formatter");

export interface JobJSON {
  name: string;
  pipeline_counter: number;
  pipeline_name: string;
  scheduled_date: string;
  stage_counter: number;
  stage_name: string;
  state: string;
  agent_uuid?: string;
}

export interface RunningSystemJSON {
  mdu: MaterialJSON[];
  jobs: JobJSON[];
}

export interface EmbeddedJSON {
  is_completely_drained: boolean;
  running_systems: RunningSystemJSON;
}

export interface DrainModeInfoJSON {
  _embedded: EmbeddedJSON;
}

export class Job {
  jobName: string;
  pipelineCounter: number;
  pipelineName: string;
  stageCounter: number;
  stageName: string;
  scheduledDate: Date;
  state?: string;
  agentUUID?: string;

  constructor(name: string, pipelineCounter: number, pipelineName: string,
              stageCounter: number, stageName: string,
              scheduledDate: Date, state?: string,
              agentUUID?: string) {
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
                   TimeFormatter.formatInDate(job.scheduled_date),
                   job.state,
                   job.agent_uuid);
  }

  stageLocator() {
    return _.join([this.pipelineName, this.pipelineCounter, this.stageName, this.stageCounter], "/");
  }

  locator() {
    return _.join([this.stageLocator(), this.jobName], "/");
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
    const jobs = runningSystemJSON.jobs ? runningSystemJSON.jobs.map(Job.fromJSON) : [];
    return new RunningSystem(jobs, Materials.fromJSON(runningSystemJSON.mdu));
  }

  groupJobsByStage() {
    const groupByStage = new Map();
    this.jobs.forEach((job) => {
      if (!groupByStage.has(job.stageLocator())) {
        groupByStage.set(job.stageLocator(), []);
      }
      groupByStage.get(job.stageLocator()).push(job);
    });
    return groupByStage;
  }
}

export class DrainModeInfo {
  isCompletelyDrained: boolean;
  runningSystem: RunningSystem;

  constructor(isCompletelyDrained: boolean, runningSystem: RunningSystem) {
    this.isCompletelyDrained = isCompletelyDrained;
    this.runningSystem       = runningSystem;
  }

  static fromJSON(json: DrainModeInfoJSON) {
    return new DrainModeInfo(json._embedded.is_completely_drained,
                             RunningSystem.fromJSON(json._embedded.running_systems));
  }
}
