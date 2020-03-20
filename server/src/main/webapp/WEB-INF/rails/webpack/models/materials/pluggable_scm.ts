/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {Configurations, PropertyJSON} from "models/shared/configuration";
import Stream from "mithril/stream";

export interface ScmsJSON {
  _embedded: EmbeddedJSON;
}

interface EmbeddedJSON {
  scms: ScmJSON[];
}

interface ScmJSON {
  id: string;
  name: string;
  auto_update: boolean;
  plugin_metadata: PluginMetadataJSON;
  configuration: PropertyJSON[]
}

export interface PluginMetadataJSON {
  id: string;
  version: string;
}

class PluginMetadata {
  id: Stream<string>;
  version: Stream<string>;

  constructor(id: string, version: string) {
    this.id      = Stream(id);
    this.version = Stream(version);
  }

  static fromJSON(data: PluginMetadataJSON): PluginMetadata {
    return new PluginMetadata(data.id, data.version);
  }
}

class Scm {
  id: Stream<string>;
  name: Stream<string>;
  autoUpdate: Stream<boolean>;
  pluginMetadata: Stream<PluginMetadata>;
  configuration: Stream<Configurations>;

  constructor(id: string, name: string, autoUpdate: boolean, pluginMetadata: PluginMetadata, configuration: Configurations) {
    this.id             = Stream(id);
    this.name           = Stream(name);
    this.autoUpdate     = Stream(autoUpdate);
    this.pluginMetadata = Stream(pluginMetadata);
    this.configuration  = Stream(configuration);
  }

  static fromJSON(data: ScmJSON): Scm {
    return new Scm(data.id, data.name, data.auto_update, PluginMetadata.fromJSON(data.plugin_metadata), Configurations.fromJSON(data.configuration));
  }
}

export class Scms extends Array<Scm> {
  constructor(...vals: Scm[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(Scms.prototype));
  }

  static fromJSON(data: ScmJSON[]): Scms {
    return new Scms(...data.map((a) => Scm.fromJSON(a)));
  }
}
