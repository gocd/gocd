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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {JsonUtils} from "helpers/json_utils";
import {SparkRoutes} from "helpers/spark_routes";
import Stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {EnvironmentVariableConfig} from "models/new_pipeline_configs/environment_variable_config";
import {Parameter} from "models/new_pipeline_configs/parameter";
import {Material, Materials} from "./materials";
import {NameableSet} from "./nameable_set";
import {StageConfig} from "./stage_configuration";

export class PipelineConfig extends ValidatableMixin {
  group: Stream<string>                                     = Stream();
  name: Stream<string>;
  labelTemplate: Stream<string>                             = Stream("${COUNT}");
  automaticScheduling: Stream<boolean>                      = Stream();
  timerSpecification: Stream<string>                        = Stream();
  runOnNewMaterial: Stream<boolean>                         = Stream();
  lockingBehaviour: Stream<string>                          = Stream("multiple_instances");
  template: Stream<string>                                  = Stream();
  materials: Stream<Materials>;
  environmentVariables: Stream<EnvironmentVariableConfig[]> = Stream();
  parameters: Stream<Parameter[]>                           = Stream();
  stages: Stream<NameableSet<StageConfig>>;
  useTrackingTool: Stream<boolean>                          = Stream();
  trackingTool: TrackingTool                                = new TrackingTool();

  constructor(name: string, materials: Material[], stages: StageConfig[]) {
    super();

    this.name = Stream(name);
    this.environmentVariables([]);
    this.parameters([]);
    this.useTrackingTool(false);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validatePresenceOf("group");
    this.validateIdFormat("group");

    this.materials = Stream(new Materials(...materials));
    this.validateNonEmptyCollection("materials", {message: `A pipeline must have at least one material`});
    this.validateAssociated("materials");

    this.stages = Stream(new NameableSet(stages));
    this.validateAssociated("stages");

    this.validateMutualExclusivityOf("template",
      "stages",
      {message: "Pipeline stages must not be defined when using a pipeline template"});
  }

  static get(name: string) {
    return ApiRequestBuilder.GET(SparkRoutes.getPipelineConfigPath(name), ApiVersion.v1);
  }

  withGroup(group: string) {
    this.group(group);
    return this;
  }

  create(pause: boolean) {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineConfigCreatePath(), ApiVersion.latest, {
      payload: this.toApiPayload(),
      headers: {"X-pause-pipeline": pause.toString()}
    });
  }

  run() {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineTriggerPath(this.name()), ApiVersion.v1);
  }

  toApiPayload(): any {
    const raw   = JsonUtils.toSnakeCasedObject(this);
    const group = raw.group;
    delete raw.group;

    return {group, pipeline: raw};
  }
}

class TrackingTool {
  pattern: Stream<string> = Stream();
  uri: Stream<string>     = Stream();
}
