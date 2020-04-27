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
import _ from "lodash";
import Stream from "mithril/stream";
import {EnvironmentVariableJSON, EnvironmentVariables} from "models/environment_variables/types";
import {MaterialJSON} from "models/materials/serialization";
import {
  Material,
  MaterialAttributes,
  PluggableScmMaterialAttributes,
  ScmMaterialAttributes
} from "models/materials/types";
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

export class Timer extends ValidatableMixin {
  spec          = Stream<string>();
  onlyOnChanges = Stream<boolean>();

  constructor(spec?: string, onlyOnChanges?: boolean) {
    super();

    this.spec(spec!);
    this.onlyOnChanges(onlyOnChanges!);
  }

  static fromJSON(json: TimerSpecJSON) {
    if (json) {
      return new Timer(json.spec, json.only_on_changes);
    }
    return new Timer();
  }

  toJSON() {
    if (!this.spec()) {
      return null;
    }

    return {
      spec: this.spec(),
      only_on_changes: this.onlyOnChanges()
    };
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

export class TrackingTool extends ValidatableMixin {
  readonly regex      = Stream<string>();
  readonly urlPattern = Stream<string>();

  constructor() {
    super();

    this.validatePresenceOf("regex", {condition: () => !_.isEmpty(this.urlPattern())});
    this.validatePresenceOf("urlPattern", {condition: () => !_.isEmpty(this.regex())});
  }

  static fromJSON(json: TrackingToolJSON) {
    const tracingTool = new TrackingTool();

    if (json) {
      tracingTool.regex(json.attributes.regex);
      tracingTool.urlPattern(json.attributes.url_pattern);
    }

    return tracingTool;
  }

  toJSON(): any {
    if (!this.regex() && !this.urlPattern()) {
      return null;
    }

    return {
      type: "generic",
      attributes: {
        url_pattern: this.urlPattern(),
        regex: this.regex()
      }
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
  readonly materials            = Stream(new Materials());
  readonly stages               = Stream(new NameableSet<Stage>());
  readonly trackingTool         = Stream<TrackingTool>();
  readonly timer                = Stream<Timer>();

  constructor(name: string = "", materials: Material[] = [], stages: Stage[] = []) {
    super();

    this.name = Stream(name);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validatePresenceOf("group");
    this.validateIdFormat("group");

    this.materials = Stream(new Materials(...materials));
    this.validateNonEmptyCollection("materials", {message: `A pipeline must have at least one material`});
    this.validateEach("materials");

    this.stages = Stream(new NameableSet(stages));
    this.validateAssociated("stages");

    this.validateAssociated("trackingTool");

    this.validateChildAttrIsUnique("parameters", "name", {message: "Parameter names must be unique"});
  }

  static get(pipelineName: string) {
    return ApiRequestBuilder.GET(SparkRoutes.adminPipelineConfigPath(pipelineName), ApiVersion.latest);
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
    pipelineConfig.materials(Materials.fromJSONArray(json.materials));
    pipelineConfig.stages(new NameableSet(Stage.fromJSONArray(json.stages || [])));
    pipelineConfig.trackingTool(TrackingTool.fromJSON(json.tracking_tool));
    pipelineConfig.timer(Timer.fromJSON(json.timer));

    return pipelineConfig;
  }

  firstStage(): Stage {
    return this.stages().values().next().value;
  }

  isUsingTemplate() {
    return !!this.template();
  }

  withGroup(group: string) {
    this.group(group);
    return this;
  }

  isDefinedInConfigRepo() {
    if (!this.origin()) {
      return false;
    }

    return this.origin().isDefinedInConfigRepo();
  }

  create(pause: boolean) {
    return ApiRequestBuilder.POST(SparkRoutes.adminPipelineConfigPath(), ApiVersion.latest, {
      payload: this.toApiPayload(),
      headers: {"X-pause-pipeline": pause.toString(), "X-pause-cause": "Under construction"}
    });
  }

  update(etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.adminPipelineConfigPath(this.name()), ApiVersion.latest, {
      payload: this.toPutApiPayload(), etag
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

  toPutApiPayload(): any {
    const json = JsonUtils.toSnakeCasedObject(this);
    if (_.isEmpty(json.label_template)) {
      delete json.label_template;
    }

    return json;
  }
}

export class Materials extends Array<Material> {
  constructor(...vals: Material[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(Materials.prototype));
  }

  static fromJSON(material: MaterialJSON): Material {
    return new Material(material.type, MaterialAttributes.deserialize(material));
  }

  static fromJSONArray(data: MaterialJSON[]): Materials {
    return new Materials(...data.map((a) => Materials.fromJSON(a)));
  }

  delete(material: Material) {
    const index = this.indexOf(material, 0);
    if (index > -1) {
      this.splice(index, 1);
    }
  }

  scmMaterialsHaveDestination(): boolean {
    return !this
      .some((material) => {
        if (material.attributes() instanceof ScmMaterialAttributes) {
          const attrs = material.attributes() as ScmMaterialAttributes;
          return _.isEmpty(attrs.destination());
        } else if (material.attributes() instanceof PluggableScmMaterialAttributes) {
          const attrs = material.attributes() as PluggableScmMaterialAttributes;
          return _.isEmpty(attrs.destination());
        } else {
          return false;
        }
      });
  }
}
