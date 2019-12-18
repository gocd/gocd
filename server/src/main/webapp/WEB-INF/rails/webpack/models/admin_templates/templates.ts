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

import {EnvironmentVariableJSON} from "models/environment_variables/types";
import {RunIfCondition} from "models/pipeline_configs/task";
import {PropertyJSON} from "models/shared/configuration";

export namespace TemplateSummary {

  export interface TemplateSummaryRootObject {
    _embedded: { templates: TemplateSummaryTemplate[] };
  }

  export interface TemplateSummaryTemplate {
    name: string;
    can_edit: boolean;
    can_administer: boolean;
    _embedded: { pipelines: TemplateSummaryPipeline[] };
  }

  export interface TemplateSummaryPipeline {
    name: string;
    can_administer: boolean;
  }
}

export interface Template {
  name: string;
  stages: Stage[];
}

export interface Stage {
  name: string;
  fetch_materials: boolean;
  clean_working_directory: boolean;
  never_cleanup_artifacts: boolean;
  approval: Approval;
  environment_variables: EnvironmentVariableJSON[];
  jobs: Job[];
}

export interface Approval {
  type: "manual" | "success";
  allow_only_on_success?: boolean;
  authorization: Authorization;
}

export interface Authorization {
  roles: string[];
  users: string[];
}

export interface Job {
  name: string;
  run_instance_count?: number | "all" | null;
  timeout?: "never" | number | null;
  elastic_profile_id?: string;
  environment_variables: EnvironmentVariableJSON[];
  resources: string[];
  tasks: Task[];
  tabs: Tab[];
  artifacts: Artifact[];
}

export interface Artifact {
  type: "test" | "build" | "external";

  // for `test` or `build` type
  source?: string;
  destination?: string;

  // for `external`
  artifact_id?: string;
  store_id?: string;
  configuration?: PropertyJSON[];
}

export interface Tab {
  name: string;
  path: string;
}

export interface Task {
  type: "pluggable_task" | "fetch" | "ant" | "exec" | "nant" | "rake";
  attributes: AntTaskAttributes | NAntTaskAttributes | RakeTaskAttributes | ExecTaskAttributes | FetchTaskAttributes | PluginTaskAttributes;
}

export interface BaseTaskAttributes {
  run_if: (RunIfCondition)[];
  on_cancel?: Task;
}

export interface WorkingDirAttributes {
  working_directory?: string;
}

export interface BuildFileAndTargetBasedTaskAttributes extends WorkingDirAttributes {
  build_file?: string;
  target?: string;
}

export type AntTaskAttributes = BaseTaskAttributes & BuildFileAndTargetBasedTaskAttributes ;

export interface NAntTaskAttributes extends BaseTaskAttributes, BuildFileAndTargetBasedTaskAttributes {
  nant_path?: string;
}

export type RakeTaskAttributes = BaseTaskAttributes & BuildFileAndTargetBasedTaskAttributes ;

export interface ExecTaskAttributes extends BaseTaskAttributes, WorkingDirAttributes {

  command: string;

  // either of these can be present
  args?: string;
  arguments?: string[];
}

export interface PluginConfiguration {
  id: string;
  version: string;
}

export interface FetchTaskAttributes extends BaseTaskAttributes {
  artifact_origin: "external" | "gocd";
  pipeline?: string;
  stage: string;
  job: string;

  // for regular fetch
  is_source_a_file?: boolean;
  source?: string;
  destination?: string;

  // for external fetch
  artifact_id?: string;
  configuration?: PropertyJSON[];
}

export interface PluginTaskAttributes extends BaseTaskAttributes {
  plugin_configuration: PluginConfiguration;
  configuration: PropertyJSON[];
}
