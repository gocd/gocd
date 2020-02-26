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

import {StageJSON} from "models/pipeline_configs/stage";
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
  stages: StageJSON[];
}

export type TaskType = "pluggable_task" | "fetch" | "ant" | "exec" | "nant" | "rake";

export interface TaskJSON {
  type: TaskType;
  attributes: AntTaskAttributesJSON | NAntTaskAttributesJSON | RakeTaskAttributesJSON | ExecTaskAttributesJSON | FetchTaskAttributesJSON | PluginTaskAttributesJSON;
}

export interface BaseTaskAttributesJSON {
  run_if: (RunIfCondition)[];
  on_cancel?: TaskJSON;
}

export interface WorkingDirAttributesJSON {
  working_directory?: string;
}

export interface BuildFileAndTargetBasedTaskAttributes extends WorkingDirAttributesJSON {
  build_file?: string;
  target?: string;
}

export type AntTaskAttributesJSON = BaseTaskAttributesJSON & BuildFileAndTargetBasedTaskAttributes ;

export interface NAntTaskAttributesJSON extends BaseTaskAttributesJSON, BuildFileAndTargetBasedTaskAttributes {
  nant_path?: string;
}

export type RakeTaskAttributesJSON = BaseTaskAttributesJSON & BuildFileAndTargetBasedTaskAttributes ;

export interface ExecTaskAttributesJSON extends BaseTaskAttributesJSON, WorkingDirAttributesJSON {
  command: string;

  // either of these can be present
  args?: string;
  arguments?: string[];
}

export interface PluginConfiguration {
  id: string;
  version: string;
}

export type ArtifactOrigin = "external" | "gocd";

export interface FetchTaskAttributesJSON extends BaseTaskAttributesJSON {
  artifact_origin: ArtifactOrigin;
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

export interface PluginTaskAttributesJSON extends BaseTaskAttributesJSON {
  plugin_configuration: PluginConfiguration;
  configuration: PropertyJSON[];
}
