/*
 * Copyright 2019 ThoughtWorks, Inc.
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

export interface PipelineActivityJSON {
  pipelineName: string;
  paused: boolean;
  pauseCause: string;
  pauseBy: string;
  canForce: boolean;
  nextLabel: string;
  groups: GroupJSON[];
  forcedBuild: boolean;
  showForceBuildButton: boolean;
  canPause: boolean;
  count: number;
  start: number;
  perPage: number;
}

export interface StageConfigJSON {
  name: string;
  isAutoApproved: boolean;
}

export interface ConfigJSON {
  stages: StageConfigJSON[];
}

export interface ModificationJSON {
  user: string;
  revision: string;
  date: string;
  comment: string;
  modifiedFiles: any[];
}

export interface MaterialRevisionJSON {
  revision: string;
  revision_href: string;
  user: string;
  date: string;
  changed: boolean;
  modifications: ModificationJSON[];
  folder: string;
  scmType: string;
  location: string;
  action: string;
}

export interface StageJSON {
  stageName: string;
  stageId: number;
  stageStatus: string;
  stageLocator: string;
  getCanRun: boolean;
  getCanCancel: boolean;
  scheduled: boolean;
  stageCounter: number;
  approvedBy: string;
}

export interface HistoryJSON {
  pipelineId: number;
  label: string;
  counterOrLabel: string;
  scheduled_date: string;
  scheduled_timestamp: number;
  buildCauseBy: string;
  modification_date: string;
  materialRevisions: MaterialRevisionJSON[];
  stages: StageJSON[];
  revision: string;
  comment?: string;
}

export interface GroupJSON {
  config: ConfigJSON;
  history: HistoryJSON[];
}
