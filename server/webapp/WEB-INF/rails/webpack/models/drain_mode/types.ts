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

export interface FilterJSON {
  ignore: string[];
}

export interface AttributesJSON {
  name: string;
  url: string;
  mdu_start_time: string;
  type: string;
  auto_update: boolean;
  branch: string;
  destination: string;
  filter: FilterJSON;
  invert_filter: boolean;
  shallow_clone: boolean;
  submodule_folder: string;
}

export interface MaterialJSON {
  attributes: AttributesJSON;
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

export class Filter {
  ignore?: string[];

  constructor(ignore?: string[]) {
    this.ignore = ignore;
  }

  static fromJSON(filter: FilterJSON) {
    return new Filter(filter ? filter.ignore : []);
  }
}

export class Attributes {
  name: string;
  url: string;
  mduStartTime: Date;
  type: string;
  autoUpdate: boolean;
  branch: string;
  destination: string;
  invertFilter: boolean;
  shallowClone: boolean;
  submoduleFolder: string;
  filter: Filter;

  constructor(type: string, name: string, url: string, branch: string, destination: string,
              mduStartTime: Date, autoUpdate: boolean,
              shallowClone: boolean, submoduleFolder: string,
              invertFilter: boolean, filter: Filter) {
    this.name            = name;
    this.url             = url;
    this.mduStartTime    = mduStartTime;
    this.type            = type;
    this.autoUpdate      = autoUpdate;
    this.branch          = branch;
    this.destination     = destination;
    this.filter          = filter;
    this.invertFilter    = invertFilter;
    this.shallowClone    = shallowClone;
    this.submoduleFolder = submoduleFolder;
  }

  static fromJSON(json: AttributesJSON) {
    const mduStartTime = json.mdu_start_time ? TimeFormatter.formatInDate(json.mdu_start_time) : null;

    return new Attributes(json.type,
                          json.name,
                          json.url,
                          json.branch,
                          json.destination,
                          mduStartTime,
                          json.auto_update,
                          json.shallow_clone,
                          json.submodule_folder,
                          json.invert_filter,
                          Filter.fromJSON(json.filter)
    );
  }
}

export class Material {
  attributes: Attributes;

  constructor(attributes: Attributes) {
    this.attributes = attributes;
  }

  static fromJSON(material: MaterialJSON) {
    return new Material(Attributes.fromJSON(material.attributes));
  }

  headerMap() {
    return new Map(
      [
        ["type", this.attributes.type],
        ["name", this.attributes.name],
        ["url", this.attributes.url],
        ["branch", this.attributes.branch],
        ["started At", this.attributes.mduStartTime.toString()]
      ]
    );
  }

  asMap() {
    const map = new Map();
    _.forEach(this.attributes, (value, key) => {
      if (_.isObject(value)) {
        map.set(key, JSON.stringify(value));
      } else {
        map.set(key, value ? value.toString() : null);
      }
    });
    return map;
  }
}

export class RunningSystem {
  jobs: Job[];
  mdu: Material[];

  constructor(jobs: Job[], mdu: Material[]) {
    this.jobs = jobs;
    this.mdu  = mdu;
  }

  static fromJSON(runningSystemJSON: RunningSystemJSON) {
    const jobs = runningSystemJSON.jobs ? runningSystemJSON.jobs.map(Job.fromJSON) : [];
    const mdu  = runningSystemJSON.mdu ? runningSystemJSON.mdu.map(Material.fromJSON) : [];
    return new RunningSystem(jobs, mdu);
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
