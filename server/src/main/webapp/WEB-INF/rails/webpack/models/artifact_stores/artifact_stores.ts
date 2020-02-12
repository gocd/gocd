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

import _ from "lodash";
import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Configurations, PropertyJSON} from "models/shared/configuration";

interface EmbeddedJSON {
  artifact_stores: ArtifactStoreJSON[];
}

export interface ArtifactStoresJSON {
  _embedded: EmbeddedJSON;
}

export interface ArtifactStoreJSON {
  id: string;
  plugin_id: string;
  properties: PropertyJSON[];
  errors?: { [key: string]: string[] };
}

export class ArtifactStores extends Array<ArtifactStore> {
  constructor(...artifactStores: ArtifactStore[]) {
    super(...artifactStores);
    Object.setPrototypeOf(this, Object.create(ArtifactStores.prototype));
  }

  static fromJSON(data: ArtifactStoresJSON) {
    const artifactStores = data._embedded.artifact_stores.map(ArtifactStore.fromJSON);
    return new ArtifactStores(...artifactStores);
  }

  groupByPlugin() {
    return _.groupBy(this, (entity) => {
      return entity.pluginId();
    });
  }
}

export class ArtifactStore extends ValidatableMixin {
  id: Stream<string>;
  pluginId: Stream<string>;
  properties: Stream<Configurations>;

  constructor(id: string,
              pluginId: string,
              properties: Configurations,
              errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);
    this.id         = Stream(id);
    this.pluginId   = Stream(pluginId);
    this.properties = Stream(properties);
    this.errors(errors);

    this.validatePresenceOf("pluginId");
    this.validatePresenceOf("id");
    this.validateFormatOf("id",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("id", 255, {message: "The maximum allowed length is 255 characters."});
  }

  static fromJSON(data: ArtifactStoreJSON) {
    return new ArtifactStore(data.id,
                             data.plugin_id,
                             Configurations.fromJSON(data.properties),
                             new Errors(data.errors));
  }

  toJSON() {
    return {
      id: this.id(),
      plugin_id: this.pluginId(),
      properties: this.properties.toJSON()
    };
  }
}

applyMixins(ArtifactStore, ValidatableMixin);
