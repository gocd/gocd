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

function ConfigRepos() {
  this.etag = Stream("");

  this.all = () => {
    const promise = ApiHelper.GET({
      url: Routes.apiv1AdminConfigReposPath(),
      apiVersion: "v1",
      etag: this.etag()
    });

    promise.then((_d, etag) => this.etag(etag));

    return promise;
  };

  this.get = (etag, id) => ApiHelper.GET({
    url: Routes.apiv1AdminConfigRepoPath(id),
    apiVersion: "v1",
    etag
  });

  this.update = (etag, payload) => ApiHelper.PUT({
    url: Routes.apiv1AdminConfigRepoPath(payload.id),
    apiVersion: "v1",
    etag,
    payload
  });

  this.delete = (id) => ApiHelper.DELETE({
    url: Routes.apiv1AdminConfigRepoPath(id),
    apiVersion: "v1"
  });

  this.create = (payload) => ApiHelper.POST({
    url: Routes.apiv1AdminConfigReposPath(),
    apiVersion: "v1",
    payload
  });
}

module.exports = ConfigRepos;
