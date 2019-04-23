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
import {NameableSet, NonEmptyCollectionValidator} from "./nameable_set";
import {Stage} from "./stage";

export class PipelineConfig extends ValidatableMixin {
  group: Stream<string> = stream("defaultGroup");
  name: Stream<string>;
  materials: Stream<NameableSet<Material>>;
  stages: Stream<NameableSet<Stage>>;

  constructor(name: string, materials: Material[], stages: Stage[]) {
    super();

    ValidatableMixin.call(this);
    this.name = stream(name);
    this.validatePresenceOf("name");
    this.validatePresenceOf("group");

    this.materials = stream(new NameableSet(materials));
    this.validateWith(new NonEmptyCollectionValidator({message: `A pipeline must have at least one material.`}), "materials");
    this.validateAssociated("materials");

    this.stages = stream(new NameableSet(stages));
    this.validateWith(new NonEmptyCollectionValidator({message: `A pipeline must have at least one stage.`}), "stages");
    this.validateAssociated("stages");
  }

  create() {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineConfigCreatePath(), ApiVersion.v7, {
      payload: this.toApiPayload()
    });
  }

  pause() {
    return ApiRequestBuilder.POST(SparkRoutes.pipelinePausePath(this.name()), ApiVersion.v1);
  }

  toApiPayload(): any {
    const raw = JsonUtils.toSnakeCasedObject(this);
    const group = raw.group;
    delete raw.group;
    return { group, pipeline: raw };
  }

  modelType(): string {
    return "PipelineConfig";
  }
}
