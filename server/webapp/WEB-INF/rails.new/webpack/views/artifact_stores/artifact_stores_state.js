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


class ArtifactStoresState {

  constructor() {
    this._artifactStores = undefined;
    this._pluginInfos    = undefined;
    this._error = false;
    this._loading = true;
  }

  init() {
    this._artifactStores = undefined;
    this._pluginInfos    = undefined;
    this._error = false;
    this._loading = true;
  }

  updateWithData(artifactStoresResponse, pluginInfosResponse) {
    this._artifactStores = artifactStoresResponse;
    this._pluginInfos    = pluginInfosResponse;
    this._error = false;
    this._loading = false;
  }

  updateWithApiError() {
    this._loading = false;
    this._error = true;
  }

  get loading() {
    return this._loading;
  }

  get error() {
    return this._error;
  }

  get artifactStores() {
    return this._artifactStores;
  }

  noPlugins() {
    return this._pluginInfos.countPluginInfo() === 0;
  }

  findPluginInfo(pluginId) {
    return this._pluginInfos.findById(pluginId);
  }

}

module.exports = ArtifactStoresState;
