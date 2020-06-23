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
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {ParameterJSON, PipelineParameter} from "models/pipeline_configs/parameter";
import {Stage, StageJSON} from "models/pipeline_configs/stage";

export interface TemplateConfigJSON {
  name: string;
  parameters: ParameterJSON[];
  stages: StageJSON[];
}

export class TemplateConfig extends ValidatableMixin {
  name: Stream<string>;
  parameters: Stream<PipelineParameter[]>;
  readonly stages = Stream<NameableSet<Stage>>();

  constructor(name: string, parameters: PipelineParameter[]) {
    super();
    this.name       = Stream(name);
    this.parameters = Stream(parameters);

    this.validatePresenceOf("name");
    this.validateIdFormat("name");
    this.validateAssociated("stages");
  }

  static getTemplate(name: string, onSuccess: (result: TemplateConfig) => void) {
    return ApiRequestBuilder.GET(SparkRoutes.templatesPath(name), ApiVersion.v7)
                            .then((res) => {
                              res.map((body) => onSuccess(TemplateConfig.fromJSON(JSON.parse(body))));
                            });
  }

  static fromJSON(json: TemplateConfigJSON) {
    const template = new TemplateConfig(json.name, PipelineParameter.fromJSONArray(json.parameters || []));
    template.stages(new NameableSet(Stage.fromJSONArray(json.stages || [])));

    return template;
  }

  firstStage() {
    return this.stages().values().next().value;
  }

  toApiPayload(): any {
    return JsonUtils.toSnakeCasedObject(this);
  }

  update(etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.templatesPath(this.name()), ApiVersion.latest, {
      payload: this.toApiPayload(), etag
    });
  }
}
