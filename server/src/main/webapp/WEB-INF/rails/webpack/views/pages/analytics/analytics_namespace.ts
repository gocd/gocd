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

import {SparkRoutes} from "helpers/spark_routes";
import {Frame} from "models/shared/analytics_frame";
import {AnalyticsCapability} from "models/shared/plugin_infos_new/analytics_plugin_capabilities";

export class AnalyticsNamespace {
  readonly uid    = this.encodeUID;
  readonly unpack = this.decodeUID;
  private readonly models: Map<string, Frame>;
  private readonly name: string;
  private readonly prefix: string;

  constructor(name: string, models: Map<string, Frame>) {
    this.name   = name;
    this.models = models;
    this.prefix = `${encodeURIComponent(name)}:`;
  }

  withPrefix(uid: string) {
    return this.prefix + uid;
  }

  withoutPrefix(uid: string): string {
    return uid.split(":").pop() as string;
  }

  encodeUID(ordinal: number, pluginId: string, type: string, id: object) {
    return this.withPrefix(btoa(JSON.stringify({plugin: pluginId, type, id, ordinal})));
  }

  decodeUID(uid: string) {
    return JSON.parse(atob(this.withoutPrefix(uid)));
  }

  group() {
    return this.name;
  }

  all() {
    return Object.keys(this.models)
                 .filter((key: string) => key === this.name)
                 .map((key: string) => this.models.get(key));
  }

  toUrl(uid: string, params: { [key: string]: string | number } = {}) {
    const c = this.decodeUID(uid);
    return SparkRoutes.showAnalyticsPath(c.plugin, c.id as AnalyticsCapability, params);
  }

  modelFor(uid: string, extraParams: { [key: string]: string | number } = {}) {
    let model = this.models.get(uid);

    if (!model) {
      model = new Frame(uid);
      model.url(this.toUrl(uid, extraParams));
      this.models.set(uid, model);
    }
    return model;
  }
}
