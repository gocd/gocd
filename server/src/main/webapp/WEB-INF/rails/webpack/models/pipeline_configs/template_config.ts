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
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import Stream from "mithril/stream";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineParameter} from "models/pipeline_configs/parameter";
import {Stage} from "models/pipeline_configs/stage";

export class TemplateConfig {
  readonly name       = Stream<string>();
  readonly parameters = Stream<PipelineParameter[]>();
  readonly stages     = Stream<NameableSet<Stage>>();

  constructor(name: string, parameters: PipelineParameter[]) {
    this.name(name);
    this.parameters(parameters);
  }

  static getTemplate(name: string, onSuccess: (result: TemplateConfig) => void) {
    ApiRequestBuilder.GET(SparkRoutes.templatesPath(name), ApiVersion.latest).then((res) => {
        res.map((body) => {
          const params = _.map(JSON.parse(body).parameters || [], (param) => new PipelineParameter(param.name, param.value));
          onSuccess(new TemplateConfig(name, params));
        });
      }
    );
  }

  firstStage() {
    return this.stages().values().next().value;
  }
}
