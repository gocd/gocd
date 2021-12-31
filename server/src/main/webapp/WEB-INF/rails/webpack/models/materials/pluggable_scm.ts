/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import Stream from "mithril/stream";
import {Errors, ErrorsJSON} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Origin, OriginJSON, OriginType} from "models/origin";
import {Configurations, PropertyJSON} from "models/shared/configuration";

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
  origin?: OriginJSON;
  plugin_metadata: PluginMetadataJSON;
  configuration: PropertyJSON[];
  errors?: ErrorsJSON;
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

    this.id      = Stream(id);
    this.version = Stream(version);

    this.validatePresenceOf("id");
  }

  static fromJSON(data: PluginMetadataJSON): PluginMetadata {
    return new PluginMetadata(data.id, data.version);
  }
}

export class Scm extends ValidatableMixin {
  id: Stream<string>;
  name: Stream<string>;
  autoUpdate: Stream<boolean>;
  origin = Stream<Origin>();
  pluginMetadata: Stream<PluginMetadata>;
  configuration: Stream<Configurations>;

  constructor(id: string, name: string, autoUpdate: boolean, origin: Origin, pluginMetadata: PluginMetadata, configuration: Configurations) {
    super();

    this.id             = Stream(id);
    this.name           = Stream(name);
    this.autoUpdate     = Stream(autoUpdate);
    this.origin         = Stream(origin);
    this.pluginMetadata = Stream(pluginMetadata);
    this.configuration  = Stream(configuration);

    this.validatePresenceOf("name");
    this.validateFormatOf("name",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Name. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period)."});
    this.validateMaxLength("name", 255, {message: "The maximum allowed length is 255 characters."});
    this.validateAssociated("pluginMetadata");
  }

  static fromJSON(data: ScmJSON): Scm {
    const origin = data.origin ? Origin.fromJSON(data.origin) : new Origin(OriginType.GoCD);
    const scm    = new Scm(data.id, data.name, data.auto_update, origin, PluginMetadata.fromJSON(data.plugin_metadata), Configurations.fromJSON(data.configuration));
    scm.errors(new Errors(data.errors));
    return scm;
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
    };
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
