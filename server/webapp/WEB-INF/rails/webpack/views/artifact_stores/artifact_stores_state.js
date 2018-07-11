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

const Stream = require('mithril/stream');

let _artifactStores, _pluginInfos, _error, _loading;

class ArtifactStoresState {

  constructor() {
    this.init();
    this.message = Stream({type: undefined, message: undefined});
  }

  init() {
    _artifactStores = undefined;
    _pluginInfos    = undefined;
    _error          = false;
    _loading        = true;
  }

  updateWithData(artifactStoresResponse, pluginInfosResponse) {
    _artifactStores = artifactStoresResponse;
    _pluginInfos    = pluginInfosResponse;
    _error          = false;
    _loading        = false;
  }

  updateWithApiError() {
    _loading = false;
    _error   = true;
  }

  get loading() {
    return _loading;
  }

  get error() {
    return _error;
  }

  get artifactStores() {
    return _artifactStores;
  }

  set artifactStores(artifactStores) {
    _artifactStores = artifactStores;
  }

  get pluginInfos() {
    return _pluginInfos;
  }

  noPlugins() {
    return _pluginInfos.countPluginInfo() === 0;
  }

  findPluginInfo(pluginId) {
    return _pluginInfos.findById(pluginId);
  }

  setAlertMessage(message) {
    this.message({type: 'alert', message});
  }

  setSuccessMessage(message) {
    this.message({type: 'success', message});
  }

  resetMessage() {
    this.message({type: undefined, message: undefined});
  }
}

module.exports = ArtifactStoresState;
