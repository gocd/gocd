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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import SparkRoutes from "helpers/spark_routes";
import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {Traversable} from "models/base/traversable";

export interface NamedTree extends Traversable {
  name: () => string;
}

abstract class NamedConcept implements NamedTree {
  name: Stream<string>;

  abstract children: NamedTree[];

  constructor(name: string) {
    this.name = stream(name);
  }
}

export class DefinedStructures extends Array<NamedTree> {
  constructor(...items: DefinedGroup[]) {
    super(...items);
  }

  static fromJSON(json: GroupJson[]) {
    return new DefinedStructures(..._.map(json, (g) => DefinedGroup.fromJSON(g)));
  }

  static fetch(repoId: string): Promise<DefinedStructures> {
    return new Promise<DefinedStructures>((resolve, reject) => {
      ApiRequestBuilder.GET(SparkRoutes.configRepoDefinedPipelinesPath(repoId), ApiVersion.v2).
        then((result: ApiResult<string>) => {
          result.do((resp) => {
            resolve(DefinedStructures.fromJSON(JSON.parse(resp.body)));
          }, (error) => {
            reject(error);
          });
        }).catch(reject);
    });
  }
}

export class DefinedGroup extends NamedConcept {
  children: DefinedPipeline[];

  constructor(name: string, pipelines: PipelineJson[]) {
    super(name);
    this.children = _.map(pipelines, (json) => DefinedPipeline.fromJSON(json));
  }

  static fromJSON(json: GroupJson) {
    return new DefinedGroup(json.name, json.pipelines);
  }
}

export class DefinedPipeline extends NamedConcept {
  children: DefinedStage[];

  constructor(name: string, stages: StageJson[]) {
    super(name);
    this.children = _.map(stages, (json) => DefinedStage.fromJSON(json));
  }

  static fromJSON(json: PipelineJson) {
    return new DefinedPipeline(json.name, json.stages);
  }
}

export class DefinedStage extends NamedConcept {
  readonly children: NamedTree[] = [];

  constructor(name: string) {
    super(name);
  }

  static fromJSON(json: StageJson) {
    return new DefinedStage(json.name);
  }
}

interface StageJson {
  name: string;
}

interface PipelineJson {
  name: string;
  stages: StageJson[];
}

interface GroupJson {
  name: string;
  pipelines: PipelineJson[];
}
