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
import JsonUtils from "helpers/json_utils";
import SparkRoutes from "helpers/spark_routes";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {Material} from "models/materials/types";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {NameableSet} from "./nameable_set";
import {Stage} from "./stage";

export class PipelineConfig extends ValidatableMixin {
  group: Stream<string> = stream("defaultGroup");
  name: Stream<string>;
  template: Stream<string> = stream();
  materials: Stream<NameableSet<Material>>;
  stages: Stream<NameableSet<Stage>>;

  constructor(name: string, materials: Material[], stages: Stage[]) {
    super();

    this.name = stream(name);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validatePresenceOf("group");
    this.validateIdFormat("group");

    this.materials = stream(new NameableSet(materials));
    this.validateNonEmptyCollection("materials", {message: `A pipeline must have at least one material`});
    this.validateAssociated("materials");

    this.stages = stream(new NameableSet(stages));
    this.validateAssociated("stages");

    this.validateMutualExclusivityOf("template", "stages", { message: "Pipeline stages must not be defined when using a pipeline template" });
  }

  create(pause: boolean) {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineConfigCreatePath(), ApiVersion.v8, {
      payload: this.toApiPayload(),
      headers: {"X-pause-pipeline": pause.toString()}
    });
  }

  run() {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineTriggerPath(this.name()), ApiVersion.v1);
  }

  toApiPayload(): any {
    const raw = JsonUtils.toSnakeCasedObject(this);
    const group = raw.group;
    delete raw.group;

    return { group, pipeline: raw };
  }
}
