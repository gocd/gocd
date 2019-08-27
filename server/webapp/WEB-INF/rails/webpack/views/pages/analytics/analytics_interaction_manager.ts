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

import {Frame} from "models/shared/analytics_frame";
import {AnalyticsNamespace} from "views/pages/analytics/analytics_namespace";

const PluginEndpointRequestHandler = require("rails-shared/plugin-endpoint-request-handler");

export namespace AnalyticsInteractionManager {
  const models: { [key: string]: Frame } = {};

  export function purge() {
    Object.keys(models).forEach((name: string) => delete models[name]);
  }

  export function all() {
    return Object.assign({}, models);
  }

  export function ns(name: string): AnalyticsNamespace {
    const modelsForNamespace: Frame[] = Object.keys(models)
                                              .filter((key: string) => key === name)
                                              .map((key: string) => models[key]);
    return new AnalyticsNamespace(name, modelsForNamespace);
  }

  export function ensure() {
    PluginEndpointRequestHandler.defineLinkHandler();
    PluginEndpointRequestHandler.defineFetchAnalyticsHandler(models);
    return AnalyticsInteractionManager;
  }
}
