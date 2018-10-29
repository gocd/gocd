/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const ApiHelper = require("helpers/api_helper");
const Routes    = require("gen/js-routes");
const Stream    = require("mithril/stream");

import * as types from "mithril/stream";
import SparkRoutes from "../../helpers/spark_routes";

type resolver = (d: object, etag?: string, status?: number) => any;
type rejector = (errorMessage: string, status?: number) => any;

interface PromiseLike {
  then: (success: resolver, fail?: rejector) => PromiseLike
}

interface Persistable {
  id: string
}

export default class ConfigRepos {
  private etag: types.Stream<string>;
  constructor() {
    this.etag = Stream("");
  }

  all = (): PromiseLike => {
    const promise = ApiHelper.GET({
      url: SparkRoutes.ConfigRepoListPath(),
      apiVersion: "v1",
      etag: this.etag()
    });

    promise.then((_d:any, etag: string) => this.etag(etag));

    return promise;
  };

  get = (etag: string, id: string): PromiseLike => ApiHelper.GET({
    url: Routes.apiv1AdminConfigRepoPath(id),
    apiVersion: "v1",
    etag
  });

  update = (etag: string, payload: Persistable): PromiseLike => ApiHelper.PUT({
    url: Routes.apiv1AdminConfigRepoPath(payload.id),
    apiVersion: "v1",
    etag,
    payload
  });

  delete = (id: string): PromiseLike => ApiHelper.DELETE({
    url: Routes.apiv1AdminConfigRepoPath(id),
    apiVersion: "v1"
  });

  create = (payload: object): PromiseLike => ApiHelper.POST({
    url: Routes.apiv1AdminConfigReposPath(),
    apiVersion: "v1",
    payload
  });
};
