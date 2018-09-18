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

const Stream     = require('mithril/stream');
const AjaxHelper = require('helpers/ajax_helper');
const AjaxPoller = require('helpers/ajax_poller');
const Routes     = require('gen/js-routes');

function ConfigRepos() {
  const repos = Stream([]);

  this.refresh = () => AjaxHelper.GET({
    url: Routes.apiv1AdminConfigReposPath(),
    apiVersion: "v1",
    etag: "blah"
  }).then((data) => {
    repos(data._embedded.config_repos);
  });

  this.repos = repos;
  const poller = new AjaxPoller(() => this.refresh());

  this.start = () => {
    poller.start();
    return this;
  };
}

module.exports = ConfigRepos;
