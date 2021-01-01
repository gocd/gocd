/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {ApiRequestBuilder} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import Stream from "mithril/stream";
import {Traversable} from "models/base/traversable";
import {ConfigReposCRUD} from "./config_repos_crud";

export interface NamedTree extends Traversable {
  name: () => string;
}

abstract class NamedConcept implements NamedTree {
  name: Stream<string>;

  abstract children: NamedTree[];

  constructor(name: string) {
    this.name = Stream(name);
  }
}

export class DefinedStructures implements NamedTree {
  children: NamedTree[];

  constructor(groups: DefinedGroup[], envs: DefinedEnvironment[]) {
    this.children = envs.concat(groups);
  }

  static fromJSON(json: RootJson) {
    return new DefinedStructures(
      _.map(json.groups || [], (j) => DefinedGroup.fromJSON(j)),
      _.map(json.environments || [], (j) => DefinedEnvironment.fromJSON(j))
    );
  }

  static fetch(repoId: string, etag?: string) {
    return ApiRequestBuilder.GET(SparkRoutes.configRepoDefinedConfigsPath(repoId), ConfigReposCRUD.API_VERSION_HEADER, { etag });
  }

  name(): string {
    return "PartialConfig";
  }
}

export class DefinedGroup extends NamedConcept {
  children: DefinedPipeline[];

  constructor(name: string, pipelines: NamedJson[]) {
    super(name);
    this.children = _.map(pipelines, (json) => DefinedPipeline.fromJSON(json));
  }

  static fromJSON(json: GroupJson) {
    return new DefinedGroup(json.name, json.pipelines);
  }
}

export class DefinedPipeline extends NamedConcept {
  readonly children: NamedTree[] = [];

  constructor(name: string) {
    super(name);
  }

  static fromJSON(json: NamedJson) {
    return new DefinedPipeline(json.name);
  }
}

export class DefinedEnvironment extends NamedConcept {
  readonly children: NamedTree[] = [];

  constructor(name: string) {
    super(name);
  }

  static fromJSON(json: NamedJson) {
    return new DefinedEnvironment(json.name);
  }
}

interface NamedJson {
  name: string;
}

interface GroupJson extends NamedJson {
  pipelines: NamedJson[];
}

interface RootJson {
  environments?: NamedJson[];
  groups?: GroupJson[];
}
