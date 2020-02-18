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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {JsonUtils} from "helpers/json_utils";
import {SparkRoutes} from "helpers/spark_routes";
import Stream from "mithril/stream";
import {EnvironmentVariableJSON, EnvironmentVariables} from "models/environment_variables/types";
import {MaterialJSON} from "models/materials/serialization";
import {Material, Materials} from "models/materials/types";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Origin, OriginJSON} from "models/origin";
import {NameableSet} from "./nameable_set";
import {ParameterJSON, PipelineParameter} from "./parameter";
import {Stage, StageJSON} from "./stage";

export type LockBehavior = "lockOnFailure" | "unlockWhenFinished" | "none";

interface TimerSpecJSON {
  spec: string;
  only_on_changes: boolean;
}

export interface PipelineConfigJSON {
  label_template: string;
  lock_behavior: LockBehavior;
  name: string;
  template: string;
  group: string;
  origin: OriginJSON;
  parameters: ParameterJSON[];
  environment_variables: EnvironmentVariableJSON[];
  materials: MaterialJSON[];
  stages: StageJSON[];
  tracking_tool: TrackingToolJSON;
  timer: TimerSpecJSON;
}

export interface Artifact {
  type: string;
  source: string;
  destination: string;
}

export interface Task {
  type: string;
  attributes: TaskAttributesJSON;
}

export interface TaskAttributesJSON {
  artifact_origin?: string;
  pipeline?: string;
  stage?: string;
  job?: string;
  run_if: string[];
  is_source_a_file?: boolean;
  source?: string;
  destination?: string;
  command?: string;
  arguments?: string[];
  working_directory?: string;
}

export class Timer {
  spec          = Stream<string>();
  onlyOnChanges = Stream<boolean>();

  constructor(spec?: string, onlyOnChanges?: boolean) {
    this.spec(spec!);
    this.onlyOnChanges(onlyOnChanges!);
  }

  static fromJSON(json: TimerSpecJSON) {
    if (json) {
      return new Timer(json.spec, json.only_on_changes);
    }
    return new Timer();
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }
}

export interface TrackingToolJSON {
  type: "generic";
  attributes: TrackingToolAttributesJSON;
}

export interface TrackingToolAttributesJSON {
  url_pattern: string;
  regex: string;
}

export class TrackingTool {
  readonly regex      = Stream<string>();
  readonly urlPattern = Stream<string>();

  static fromJSON(json: TrackingToolJSON) {
    const tracingTool = new TrackingTool();
    if (json) {
      tracingTool.regex(json.attributes.regex);
      tracingTool.urlPattern(json.attributes.url_pattern);
    }
    return tracingTool;
  }

  toApiPayload(): any {
    return {
      type: "generic",
      attributes: JsonUtils.toSnakeCasedObject(this)
    };
  }
}

export class PipelineConfig extends ValidatableMixin {
  readonly labelTemplate        = Stream<string>();
  readonly lockBehavior         = Stream<LockBehavior>();
  readonly name                 = Stream<string>();
  readonly template             = Stream<string | undefined>();
  readonly group                = Stream<string>();
  readonly origin               = Stream<Origin>();
  readonly parameters           = Stream<PipelineParameter[]>([]);
  readonly environmentVariables = Stream<EnvironmentVariables>();
  readonly materials            = Stream(new NameableSet<Material>());
  readonly stages               = Stream(new NameableSet<Stage>());
  readonly trackingTool         = Stream<TrackingTool>();
  readonly timer                = Stream<Timer>();

  private readonly __usingTemplate = Stream<boolean>(false);

  constructor(name: string = "", materials: Material[] = [], stages: Stage[] = []) {
    super();

    this.name = Stream(name);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validatePresenceOf("group");
    this.validateIdFormat("group");

    this.materials = Stream(new NameableSet(materials));
    this.validateNonEmptyCollection("materials", {message: `A pipeline must have at least one material`});
    this.validateAssociated("materials");

    this.stages = Stream(new NameableSet(stages));
    this.validateAssociated("stages");

    this.validateMutualExclusivityOf("template",
                                     "stages",
                                     {message: "Pipeline stages must not be defined when using a pipeline template"});
    this.validateChildAttrIsUnique("parameters", "name", {message: "Parameter names must be unique"});
  }

  static get(pipelineName: string) {
    return ApiRequestBuilder.GET(SparkRoutes.getOrUpdatePipelineConfigPath(pipelineName), ApiVersion.latest);
  }

  static fromJSON(json: PipelineConfigJSON) {
    const pipelineConfig = new PipelineConfig();
    pipelineConfig.labelTemplate(json.label_template);
    pipelineConfig.lockBehavior(json.lock_behavior || "none");
    pipelineConfig.name(json.name);
    pipelineConfig.template(json.template);
    pipelineConfig.group(json.group);
    pipelineConfig.origin(Origin.fromJSON(json.origin));
    pipelineConfig.parameters(PipelineParameter.fromJSONArray(json.parameters || []));
    pipelineConfig.environmentVariables(EnvironmentVariables.fromJSON(json.environment_variables || []));
    pipelineConfig.materials(new NameableSet<Material>(Materials.fromJSONArray(json.materials || [])));
    pipelineConfig.stages(new NameableSet(Stage.fromJSONArray(json.stages || [])));
    pipelineConfig.trackingTool(TrackingTool.fromJSON(json.tracking_tool));
    pipelineConfig.timer(Timer.fromJSON(json.timer));
    pipelineConfig.__usingTemplate(!!json.template);

    return pipelineConfig;
  }

  firstStage(): Stage {
    return this.stages().values().next().value;
  }

  isUsingTemplate() {
    return this.__usingTemplate;
  }

  withGroup(group: string) {
    this.group(group);
    return this;
  }

  create(pause: boolean) {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineConfigCreatePath(), ApiVersion.latest, {
      payload: this.toApiPayload(),
      headers: {"X-pause-pipeline": pause.toString(), "X-pause-cause": "Under construction"}
    });
  }

  update() {
    return ApiRequestBuilder.PUT(SparkRoutes.getOrUpdatePipelineConfigPath(this.name()), ApiVersion.latest, {
      payload: JsonUtils.toSnakeCasedObject(this),
    });
  }

  run() {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineTriggerPath(this.name()), ApiVersion.latest);
  }

  toApiPayload(): any {
    const raw   = JsonUtils.toSnakeCasedObject(this);
    const group = raw.group;
    delete raw.group;

    return {group, pipeline: raw};
  }
}
