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

export type stringOrNull = string | null;
export type stringOrUndefined = string | undefined;
export type dateOrUndefined = Date | undefined;

interface LinkJSON {
  href: string;
}

export interface PipelineHistoryJSON {
  _links?: {
    next?: LinkJSON;
    previous?: LinkJSON;
  };
  pipelines: PipelineInstanceJSON[];
}

export interface PipelineInstanceJSON {
  id: number;
  name: string;
  counter: number;
  label: string;
  natural_order: number;
  can_run: boolean;
  preparing_to_schedule: boolean;
  scheduled_date: number;
  comment: stringOrNull;
  build_cause: BuildCauseJSON;
  stages: StageJSON[];
}

export interface BuildCauseJSON {
  trigger_message: string;
  trigger_forced: boolean;
  approver: string;
  material_revisions: MaterialRevisionJSON[];
}

export interface MaterialRevisionJSON {
  changed: boolean;
  material: MaterialJSON;
  modifications: ModificationJSON[];
}

export interface MaterialJSON {
  id: number;
  name: string;
  fingerprint: string;
  type: string;
  description: string;
}

export interface ModificationJSON {
  id: number;
  revision: string;
  modified_time: string;
  user_name: string;
  comment: stringOrNull;
  email_address: stringOrNull;
  pipeline_label: stringOrNull;
}

export interface StageJSON {
  result: string;
  status: string;
  id: number;
  name: string;
  counter: string;
  scheduled: boolean;
  approval_type: string;
  approved_by: string;
  operate_permission: boolean;
  can_run: boolean;
  jobs: JobJSON[];
}

export interface JobJSON {
  id: number;
  name: string;
  scheduled_date: number;
  state: string;
  result: string;
}
