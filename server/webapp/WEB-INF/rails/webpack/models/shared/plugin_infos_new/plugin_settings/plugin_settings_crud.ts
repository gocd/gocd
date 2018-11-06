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

import * as Routes from "gen/ts-routes";
import {ApiRequestBuilder, ApiVersion, HttpResponseWithEtag} from "helpers/api_request_builder";
import {PluginSettings} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";

export class PluginSettingsCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static all() {
    return ApiRequestBuilder.GET(Routes.apiv1AdminPluginSettingsPath(), this.API_VERSION_HEADER)
      .then((xhr: XMLHttpRequest) => PluginSettings.fromJSON(JSON.parse(xhr.responseText)));
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(Routes.apiv1AdminPluginSettingPath(id), this.API_VERSION_HEADER)
      .then(this.extractResponseWithEtag());
  }

  static create(pluginSettings: PluginSettings) {
    return ApiRequestBuilder.POST(Routes.apiv1AdminPluginSettingsPath(), this.API_VERSION_HEADER, pluginSettings.toJSON())
      .then(this.extractResponseWithEtag());
  }

  static update(pluginSettings: PluginSettings, etag: string) {
    return ApiRequestBuilder.PUT(Routes.apiv1AdminPluginSettingPath(pluginSettings.plugin_id), this.API_VERSION_HEADER, pluginSettings.toJSON(), etag)
      .then(this.extractResponseWithEtag());
  }

  private static extractResponseWithEtag() {
    return (xhr: XMLHttpRequest) => {
      return {
        object: PluginSettings.fromJSON(JSON.parse(xhr.responseText)),
        etag: xhr.getResponseHeader("etag")
      } as HttpResponseWithEtag<PluginSettings>;
    };
  }
}
