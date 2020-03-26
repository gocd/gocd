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
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

export interface ScmsJSON {
  _embedded: EmbeddedJSON;
}

interface EmbeddedJSON {
  scms: ScmJSON[];
}

export interface ScmJSON {
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

export interface ScmUsageJSON {
  group: string;
  pipeline: string;
}

export interface ScmUsagesJSON {
  usages: ScmUsageJSON[];
}

export class PluginMetadata extends ValidatableMixin {
  id: Stream<string>;
  version: Stream<string>;

  constructor(id: string, version: string) {
    super();
    ValidatableMixin.call(this);
    this.id      = Stream(id);
    this.version = Stream(version);

    this.validatePresenceOf("id");
  }

  static fromJSON(data: PluginMetadataJSON): PluginMetadata {
    return new PluginMetadata(data.id, data.version);
  }
}

applyMixins(PluginMetadata, ValidatableMixin);

export class Scm extends ValidatableMixin {
  id: Stream<string>;
  name: Stream<string>;
  autoUpdate: Stream<boolean>;
  pluginMetadata: Stream<PluginMetadata>;
  configuration: Stream<Configurations>;

  constructor(id: string, name: string, autoUpdate: boolean, pluginMetadata: PluginMetadata, configuration: Configurations) {
    super();
    ValidatableMixin.call(this);
    this.id             = Stream(id);
    this.name           = Stream(name);
    this.autoUpdate     = Stream(autoUpdate);
    this.pluginMetadata = Stream(pluginMetadata);
    this.configuration  = Stream(configuration);

    this.validatePresenceOf("id");
    this.validatePresenceOf("name");
    this.validateFormatOf("name",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("name", 255, {message: "The maximum allowed length is 255 characters."});
    this.validateAssociated("pluginMetadata");
  }

  static fromJSON(data: ScmJSON): Scm {
    return new Scm(data.id, data.name, data.auto_update, PluginMetadata.fromJSON(data.plugin_metadata), Configurations.fromJSON(data.configuration));
  }

  toJSON(): object {
    return {
      id:              this.id(),
      name:            this.name(),
      auto_update:     this.autoUpdate(),
      plugin_metadata: {
        id:      this.pluginMetadata().id(),
        version: this.pluginMetadata().version()
      },
      configuration:   this.configuration().toJSON()
    }
  }
}

applyMixins(Scm, ValidatableMixin);

export class Scms extends Array<Scm> {
  constructor(...vals: Scm[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(Scms.prototype));
  }

  static fromJSON(data: ScmJSON[]): Scms {
    return new Scms(...data.map((a) => Scm.fromJSON(a)));
  }
}

class ScmUsage {
  group: string;
  pipeline: string;

  constructor(group: string, pipeline: string) {
    this.group    = group;
    this.pipeline = pipeline;
  }

  static fromJSON(data: ScmUsageJSON): ScmUsage {
    return new ScmUsage(data.group, data.pipeline);
  }
}

export class ScmUsages extends Array<ScmUsage> {
  constructor(...vals: ScmUsage[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(ScmUsages.prototype));
  }

  static fromJSON(data: ScmUsagesJSON): ScmUsages {
    return new ScmUsages(...data.usages.map((a) => ScmUsage.fromJSON(a)));
  }
}
