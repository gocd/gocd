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
import {timeFormatter} from "helpers/time_formatter";
import _ from "lodash";
import Stream from "mithril/stream";
import {MaterialJSON, Materials} from "models/maintenance_mode/material";

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
  material_update_in_progress?: MaterialJSON[];
  building_jobs?: JobJSON[];
  scheduled_jobs?: JobJSON[];
}

export interface MaintenanceModeMetadataJSON {
  updated_by: string;
  updated_on: string;
}

export interface Attributes {
  has_running_systems?: boolean;
  running_systems?: RunningSystemJSON;
}

export interface MaintenanceModeInfoJSON {
  is_maintenance_mode: boolean;
  metadata: MaintenanceModeMetadataJSON;
  attributes?: Attributes;
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

  toString() {
    return `${this.pipelineName}/${this.pipelineCounter}/${this.stageName}/${this.stageCounter}`;
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
                   timeFormatter.toDate(job.scheduled_date),
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

export class Stage {
  private __isCancelInProgress: boolean = false;
  private jobs: Job[];
  private stageLocator: string;

  constructor(stageLocator: string, jobs: Job[]) {
    this.jobs         = jobs;
    this.stageLocator = stageLocator;
  }

  getStageLocator(): StageLocator {
    return StageLocator.fromStageLocatorString(this.stageLocator);
  }

  getStageLocatorAsString(): string {
    return this.stageLocator;
  }

  getJobs(): Job[] {
    return this.jobs;
  }

  startCancelling(): void {
    this.__isCancelInProgress = true;
  }

  isStageCancelInProgress(): boolean {
    return this.__isCancelInProgress;
  }
}

export class RunningSystem {
  buildingJobsGroupedByStages: Stage[];
  scheduledJobsGroupedByStages: Stage[];
  materialUpdateInProgress: Materials;

  constructor(buildingJobsGroupedByStages: Stage[], scheduledJobsGroupedByStages: Stage[], mdu: Materials) {
    this.buildingJobsGroupedByStages  = buildingJobsGroupedByStages;
    this.scheduledJobsGroupedByStages = scheduledJobsGroupedByStages;
    this.materialUpdateInProgress     = mdu;
  }

  static fromJSON(runningSystemJSON?: RunningSystemJSON) {
    if (!runningSystemJSON) {
      return;
    }
    const buildingJobsGroupedByStages  = RunningSystem.groupJobsByStage(runningSystemJSON!.building_jobs ? runningSystemJSON!.building_jobs!.map(
      Job.fromJSON) : []);
    const scheduledJobsGroupedByStages = RunningSystem.groupJobsByStage(runningSystemJSON!.scheduled_jobs ? runningSystemJSON!.scheduled_jobs!.map(
      Job.fromJSON) : []);
    const materials                    = runningSystemJSON!.material_update_in_progress ? Materials.fromJSON(
      runningSystemJSON!.material_update_in_progress!) : new Materials([]);
    return new RunningSystem(buildingJobsGroupedByStages, scheduledJobsGroupedByStages, materials);
  }

  private static groupJobsByStage(jobs: Job[]) {
    const groupByStage = new Map();
    jobs.forEach((job) => {
      if (!groupByStage.has(job.stageLocatorString())) {
        groupByStage.set(job.stageLocatorString(), []);
      }
      groupByStage.get(job.stageLocatorString()).push(job);
    });

    const stages: Stage[] = [];
    groupByStage.forEach((jobs, stageLocator) => {
      stages.push(new Stage(stageLocator, jobs));
    });

    return stages;
  }
}

export class MaintenanceModeMetadata {
  updatedBy: string;
  updatedOn: string;

  constructor(updatedBy: string, updatedOn: string) {
    this.updatedBy = updatedBy;
    this.updatedOn = timeFormatter.format(updatedOn);
  }

  static fromJSON(json: MaintenanceModeMetadataJSON) {
    return new MaintenanceModeMetadata(json.updated_by, json.updated_on);
  }
}

export class MaintenanceModeInfo {
  public readonly maintenanceModeState: Stream<boolean>;
  public hasRunningSystems?: boolean;
  public metdata: MaintenanceModeMetadata;
  public runningSystem?: RunningSystem;

  constructor(isMaintenanceMode: boolean,
              maintenanceModeMetadata: MaintenanceModeMetadata,
              hasRunningSystems?: boolean,
              runningSystem?: RunningSystem) {
    this.maintenanceModeState = Stream(isMaintenanceMode);
    this.hasRunningSystems    = hasRunningSystems;
    this.metdata              = maintenanceModeMetadata;
    this.runningSystem        = runningSystem;
  }

  static fromJSON(json: MaintenanceModeInfoJSON) {
    json.attributes = json.attributes || {} as Attributes;
    return new MaintenanceModeInfo(json.is_maintenance_mode,
                                   MaintenanceModeMetadata.fromJSON(json.metadata),
                                   json.attributes.has_running_systems,
                                   RunningSystem.fromJSON(json.attributes.running_systems));
  }
}
