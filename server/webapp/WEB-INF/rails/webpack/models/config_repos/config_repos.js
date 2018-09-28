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

const AjaxHelper = require("helpers/ajax_helper");
const Routes     = require("gen/js-routes");
const Stream     = require("mithril/stream");
const parseError = require("helpers/mrequest").unwrapErrorExtractMessage;
const Dfr        = require("jquery").Deferred;

function ConfigRepos() {
  this.etag = Stream("");

  this.all = () => {
    const promise = req(() => AjaxHelper.GET({
      url: Routes.apiv1AdminConfigReposPath(),
      apiVersion: "v1",
      etag: this.etag()
    }));

    promise.then((_d, etag) => this.etag(etag));

    return promise;
  };

  this.get = (etag, id) => req(() => AjaxHelper.GET({
    url: Routes.apiv1AdminConfigRepoPath(id),
    apiVersion: "v1",
    etag
  }));

  this.update = (etag, payload) => req(() => AjaxHelper.PUT({
    url: Routes.apiv1AdminConfigRepoPath(payload.id),
    apiVersion: "v1",
    etag,
    payload
  }));

  this.delete = (id) => req(() => AjaxHelper.DELETE({
    url: Routes.apiv1AdminConfigRepoPath(id),
    apiVersion: "v1"
  }));

  this.create = (payload) => req(() => AjaxHelper.POST({
    url: Routes.apiv1AdminConfigReposPath(),
    apiVersion: "v1",
    payload
  }));
}

function req(exec) {
  return Dfr(function run() {
    const success = (data, _s, xhr) => this.resolve(data, parseEtag(xhr), xhr.status);
    const failure = (xhr) => this.reject(parseError(JSON.parse(xhr.responseText), xhr));

    exec().then(success, failure);
  }).promise();
}

function parseEtag(req) { return (req.getResponseHeader("ETag") || "").replace(/--(gzip|deflate)/, ""); }

module.exports = ConfigRepos;
