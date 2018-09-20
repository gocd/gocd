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

const AjaxHelper = require('helpers/ajax_helper');
const Routes     = require('gen/js-routes');
const Stream     = require('mithril/stream');

function ConfigRepos() {
  const etag = Stream("");

  this.all = () => {
    const promise = AjaxHelper.GET({
      url: Routes.apiv1AdminConfigReposPath(),
      apiVersion: "v1",
      etag: etag()
    });
    promise.then((_d, _s, req) => etag(parseEtag(req)));
    return promise;
  };

  this.get = (etag, id) => AjaxHelper.GET({
    url: Routes.apiv1AdminConfigRepoPath(id),
    apiVersion: "v1",
    etag,
  });

  this.update = (etag, payload) => AjaxHelper.PUT({
    url: Routes.apiv1AdminConfigRepoPath(payload.id),
    apiVersion: "v1",
    etag,
    payload
  });
}

function parseEtag(req) { return (req.getResponseHeader("ETag") || "").replace(/--(gzip|deflate)/, ""); }

module.exports = ConfigRepos;
